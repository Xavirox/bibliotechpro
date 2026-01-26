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

- **Diseño Premium**: Interfaz glassmorphism con animaciones fluidas y modo oscuro
- **IA Integrada**: Recomendaciones personalizadas mediante Google Gemini AI
- **Arquitectura Limpia**: Separación clara de responsabilidades y código modular
- **Experiencia de Usuario Superior**: Microinteracciones, feedback visual instantáneo

---

## ✨ Características Principales

### 👤 Gestión de Usuarios

| Rol | Capacidades |
|-----|-------------|
| **Socio** | Explorar catálogo, reservar libros, gestionar préstamos, recibir recomendaciones IA |
| **Bibliotecario** | Todo lo anterior + gestión de préstamos, visualización de estadísticas |
| **Administrador** | Acceso completo al sistema |

### 📖 Gestión de Libros

- ✅ Catálogo completo con búsqueda en tiempo real
- ✅ Filtrado por categorías (Novela, Ciencia Ficción, Fantasía, etc.)
- ✅ Sistema de reservas con expiración automática (24h)
- ✅ Control de disponibilidad de ejemplares
- ✅ Historial de préstamos completo

### 🤖 Inteligencia Artificial

- ✅ Motor de recomendaciones personalizado
- ✅ Análisis de historial de lectura
- ✅ Sugerencias basadas en preferencias detectadas
- ✅ Integración con **Google Gemini API**

### 🎨 Diseño & UX Premium

- ✅ Interfaz moderna con glassmorphism
- ✅ Animaciones CSS3 fluidas
- ✅ Efecto 3D tilt en tarjetas de libros
- ✅ Modo claro/oscuro con persistencia
- ✅ Totalmente responsive (desktop, tablet, móvil)
- ✅ Accesibilidad WCAG 2.1 AA

### 🤖 Bot de Telegram

- ✅ Consultas del catálogo en tiempo real
- ✅ Recomendaciones automáticas cada hora
- ✅ Sistema de suscripciones opt-in
- ✅ Búsqueda por categorías
- ✅ Integración con n8n para automatizaciones
- ✅ Notificaciones de reservas y préstamos

**Comandos principales:**
```
/catalogo   - Ver libros disponibles
/buscar X   - Buscar por título/autor
/recomendar - Obtener recomendación IA
/suscribir  - Activar notificaciones horarias
```

> Ver documentación completa en [docs/BOT_TELEGRAM.md](docs/BOT_TELEGRAM.md)


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

## 🚀 Inicio Rápido

### Prerrequisitos

- Java 17 o superior
- Maven 3.8+
- **Nginx** (recomendado) o Python 3 (fallback)
- Oracle Database ejecutándose (puerto 1521)
- Navegador moderno (Chrome, Firefox, Edge)

> 💡 **¿Por qué Nginx?** A diferencia de `python -m http.server` (monohilo, sin caché ni compresión), Nginx es un servidor de producción que ofrece compresión Gzip, caché de assets, headers de seguridad y proxy reverso. El script detecta automáticamente cuál usar.

### Instalación

1. **Configurar variables de entorno**
   
   Editar `backend/.env` con tus credenciales:
   ```
   DB_USER=tu_usuario
   DB_PASSWORD=tu_contraseña
   DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1
   GEMINI_API_KEY=tu_api_key_gemini
   ```

2. **Iniciar la aplicación (un clic)**
   ```bash
   # Doble clic en:
   abrir_proyecto.bat
   ```
   
   Esto inicia automáticamente el backend y el frontend.

   **Alternativa: Docker Compose** 🐳
   ```bash
   # Iniciar Nginx + Oracle DB en contenedores
   docker-compose up -d
   
   # Luego iniciar el backend manualmente
   cd backend && ./start.ps1
   ```

3. **Acceder a la aplicación**
   ```
   http://localhost:8000
   ```

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
