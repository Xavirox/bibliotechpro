# 🌐 Frontend BiblioTech Pro

## Servidor Web

### Opción Recomendada: Nginx (Producción)

El frontend está configurado para servirse con **Nginx**, un servidor web de alto rendimiento utilizado en producción por millones de sitios web.

#### ¿Por qué Nginx en lugar de Python http.server?

| Característica | Python http.server | Nginx |
|----------------|-------------------|-------|
| Concurrencia | ❌ Monohilo | ✅ Miles de conexiones |
| Compresión Gzip | ❌ No | ✅ Sí |
| Caché de assets | ❌ No | ✅ Configurable |
| Headers de seguridad | ❌ No | ✅ Completos |
| SPA Routing (try_files) | ❌ No | ✅ Nativo |
| Proxy reverso | ❌ No | ✅ Integrado |
| Apto para producción | ❌ **NO** | ✅ **SÍ** |

> ⚠️ **IMPORTANTE**: `python -m http.server` es **solo para desarrollo rápido** y **nunca debe usarse en producción**. Es monohilo, no gestiona cabeceras de caché ni compresión, y es vulnerable a ataques de denegación de servicio.

### Instalación de Nginx en Windows

1. **Descargar** desde: https://nginx.org/en/download.html (versión Windows)
2. **Extraer** en `C:\nginx`
3. **Verificar** que existe `C:\nginx\nginx.exe`

El script `abrir_proyecto.bat` detectará automáticamente Nginx si está instalado.

### Ejecución Manual

**Con Nginx:**
```batch
cd frontend
nginx -c nginx.conf -p .
```

**Con Python (solo desarrollo/emergencia):**
```batch
cd frontend
python -m http.server 8000
```

### Características de la Configuración Nginx

La configuración `nginx.conf` incluye:

- ✅ **Compresión Gzip** - Reduce tamaño de transferencia ~70%
- ✅ **Caché de assets** - CSS/JS: 7 días, Imágenes: 30 días
- ✅ **Headers de seguridad** - X-Frame-Options, X-XSS-Protection, etc.
- ✅ **Proxy reverso** - `/api/*` → `localhost:9091`
- ✅ **SPA fallback** - `try_files` para rutas de JavaScript
- ✅ **Service Worker** - Sin caché para actualizaciones inmediatas

### Comandos Útiles de Nginx

```batch
nginx -s reload    # Recargar configuración sin detener
nginx -s quit      # Detener gracefully
nginx -s stop      # Detener inmediatamente
nginx -t           # Verificar sintaxis de configuración
```

---

## Estructura de Archivos

```
frontend/
├── index.html        # Aplicación SPA principal
├── nginx.conf        # Configuración del servidor Nginx
├── manifest.json     # PWA manifest
├── sw.js             # Service Worker
├── css/
│   ├── styles.css    # Sistema de diseño
│   ├── components.css
│   └── visuals.css
└── js/
    ├── main.js       # Punto de entrada
    ├── api.js        # Cliente HTTP
    ├── auth.js       # Autenticación JWT
    ├── catalog.js    # Catálogo de libros
    ├── user.js       # Panel de usuario
    ├── librarian.js  # Panel bibliotecario
    └── effects.js    # Animaciones
```
