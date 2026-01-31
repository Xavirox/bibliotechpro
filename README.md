# 📚 BiblioTech Pro - Sistema de Gestión de Biblioteca Digital

<div align="center">

![BiblioTech Pro](https://img.shields.io/badge/BiblioTech-Pro-6366F1?style=for-the-badge&logo=bookstack&logoColor=white)
![Version](https://img.shields.io/badge/Version-2.0.0-success?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![License](https://img.shields.io/badge/License-Academic-blue?style=for-the-badge)

**Sistema profesional de gestión bibliotecaria con Inteligencia Artificial integrada**

[🔗 Acceder a Producción](http://asir.javiergimenez.es:9142) •
[🚀 Inicio Rápido](#-inicio-rápido) •
[✨ Características](#-características-principales) •
[🏗️ Arquitectura](#️-arquitectura) •
[📖 Documentación](#-documentación)

> **🚀 PROYECTO DESPLEGADO:** Accede a la versión en vivo en [asir.javiergimenez.es:9142](http://asir.javiergimenez.es:9142)

</div>

---

## 🎯 Descripción

**BiblioTech Pro** es una aplicación web full-stack diseñada para la gestión integral de bibliotecas. Combina una interfaz de usuario moderna y elegante con un backend robusto basado en Spring Boot, implementando las mejores prácticas de desarrollo de software.

### 🌟 Puntos Destacados

- **Arquitectura Robusta**: Backend Spring Boot siguiendo arquitectura por capas (Controller-Service-Repository).
- **Seguridad**: Autenticación JWT y validación exhaustiva de reglas de negocio.
- **Calidad de Código**: Tests unitarios con JUnit 5 y documentación Javadoc integrada.
- **Interfaz Limpia**: Diseño responsive y accesible enfocado en la usabilidad.

---

## ✨ Características Principales

### 👤 Gestión de Usuarios

| Rol | Capacidades |
|-----|-------------|
| **Socio** | Explorar catálogo, reservar libros, consultar historial de préstamos. |
| **Bibliotecario** | Gestión integral de préstamos y devoluciones, control de inventario. |
| **Administrador** | Administración total del sistema y usuarios. |

### 📖 Funcionalidades Core

- ✅ Catálogo de libros con búsqueda y filtrado dinámico
- ✅ Control de disponibilidad de ejemplares en tiempo real
- ✅ Sistema de reservas (Bloqueos) con expiración automática
- ✅ Gestión de préstamos y devoluciones con validación de límites
- ✅ Historial detallado de lectura por usuario

### 🎨 Interfaz de Usuario

- ✅ Diseño moderno y minimalista
- ✅ Modo claro/oscuro con persistencia
- ✅ Totalmente responsive (desktop, tablet, móvil)
- ✅ Accesibilidad WCAG 2.1 AA

### 🔌 Integraciones y Automatización (Módulo Avanzado)

Implementación de arquitecturas modernas y orquestación de servicios para demostrar competencias técnicas avanzadas:

- **Bot de Telegram**: Interfaz conversacional accesible para consultas en movilidad.
- **Automatización con n8n**: Workflows para la gestión de notificaciones y tareas programadas.
- **Webhooks & APIs**: Integración en tiempo real entre microservicios.

> Este módulo evidencia el dominio de integración de sistemas, uso de APIs de terceros (Telegram, Gemini) y herramientas de automatización.


## 🏗️ Arquitectura

```
biblioteca_web/
├── 📁 backend/                    # API REST con Spring Boot
│   ├── 📁 src/main/java/
│   │   └── 📁 com/biblioteca/
│   │       ├── 📁 controller/     # Endpoints REST
│   │       ├── 📁 service/        # Lógica de negocio
│   │       ├── 📁 repository/     # Acceso a datos (JPA)
│   │       ├── 📁 model/          # Entidades del dominio
│   │       ├── 📁 dto/            # Data Transfer Objects
│   │       ├── 📁 security/       # Configuración JWT
│   │       └── 📁 config/         # Configuraciones
│   └── 📁 src/main/resources/
│       └── application.properties
│
├── 📁 frontend/                   # SPA con JavaScript vanilla
│   ├── 📁 css/
│   │   ├── styles.css             # Sistema de diseño principal
│   │   ├── components.css         # Estilos de componentes
│   │   └── visuals.css            # Efectos avanzados
│   ├── 📁 js/
│   │   ├── main.js                # Punto de entrada
│   │   ├── auth.js                # Autenticación JWT
│   │   ├── catalog.js             # Gestión del catálogo
│   │   ├── user.js                # Funciones de usuario
│   │   ├── librarian.js           # Panel de administración
│   │   ├── effects.js             # Efectos visuales
│   │   ├── utils.js               # Utilidades comunes
│   │   └── config.js              # Configuración
│   └── index.html                 # SPA principal
│
└── 📁 docs/                       # Documentación técnica
```

### 🔧 Stack Tecnológico

#### Backend
- **Java 17+** - Lenguaje principal
- **Spring Boot 3.2** - Framework web
- **Spring Security** - Autenticación JWT
- **Spring Data JPA** - Persistencia
- **Oracle Database** - Base de datos
- **Lombok** - Reducción de boilerplate
- **Maven** - Gestión de dependencias

#### Frontend
- **HTML5 Semántico** - Estructura accesible
- **CSS3 Moderno** - Variables CSS, Grid, Flexbox
- **JavaScript ES6+** - Módulos nativos
- **Font Awesome 6** - Iconografía
- **Google Fonts (Inter)** - Tipografía premium

---

## 🚀 Despliegue en Producción (VPS)

### Prerrequisitos

- **Docker & Docker Compose** instalado en el servidor.
- Puerto **9142** (HTTP) abierto en el firewall.
- Conexión a internet para descarga de imágenes.

### 🛠️ Instalación y Despliegue

1. **Configurar variables de entorno**
   
   Crea un archivo `.env` en la raíz del proyecto basándote en `.env.example`:
   ```bash
   cp .env.example .env
   # Edita con tus credenciales reales (Oracle, Gemini, Telegram)
   nano .env
   ```

2. **Compilar el Backend (Local o CI/CD)**
   
   Para optimizar recursos en el VPS, se recomienda compilar el JAR antes de enviarlo:
   ```bash
   cd backend
   mvn clean package -DskipTests
   ```

3. **Iniciar la infraestructura completa**
   
   Ejecuta el orquestador desde la raíz:
   ```bash
   docker compose up -d --build
   ```

   Esto levantará:
   - **Nginx** (Puerto 9142): Frontend y Proxy Reverso.
   - **Backend** (Puerto 9141): API REST de la biblioteca.
   - **Oracle DB** (Puerto 9140): Base de datos persistente.
   - **AI Service**: Motor de recomendaciones (Gemini).
   - **Telegram Bot**: Interfaz conversacional.
   - **n8n**: Automatización de notificaciones.

### 🌍 Acceso a la Aplicación

Una vez iniciados los contenedores:
- **Frontend**: `http://tu-vps-ip:9142`
- **Documentación API**: `http://tu-vps-ip:9141/swagger-ui.html`
- **Dashboard n8n**: `http://tu-vps-ip:9144`

### Credenciales de Prueba

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| socio1 | password | SOCIO |
| biblio | password | BIBLIOTECARIO |
| admin | password | ADMIN |

---

## 📖 Documentación

### API REST

La API sigue los principios RESTful y está documentada con los siguientes endpoints principales:

```
GET    /api/libros              # Listar libros
GET    /api/libros/{id}         # Obtener libro específico
POST   /api/bloqueos            # Crear reserva
DELETE /api/bloqueos/{id}       # Cancelar reserva
POST   /api/prestamos           # Crear préstamo
PUT    /api/prestamos/{id}      # Devolver libro
POST   /api/recomendaciones     # Obtener recomendaciones IA
```

### Seguridad

- Autenticación basada en **JWT** (JSON Web Tokens)
- Tokens con expiración configurable
- Endpoints protegidos por roles
- Validación de datos en frontend y backend

---

## 🎨 Guía de Estilos

El proyecto implementa un sistema de diseño coherente basado en:

### Paleta de Colores

| Variable | Valor | Uso |
|----------|-------|-----|
| `--primary` | #6366F1 | Acciones principales |
| `--accent` | #F472B6 | Acentos y badges |
| `--success` | #10B981 | Estados positivos |
| `--warning` | #F59E0B | Alertas |
| `--danger` | #EF4444 | Errores |

### Tipografía

- **Inter** - Texto principal (400, 500, 600, 700, 800)
- Escala: xs (0.75rem) → 4xl (2.25rem)

### Componentes

- Botones con gradientes y sombras
- Cards con efecto 3D al hover
- Inputs con estados visuales claros
- Toasts animados para notificaciones

---

## 📊 Métricas del Proyecto

```
📁 Archivos de código:     ~25 archivos
📝 Líneas de código:       ~5,000+ líneas
🎨 Componentes CSS:        3 archivos modulares
📡 Endpoints API:          15+ endpoints
🧪 Cobertura de tests:     Backend con JUnit
```

---

## 🔧 Troubleshooting y Mantenimiento

### 🚨 Problemas Comunes en VPS

#### El proyecto no responde

Si tu proyecto en el VPS no está funcionando, ejecuta el diagnóstico automático:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\vps\diagnose_vps.ps1
```

Este script verifica:
- ✅ Conexión SSH al VPS
- ✅ Estado de todos los contenedores Docker
- ✅ Logs recientes de cada servicio
- ✅ Puertos expuestos
- ✅ Conectividad de endpoints públicos
- ✅ Uso de recursos (CPU/RAM)

#### Oracle Database caído

**Síntoma**: Backend muestra estado `unhealthy`, no puedes hacer login.

**Solución rápida**:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\vps\recover_vps.ps1
```

Este script automáticamente:
1. Reinicia Oracle Database
2. Espera a que esté completamente iniciado
3. Reinicia el Backend
4. Verifica la conectividad

**Tiempo estimado**: 3-4 minutos

#### Problemas de memoria en el VPS

Si el VPS tiene múltiples instancias Oracle corriendo:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\vps\cleanup_memory.ps1
```

Este script te permite:
- Ver uso de memoria del sistema
- Identificar contenedores Oracle
- Detener instancias innecesarias
- Liberar recursos

### 📚 Documentación de Troubleshooting

Para análisis detallado de problemas, consulta:

- **`GUIA_RECUPERACION.md`**: Guía paso a paso para recuperar el proyecto
- **`DIAGNOSTICO_VPS.md`**: Análisis completo de problemas comunes

### 🛠️ Scripts de Mantenimiento

| Script | Descripción | Uso |
|--------|-------------|-----|
| `diagnose_vps.ps1` | Diagnóstico completo del VPS | Identificar problemas |
| `recover_vps.ps1` | Recuperación automática | Reiniciar servicios caídos |
| `cleanup_memory.ps1` | Gestión de memoria | Liberar recursos |
| `deploy_to_vps.ps1` | Despliegue completo | Actualizar el proyecto |

### 📞 Comandos Útiles

```bash
# Ver logs en tiempo real
ssh -i ~/.ssh/vps_key usuario@vps "cd ~/bibliotech-pro && docker compose logs -f backend"

# Reiniciar un servicio específico
ssh -i ~/.ssh/vps_key usuario@vps "cd ~/bibliotech-pro && docker compose restart backend"

# Ver estado de contenedores
ssh -i ~/.ssh/vps_key usuario@vps "cd ~/bibliotech-pro && docker compose ps"

# Ver uso de recursos
ssh -i ~/.ssh/vps_key usuario@vps "docker stats --no-stream"
```

---

## 👨‍💻 Autor

**Xavier Aerox**

Proyecto desarrollado como parte del curso de Desarrollo de Aplicaciones Web.

---

## 📄 Licencia

Este proyecto es de uso académico. Todos los derechos reservados © 2026.

---

<div align="center">

**Hecho con ❤️ y ☕ por Xavier Aerox**

[![GitHub](https://img.shields.io/badge/GitHub-Profile-181717?style=flat-square&logo=github)](https://github.com)

</div>
