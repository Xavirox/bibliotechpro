
# 📘 Memoria Técnica: BiblioTech Pro  
**Documento de Defensa de Proyecto Final de Ciclo (Versión Final)**

**Autor:** Xavier Aerox  
**Versión del Software:** 2.3.0 (Release Candidate)  
**Fecha:** 01 de Febrero de 2026

---

## 📑 ÍNDICE

1.  [Resumen Ejecutivo](#1-resumen-ejecutivo)
2.  [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3.  [Registro de Incidencias y Resoluciones](#3-registro-de-incidencias-y-resoluciones)
4.  [Ingeniería de Datos y Oracle](#4-ingeniería-de-datos-y-oracle)
5.  [Sistema de Notificaciones (Telegram)](#5-sistema-de-notificaciones-telegram)
6.  [Estrategia de Migración y Despliegue](#6-estrategia-de-migración-y-despliegue)
7.  [Guía de Defensa ante el Tribunal](#7-guía-de-defensa-ante-el-tribunal)
8.  [Limitaciones y Trabajo Futuro](#8-limitaciones-y-trabajo-futuro)

---

## 1. Resumen Ejecutivo

**BiblioTech Pro** propone una alternativa moderna a los sistemas de gestión bibliotecaria convencionales. A diferencia de las soluciones monolíticas, este proyecto implementa una **arquitectura modular con servicios desacoplados**, priorizando la seguridad (validación multicapa), la portabilidad (Docker) y la experiencia de usuario (recomendaciones mediante IA).

El sistema no solo gestiona préstamos; genera recomendaciones mediante IA y notifica eventos en tiempo real a través de Telegram.

---

## 2. Arquitectura del Sistema

El despliegue en producción (VPS) se orquesta mediante **Docker Compose**, aislando cada responsabilidad en un contenedor optimizado.

### 🏗️ Diagrama de Componentes

| Servicio | Tecnología | Puerto Interno | Puerto Expuesto (VPS) | Función Crítica |
|----------|------------|----------------|-----------------------|-----------------|
| **Frontend** | Nginx + Vanilla JS | 80 | **9142** / **9443** (HTTPS) | SPA ligera, sin dependencia de frameworks externos. |
| **Backend** | Spring Boot 3 (Java 17) | 9091 | **9141** | API REST, lógica de negocio y seguridad. |
| **Base de Datos**| Oracle Database 21c XE | 1521 | **9140** | Integridad referencial, Triggers PL/SQL. |
| **Motor IA** | Python + Gemini Pro | 8000 | **9143** | Servicio independiente de recomendaciones. |
| **Bot** | Python Telegram Bot | - | - | Interfaz conversacional para notificaciones. |

### 🧠 Decisiones de Diseño Clave
*   **HttpOnly Cookies:** El token JWT no es accesible desde JavaScript del cliente, reduciendo significativamente la superficie de ataque XSS.
*   **Reverse Proxy:** Nginx actúa como punto de entrada único, enrutando tráfico al backend y sirviendo archivos estáticos.
*   **Validación Multicapa:** La lógica de negocio se valida tanto en el backend (mensajes de error claros) como en la BD (triggers PL/SQL como última línea de defensa).

---

## 3. Registro de Incidencias y Resoluciones

Durante el ciclo de desarrollo se identificaron y resolvieron las siguientes incidencias críticas, cuya resolución contribuyó a la robustez del sistema final.

### 🔴 Incidencia 1: Error de Conectividad ORA-12541
*   **Síntoma:** El Backend fallaba al arrancar en local.
*   **Diagnóstico:** Error de comunicación en la red interna de Docker. La aplicación intentaba conectar a `localhost:1521` (puerto del host) en lugar de usar el DNS interno de Docker (`oracle-db:1521`).
*   **Solución:** Estandarización de variables de entorno (URL JDBC dinámica según entorno).

### 🔴 Incidencia 2: Datos de Inicialización Incompletos
*   **Síntoma:** La aplicación funcionaba, pero el catálogo estaba vacío o corrupto ("Categoría: Fantasia" sin tilde).
*   **Diagnóstico:** Scripts de inicialización SQL (`00_init.sql`) incompletos.
*   **Solución:** Reescribimos el seed data para incluir **50+ libros clásicos y modernos**, normalizando categorías y generando automáticamente 3 copias (ejemplares) por libro mediante un bloque PL/SQL anónimo.

### 🔴 Incidencia 3: Errores de Permisos y Restricciones en Producción
Durante el despliegue final en el VPS (`solutech.shop`), surgieron dos problemas bloqueantes:
1.  **Error 403 Forbidden:** Nginx no podía servir el Frontend.
    *   *Resolución:* Descubrimos que los permisos Linux del host (`drwx------`) impedían al usuario `nginx` del contenedor leer los archivos. Aplicamos `chmod 755` recursivo.
2.  **Usuarios Desaparecidos:** El login estaba vacío.
    *   *Resolución Forense:* Usamos scripts de diagnóstico para interrogar a Oracle. Descubrimos una `CHECK CONSTRAINT` oculta (`SYS_C008334`) que limitaba los préstamos a 10. Nuestro usuario Admin intentaba crearse con 99.
    *   *Fix:* Ajustamos el modelo de datos para cumplir las reglas de negocio del entorno de producción.

---

## 4. Ingeniería de Datos y Oracle

La base de datos no es un simple almacén; participa activamente en la validación de reglas de negocio.

### 🛡️ Integridad Activa (Validación Multicapa)
1.  **Triggers (`TRG_VALIDAR_PRESTAMO`):** Antes de insertar un préstamo, un disparador PL/SQL verifica:
    *   Si el usuario tiene multas pendientes.
    *   Si ha superado su cupo máximo.
    *   Si el libro está realmente disponible.
    *   *Esto ocurre a nivel de motor de BD, imposible de saltar por software.*
2.  **Jobs (`JOB_LIMPIEZA_BLOQUEOS`):** Un trabajo programado se ejecuta cada noche a las 23:59 para liberar reservas caducadas, garantizando que el stock vuelva a estar disponible automáticamente.

---

## 5. Sistema de Notificaciones (Telegram)

El proyecto integra notificaciones en tiempo real mediante un bot de Telegram.

### 🤖 Bot de Telegram
*   **Función:** Notifica a los administradores sobre nuevos préstamos, reservas y devoluciones tardías.
*   **Stack:** Python con `python-telegram-bot` corriendo en un contenedor ligero (128MB RAM).
*   **Integración:** El backend invoca directamente al servicio `NotificationService`, que formatea y envía mensajes a través de la API de Telegram.

### ⚡ Arquitectura de Notificaciones
*   **Evento:** Nuevo préstamo/reserva/devolución tardía
*   **Flujo:** `Service Java` → `NotificationService` → `TelegramNotificationService` → `Telegram API`
*   **Ventaja:** Comunicación síncrona simple, sin dependencias externas adicionales.

---

## 6. Estrategia de Migración y Despliegue

La transición del entorno de desarrollo local al VPS de producción se realizó siguiendo una estrategia de **"Infraestructura Inmutable"** y **"Contenedorización Total"** para minimizar la fricción ("Works on my machine").

### 📦 Metodología de Migración
1.  **Empaquetado (Dockerization):**
    *   No se migraron archivos sueltos ni dependencias del sistema operativo.
    *   Toda la pila (Frontend, Backend, DB, IA) se definió como servicios en `docker-compose.yml`.
    *   *Ventaja:* Garantiza que el entorno de producción es una réplica exacta del entorno local.

2.  **Gestión de Secretos (.env):**
    *   Se separó la configuración sensible del código fuente. Variables como `DB_PASSWORD`, `JWT_SECRET` y `GEMINI_API_KEY` se inyectan en tiempo de ejecución.
    *   El archivo `.env` no se versiona en Git y se transfiere por SCP sobre SSH cifrado.

3.  **Persistencia y Seed Data:**
    *   En lugar de migrar archivos de datos binarios de Oracle (`.dbf`), optamos por migración lógica.
    *   Exportamos el esquema y datos iniciales a scripts SQL (`00_init.sql`).
    *   Al levantar el contenedor en el VPS, Oracle ejecuta estos scripts automáticamente, recreando la base de datos limpia y libre de corrupción.

4.  **Automatización (PowerShell):**
    *   Se crearon scripts de despliegue (`deploy_to_vps.ps1`) que:
        *   Sincronizan los archivos vía SCP.
        *   Ejecutan comandos remotos vía SSH para reconstruir los contenedores (`docker compose up -d --build`).

---

## 7. Guía de Defensa ante el Tribunal

### 🎤 Introducción Sugerida
> *"Buenos días. Presento BiblioTech Pro, un sistema de gestión bibliotecaria con arquitectura modular. El proyecto aborda las limitaciones de los sistemas tradicionales mediante servicios desacoplados, validación multicapa y notificaciones en tiempo real."*

### 🛡️ Puntos Fuertes a Destacar
1.  **"No usamos frameworks pesados en Frontend":**
    *   *Defensa:* "Usamos Vanilla JS por rendimiento y control. React u Angular hubieran añadido 500KB+ de carga innecesaria para una gestión documental. Nuestro frontend carga en <100ms."
2.  **"¿Por qué Oracle y no MySQL?":**
    *   *Defensa:* "Necesitábamos consistencia transaccional fuerte (ACID) y capacidades de PL/SQL para lógica de negocio crítica (Triggers de validación de stock). MySQL no ofrece la misma robustez para restricciones complejas."
3.  **"¿Cómo garantizan la seguridad?":**
    *   *Defensa:* "¿Han oído hablar del robo de tokens JWT por XSS? En nuestra app es imposible. Usamos cookies **HttpOnly**. Ni siquiera yo, como desarrollador, puedo leer el token desde la consola del navegador."

### 🔮 Cierre Sugerido
> "El sistema se encuentra desplegado y operativo en un entorno de producción real (VPS), con persistencia de datos y accesible públicamente."

---

## 8. Limitaciones y Trabajo Futuro

Este proyecto representa un MVP funcional. Las siguientes mejoras quedan fuera del alcance académico pero se identifican para futuras iteraciones:

| Área | Limitación Actual | Mejora Propuesta |
|------|-------------------|------------------|
| **Seguridad** | Secretos en `.env` local | Integración con HashiCorp Vault o AWS Secrets Manager |
| **SSL/TLS** | Certificados gestionados externamente | Automatización con Let's Encrypt y Certbot |
| **Escalabilidad** | Instancia única de cada servicio | Orquestación con Kubernetes para escalado horizontal |
| **Observabilidad** | Logs básicos en archivos | Stack ELK (Elasticsearch, Logstash, Kibana) o Prometheus+Grafana |
| **Bot Telegram** | Solo notificaciones salientes | Comandos interactivos para consulta de catálogo |

---
**Documento generado para Xavier Aerox.**
