# 📘 Memoria Técnica Unificada: BiblioTech Pro
**Sistema Integral de Gestión Bibliotecaria con Inteligencia Artificial**

**Autor:** Xavier Aerox  
**Versión del Proyecto:** 2.3.0 (Gold Release)  
**Fecha:** Febrero 2026

---

## 📑 ÍNDICE
1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Modelo de Datos y Lógica en Base de Datos](#3-modelo-de-datos-y-lógica-en-base-de-datos)
4. [Backend: Core de Servicios (Spring Boot)](#4-backend-core-de-servicios-spring-boot)
5. [Frontend: Interfaz y UX (Vanilla JS)](#5-frontend-interfaz-y-ux-vanilla-js)
6. [Módulos Avanzados: IA y Automatización](#6-módulos-avanzados-ia-y-automatización)
7. [Infraestructura, DevOps y Seguridad](#7-infraestructura-devops-y-seguridad)
8. [Registro de Incidencias y Resoluciones](#8-registro-de-incidencias-y-resoluciones)
9. [Guía de Defensa ante el Tribunal](#9-guía-de-defensa-ante-el-tribunal)
10. [Conclusión y Trabajo Futuro](#10-conclusión-y-trabajo-futuro)

---

## 1. Resumen Ejecutivo
BiblioTech Pro es una solución full-stack diseñada para modernizar la gestión de bibliotecas físicas. El proyecto destaca por su enfoque en la **seguridad proactiva**, la **integridad de datos en múltiples capas** y la **automatización inteligente**. No es solo un CRUD de libros; es un ecosistema que integra recomendaciones por IA, notificaciones automáticas por Telegram y una arquitectura orquestada mediante Docker, garantizando un despliegue profesional bajo principios de infraestructura inmutable.

---

## 2. Arquitectura del Sistema
El sistema emplea una arquitectura desacoplada organizada en contenedores:

### 🏗️ Stack Tecnológico
*   **Backend:** Java 17, Spring Boot 3.2, Spring Security (JWT), JPA/Hibernate.
*   **Frontend:** HTML5, CSS3 Moderno (Variables, Flexbox, Grid), JavaScript ES6 Vanilla.
*   **Base de Datos:** Oracle Database 21c XE (PL/SQL intensivo).
*   **IA & Automatización:** Python (FastAPI para IA), Gemini Pro API, n8n, Telegram Bot API.
*   **Infraestructura:** Docker Compose, Nginx (Proxy Inverso), SSL/TLS (Certbot).

### 📡 Flujo de Comunicación
`Cliente (HTTPS) -> Nginx -> Backend (REST API) -> Oracle DB / AI Service / Bot`

---

## 3. Modelo de Datos y Lógica en Base de Datos
A diferencia de otros sistemas, BiblioTech Pro delega la integridad crítica al motor de base de datos (Oracle), aplicando el principio de **Defensa en Profundidad**.

### 🛠️ Objetos Inteligentes en PL/SQL
1.  **Triggers de Validación (`TRG_VALIDAR_PRESTAMO`):**
    *   **Función:** Antes de que el backend inserte un préstamo, la BD verifica si el usuario tiene multas, si ha superado el máximo de libros o si hay stock real.
    *   **Importancia:** Impide corrupciones de datos incluso si el backend fuera vulnerado o fallara.
2.  **Jobs Programados (`DBMS_SCHEDULER`):**
    *   **Función:** Un proceso nocturno (`JOB_LIMPIEZA_BLOQUEOS`) libera automáticamente las reservas (bloqueos) que han expirado sin ser recogidas.
3.  **Índices Únicos Condicionales:**
    *   **Función:** Garantizan que un usuario solo pueda tener una reserva activa a la vez.

---

## 4. Backend: Core de Servicios (Spring Boot)
El backend sigue la arquitectura por capas: `Controller -> Service -> Repository`.

### 🔑 Seguridad JWT y HttpOnly
*   **Innovación:** Los tokens de autenticación no se guardan en `localStorage` (vulnerable a XSS), sino en **Cookies HttpOnly**. Esto hace que el token sea invisible para JavaScript, eliminando el riesgo de robo de identidad por scripts maliciosos.

### ⚙️ Funciones Principales por Módulo
*   **`BloqueoService`**: Gestiona el ciclo de vida de las reservas. Implementa lógica de expiración y validación de disponibilidad.
*   **`PrestamoService`**: Orquesta la entrega y devolución física. Calcula penalizaciones y actualiza el estado de los ejemplares.
*   **`AIGenerationService`**: Se comunica con el motor Python para obtener recomendaciones personalizadas basadas en el historial del socio.
*   **`NotificationService`**: Despacha avisos en tiempo real hacia Telegram y n8n tras eventos críticos.

---

## 5. Frontend: Interfaz y UX (Vanilla JS)
Se optó por no usar frameworks (React/Angular) para maximizar el rendimiento y demostrar dominio del estándar Web.

### 🎨 Diseño y Experiencia
*   **Sistema de Diseño:** Basado en una paleta personalizada (Indigo/Pink) con soporte nativo para **Modo Oscuro**.
*   **SPA (Single Page Application):** El contenido cambia dinámicamente sin recargar la página, ofreciendo una experiencia fluida.
*   **Seguridad:** Validación de formularios en tiempo real y sanitización de respuestas de la IA para evitar inyecciones de código.

---

## 6. Módulos Avanzados: IA y Automatización
El proyecto demuestra competencias en integración de servicios modernos.

1.  **Motor de Recomendaciones (AI Service):** Un servicio Python actúa como puente con **Google Gemini**, transformando el historial de lectura del usuario en sugerencias literarias coherentes.
2.  **Bot de Telegram:** Permite a los usuarios consultar el catálogo y recibir alertas fuera de la aplicación web.
3.  **Orquestación con n8n:** Flujos de trabajo que conectan el backend con servicios externos, permitiendo escalabilidad en las notificaciones.

---

## 7. Infraestructura, DevOps y Seguridad
El proyecto reside en un VPS fortificado bajo la consigna de "Puerto Seguro".

*   **Contenerización:** Cada servicio corre en su propio contenedor (Docker), aislando recursos y dependencias.
*   **Proxy Inverso (Nginx):** Centraliza el tráfico HTTPS, gestiona certificados SSL y oculta la topología interna de la red.
*   **Hardening de Red:** Solo los puertos 80 y 443 están expuestos. El acceso a la Base de Datos y al Backend está restringido a la red interna de Docker.
*   **CI/CD:** Automatización del despliegue mediante **GitHub Actions**. Cada `push` a la rama `main` dispara un workflow que empaqueta la aplicación, la transfiere vía SCP y reconstruye los contenedores en el VPS.
    *   *Nota Técnica:* El flujo requiere la configuración de Secrets en el repositorio (`VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`) para garantizar una entrega continua segura.

---

## 8. Registro de Incidencias y Resoluciones
La madurez del proyecto se refleja en los retos superados durante su desarrollo:

1.  **Conflicto de DNS Interno (Docker):** El backend no conectaba a Oracle. Se resolvió estandarizando variables de entorno para usar el nombre del servicio en lugar de IPs.
2.  **Permisos de Nginx:** Error 403 en producción. Se corrigió ajustando los permisos de usuario en el host para permitir la lectura de volúmenes compartidos.
3.  **Restricciones de Negocio Invisibles:** Se detectaron `CHECK CONSTRAINTS` en la BD que bloqueaban al usuario administrador. Se recalibró el modelo de datos para alinear la BD con las necesidades del negocio.

---

## 9. Guía de Defensa ante el Tribunal
Puntos clave para impresionar al jurado:
*   **Rendimiento:** "Nuestra app carga en milisegundos porque no arrastramos el peso de frameworks innecesarios".
*   **Seguridad:** "Usamos Cookies HttpOnly, una técnica de nivel bancario para proteger sesiones".
*   **Integridad:** "Incluso si alguien borrara el código del backend, la base de datos Oracle mantendría las reglas de negocio gracias a nuestros Triggers PL/SQL".
*   **Innovación:** "Integramos IA generativa no como un juguete, sino como un servicio desacoplado y seguro".

---

## 10. Conclusión y Trabajo Futuro
BiblioTech Pro es un sistema robusto, listo para producción. Como líneas futuras para la versión 3.0, se plantean:
*   **Escalabilidad:** Introducir Kubernetes para balanceo de carga.
*   **Observabilidad:** Implementar un stack ELK (Elasticsearch/Kibana) para monitorización.
*   **Comandos Interactivos:** Hacer que el Bot de Telegram permita realizar reservas directamente.

---
**Documentación Unificada - Xavier Aerox 2026**
