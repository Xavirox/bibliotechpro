# 🔒 Guía de Seguridad y SSL para BiblioTech Pro

Esta guía detalla cómo asegurar el acceso a la aplicación en el VPS utilizando HTTPS (SSL/TLS).

## 1. Contexto de la Infraestructura

Actualmente, la aplicación expone el frontend en el puerto **9142**.
Para habilitar HTTPS de forma profesional y segura, recomendamos configurar un **Proxy Inverso** en el host del VPS (fuera de Docker) o utilizar un contenedor dedicado como `Nginx Proxy Manager`.

## 2. Opción A: Configurar Certbot en el Host (Recomendada)

Si tienes acceso root al VPS y el dominio `asir.javiergimenez.es` apunta a la IP del VPS, sigue estos pasos:

### Paso 1: Instalar Nginx y Certbot en el Host

```bash
sudo apt update
sudo apt install nginx certbot python3-certbot-nginx
```

### Paso 2: Crear configuración de Nginx

Edita `/etc/nginx/sites-available/bibliotech` con:

```nginx
server {
    server_name asir.javiergimenez.es;

    location / {
        # Redirigir tráfico al contenedor Docker (Puerto 9142)
        proxy_pass http://localhost:9142;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Paso 3: Activar el sitio y Certbot

```bash
sudo ln -s /etc/nginx/sites-available/bibliotech /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# Obtener certificado SSL Automático
sudo certbot --nginx -d asir.javiergimenez.es
```

¡Listo! Certbot configurará automáticamente la redirección HTTPS y renovará los certificados.

## 3. Opción B: Self-Signed (Solo Pruebas)

Si solo necesitas encriptación interna y no importa la alerta del navegador:

1. Genera certificados:
   ```bash
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout nginx-selfsigned.key -out nginx-selfsigned.crt
   ```
2. Monta estos archivos en el contenedor `nginx` en `docker-compose.yml`.
3. Configura `nginx.conf` para escuchar en 443 ssl.

## 4. Headers de Seguridad

El contenedor Nginx actual ya incluye headers de seguridad básicos:
- `X-Frame-Options: SAMEORIGIN`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`

Al implementar HTTPS, asegúrate de añadir HSTS en el proxy superior:
`add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;`
