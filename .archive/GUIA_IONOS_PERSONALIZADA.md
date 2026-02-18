# 🚀 GUÍA RÁPIDA - Migración a Tu VPS IONOS

**IP VPS**: 82.223.44.230  
**Usuario**: root → bibliotech (se creará)  
**Fecha**: 2026-01-31

---

## ⚡ OPCIÓN RÁPIDA (TODO AUTOMATIZADO)

### Paso 1: Configurar VPS (10 minutos)

```powershell
# Ejecutar desde PowerShell en el directorio del proyecto
cd C:\Users\Xavi\.gemini\antigravity\scratch\biblioteca_web

# Ejecutar configuración inicial
.\scripts\vps\setup_ionos_initial.ps1
```

**¿Qué hace este script?**
- ✅ Genera clave SSH automáticamente
- ✅ Conecta al VPS (te pedirá la contraseña: `bM7yB6vU`)
- ✅ Actualiza el sistema
- ✅ Crea usuario `bibliotech`
- ✅ Instala Docker y Docker Compose
- ✅ Configura swap de 4GB
- ✅ Configura firewall (UFW)
- ✅ Instala Fail2Ban
- ✅ Configura acceso SSH con clave

**Tiempo**: ~10 minutos

### Paso 2: Desplegar BiblioTech Pro (10-15 minutos)

```powershell
# Ejecutar script de despliegue
.\scripts\vps\deploy_to_ionos.ps1
```

**Información que necesitarás**:
- IP VPS: `82.223.44.230` (ya configurada)
- Usuario: `bibliotech` (ya configurado)
- Variables de entorno:
  - `ORACLE_PASSWORD`: Tu contraseña para Oracle (elige una segura)
  - `DB_USER`: `bibliotech_user` (default)
  - `DB_PASSWORD`: Tu contraseña para la BD (elige una segura)
  - `JWT_SECRET`: Se genera automáticamente
  - `GEMINI_API_KEY`: Tu API key de Gemini
  - `TELEGRAM_BOT_TOKEN`: Tu token de Telegram

**Tiempo**: ~10-15 minutos

### Paso 3: Verificar (2 minutos)

```powershell
# Abrir navegador
start https://82.223.44.230:9145
```

**Credenciales por defecto**:
- Usuario: `admin`
- Contraseña: `admin123`

---

## 📋 COMANDOS ÚTILES PARA TU VPS

### Conectar al VPS

```powershell
# Con clave SSH (después de ejecutar setup_ionos_initial.ps1)
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230

# Con contraseña (antes de configurar SSH)
ssh root@82.223.44.230
# Contraseña: bM7yB6vU
```

### Ver Estado de Servicios

```powershell
# Estado de contenedores
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose ps"

# Logs en tiempo real
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose logs -f"

# Uso de recursos
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "docker stats --no-stream"
```

### Reiniciar Servicios

```powershell
# Reiniciar todo
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose restart"

# Reiniciar solo Oracle
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose restart oracle-db"

# Reiniciar solo Backend
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose restart backend"
```

---

## 🌐 URLs DE ACCESO

Después del despliegue, podrás acceder a:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Frontend** | https://82.223.44.230:9145 | Aplicación web principal |
| **Backend API** | http://82.223.44.230:9141 | API REST |
| **AI Service** | http://82.223.44.230:9143 | Servicio de IA |
| **n8n** | http://82.223.44.230:9144 | Automatización de workflows |

---

## 🔐 INFORMACIÓN DE SEGURIDAD

### Contraseñas del VPS

- **Root**: `bM7yB6vU` (solo para configuración inicial)
- **Usuario bibliotech**: `BiblioTech2026!` (se crea automáticamente)
- **Acceso SSH**: Clave privada en `C:\Users\Xavi\.ssh\ionos_vps_key`

### Recomendaciones

1. **Cambia la contraseña de root** después de la configuración inicial:
   ```bash
   ssh root@82.223.44.230
   passwd
   ```

2. **Deshabilita login root por SSH** (opcional, después de verificar que el usuario bibliotech funciona):
   ```bash
   sudo vim /etc/ssh/sshd_config
   # Cambiar: PermitRootLogin no
   sudo systemctl restart sshd
   ```

3. **Guarda la clave SSH** en un lugar seguro:
   - Ubicación: `C:\Users\Xavi\.ssh\ionos_vps_key`
   - Haz un backup en un USB o cloud seguro

---

## ⚠️ TROUBLESHOOTING

### Problema: "Permission denied" al conectar por SSH

**Solución**:
```powershell
# Verificar que la clave existe
Test-Path C:\Users\Xavi\.ssh\ionos_vps_key

# Si no existe, ejecutar setup_ionos_initial.ps1 primero
.\scripts\vps\setup_ionos_initial.ps1
```

### Problema: "Connection refused"

**Solución**:
```powershell
# Verificar que la IP es correcta
ping 82.223.44.230

# Verificar que el puerto 22 está abierto
Test-NetConnection -ComputerName 82.223.44.230 -Port 22
```

### Problema: Oracle no inicia

**Solución**:
```bash
# Conectar al VPS
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230

# Ver logs de Oracle
cd ~/bibliotech-pro
docker compose logs -f oracle-db

# Reiniciar Oracle
docker compose restart oracle-db

# Esperar 2-3 minutos
```

### Problema: Backend no conecta a Oracle

**Solución**:
```bash
# Verificar que Oracle esté healthy
docker compose ps

# Si Oracle está healthy, reiniciar backend
docker compose restart backend

# Ver logs del backend
docker compose logs -f backend
```

---

## 📊 VERIFICACIÓN DE RECURSOS

### Antes de Desplegar

```powershell
# Conectar al VPS
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230

# Ver recursos disponibles
free -h
df -h
nproc
```

### Después de Desplegar

```powershell
# Ver uso de recursos
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "docker stats --no-stream"
```

**Uso esperado**:
- Oracle: ~1.5-2GB RAM
- Backend: ~400-600MB RAM
- AI Service: ~200-400MB RAM
- Otros servicios: ~200MB RAM
- **Total**: ~2.5-3.5GB RAM

---

## ✅ CHECKLIST FINAL

### Configuración Inicial
- [ ] Ejecutar `.\scripts\vps\setup_ionos_initial.ps1`
- [ ] Verificar que se creó la clave SSH
- [ ] Verificar que puedes conectar con `ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230`
- [ ] Verificar que Docker está instalado

### Despliegue
- [ ] Ejecutar `.\scripts\vps\deploy_to_ionos.ps1`
- [ ] Proporcionar variables de entorno
- [ ] Esperar a que termine el despliegue
- [ ] Verificar que todos los servicios estén UP

### Verificación
- [ ] Acceder a https://82.223.44.230:9145
- [ ] Hacer login (admin/admin123)
- [ ] Crear una reserva de prueba
- [ ] Verificar notificación en Telegram
- [ ] Verificar que n8n funciona (http://82.223.44.230:9144)

### Post-Despliegue
- [ ] Cambiar contraseña de root
- [ ] Hacer backup de la clave SSH
- [ ] Configurar dominio (opcional)
- [ ] Configurar Let's Encrypt (opcional)
- [ ] Configurar backups automáticos

---

## 🎯 RESUMEN DE 1 MINUTO

```powershell
# 1. Configurar VPS (10 min)
cd C:\Users\Xavi\.gemini\antigravity\scratch\biblioteca_web
.\scripts\vps\setup_ionos_initial.ps1

# 2. Desplegar aplicación (15 min)
.\scripts\vps\deploy_to_ionos.ps1

# 3. Acceder
start https://82.223.44.230:9145
```

**Total**: ~25 minutos para tener BiblioTech Pro funcionando en tu VPS IONOS

---

## 📞 SIGUIENTE PASO

**Ejecuta ahora**:
```powershell
.\scripts\vps\setup_ionos_initial.ps1
```

Este script te guiará paso a paso y al final tendrás el VPS listo para desplegar BiblioTech Pro.

---

**¿Listo para empezar?** 🚀
