# BiblioTech Pro - Guía de Despliegue en VPS

## 📋 Requisitos Previos

### En tu máquina local:
- Git instalado
- PowerShell o Bash
- SSH configurado con clave privada

### En el VPS:
- Ubuntu 22.04 LTS o Debian 12
- Docker Engine 24+
- Docker Compose v2+
- Mínimo 4GB RAM, 2 vCPUs
- Puerto SSH (22) accesible

---

## 🔐 Paso 1: Configurar Acceso SSH Seguro

### 1.1 Generar par de claves (si no tienes)
```bash
ssh-keygen -t ed25519 -C "bibliotech@vps" -f ~/.ssh/vps_key
```

### 1.2 Copiar clave pública al VPS
```bash
ssh-copy-id -i ~/.ssh/vps_key.pub usuario@tu-vps.com
```

### 1.3 Configurar SSH en el VPS (solo clave, sin password)
```bash
# En el VPS, editar /etc/ssh/sshd_config:
sudo nano /etc/ssh/sshd_config

# Cambiar estas líneas:
PasswordAuthentication no
PubkeyAuthentication yes
PermitRootLogin no

# Reiniciar SSH
sudo systemctl restart sshd
```

---

## 🐳 Paso 2: Instalar Docker en el VPS

```bash
# Conectar al VPS
ssh -i ~/.ssh/vps_key usuario@tu-vps.com

# Instalar Docker
curl -fsSL https://get.docker.com | sudo sh

# Añadir usuario al grupo docker
sudo usermod -aG docker $USER

# Cerrar sesión y volver a entrar
exit
ssh -i ~/.ssh/vps_key usuario@tu-vps.com

# Verificar
docker --version
docker compose version
```

---

## 📦 Paso 3: Desplegar la Aplicación

### 3.1 Desde Windows (PowerShell)
```powershell
# Ejecutar el script de despliegue
.\deploy_to_vps.ps1
```

### 3.2 Manualmente
```bash
# En el VPS
cd ~/bibliotech-pro

# Crear archivo .env con secretos
cat > .env << 'EOF'
ORACLE_PASSWORD=TuPasswordSeguro123!
DB_USER=biblioteca
DB_PASSWORD=biblioteca123
GEMINI_API_KEY=tu_api_key_de_gemini
JWT_SECRET=una_cadena_muy_larga_y_segura_de_al_menos_64_caracteres
TELEGRAM_BOT_TOKEN=tu_token_de_telegram
EOF

# Iniciar servicios
docker compose -f docker-compose-vps.yml up -d --build

# Ver logs
docker compose -f docker-compose-vps.yml logs -f
```

---

## 🔥 Paso 4: Configurar Firewall

```bash
# Solo permitir puertos necesarios
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 9142/tcp  # Nginx (frontend)
# NO exponemos 9140 (Oracle), 9141 (Backend), 9143 (Python) externamente
sudo ufw enable
```

---

## 🌐 Paso 5: Configurar Dominio (Opcional)

### 5.1 DNS
Apunta tu dominio al IP del VPS:
```
bibliotech.tudominio.com  A  123.45.67.89
```

### 5.2 HTTPS con Certbot
```bash
# Instalar Certbot
sudo apt install certbot python3-certbot-nginx

# Obtener certificado
sudo certbot --nginx -d bibliotech.tudominio.com
```

---

## 📊 Comandos Útiles

```bash
# Ver estado de contenedores
docker compose -f docker-compose-vps.yml ps

# Ver logs de un servicio específico
docker compose -f docker-compose-vps.yml logs backend

# Reiniciar un servicio
docker compose -f docker-compose-vps.yml restart backend

# Actualizar y redesplegar
git pull
docker compose -f docker-compose-vps.yml up -d --build

# Limpiar imágenes no usadas
docker system prune -a
```

---

## 🏗️ Arquitectura de Puertos

| Servicio | Puerto Interno | Puerto Externo | Exposición |
|----------|----------------|----------------|------------|
| Oracle DB | 1521 | 9140 | Solo docker network |
| Backend Java | 9091 | 9141 | Solo docker network |
| AI Service (Python) | 8000 | 9143 | Solo docker network |
| Nginx | 80 | **9142** | **Público** |

**Acceso externo**: Solo Nginx está expuesto. Las rutas internas son:
- `http://tudominio:9142/` → Frontend
- `http://tudominio:9142/api/` → Backend Java
- `http://tudominio:9142/magic/` → Servicio Python (Antigravity)

---

## ✅ Verificación

```bash
# Health check de Nginx
curl http://localhost:9142/health

# Test API
curl http://localhost:9142/api/libros

# Test servicio Python
curl http://localhost:9142/magic/health
```

---

## 🚨 Solución de Problemas

### Error: "Cannot connect to Oracle"
```bash
# Esperar a que Oracle esté healthy (puede tardar 2-3 min)
docker compose -f docker-compose-vps.yml logs oracle-db
```

### Error: "Permission denied"
```bash
# Dar permisos correctos
chmod +x deploy/*.sh
chmod 600 ~/.ssh/vps_key
```

### Ver todos los logs
```bash
docker compose -f docker-compose-vps.yml logs --tail=100 -f
```
