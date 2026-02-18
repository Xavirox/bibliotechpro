# 🔐 INFRAESTRUCTURA Y ACCESOS - BIBLIOTECH PRO

Este documento contiene la información crítica de acceso a los servidores y servicios del proyecto.
**CLASIFICACIÓN: CONFIDENCIAL**

---

## 1. 🚀 SERVIDOR PRODUCCIÓN (ACTUAL)
**Estado:** OPERATIVO - FORTIFICADO
**Rol:** Servidor principal, aloja la aplicación en vivo con SSL.

| Recurso | Detalle |
|---------|---------|
| **Dominio** | `https://solutech.shop` |
| **IP Pública** | `82.223.44.230` |
| **Usuario SSH** | `bibliotech` |
| **Clave SSH** | `C:\Users\Xavi\.ssh\ionos_vps_key` |
| **Puertos Abiertos** | 22 (SSH), 80 (HTTP), 443 (HTTPS) |
| **Puertos Cerrados** | 9140 (Oracle), 9141 (Backend), 9144 (n8n), 9143 (AI) - Solo accesibles vía localhost |

### 🔑 Credenciales Aplicación
| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `password` | Administrador Total |
| `biblio` | `password` | Bibliotecario |
| `socio1` | `password` | Usuario Estándar |

### 🛠️ Comandos Útiles
```powershell
# Conectar por SSH
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230

# Ver logs en tiempo real
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose logs -f"

# Reiniciar servicios
ssh -i C:\Users\Xavi\.ssh\ionos_vps_key bibliotech@82.223.44.230 "cd ~/bibliotech-pro && docker compose restart"
```

---

## 2. 📦 SERVIDOR BACKUP / LEGACY (ANTIGUO)
**Estado:** INACTIVO (LIMPIO)
**Rol:** Reserva de recursos, backup frío o pruebas futuras.

| Recurso | Detalle |
|---------|---------|
| **Host** | `asir.javiergimenez.es` |
| **Usuario SSH** | `francisco` |
| **Clave SSH** | `C:\Users\Xavi\.ssh\id_rsa` |

### 🛠️ Comandos Útiles
```powershell
# Conectar por SSH
ssh -i C:\Users\Xavi\.ssh\id_rsa francisco@asir.javiergimenez.es
```

---

## 3. 🛡️ AUDITORÍA DE SEGURIDAD (Estado Actual)
- [x] **SSL/TLS:** Activado (Let's Encrypt) con renovación automática.
- [x] **Firewall:** UFW activado. Política "Deny Incoming" excepto 22, 80, 443.
- [x] **Docker:** Servicios internos vinculados a `127.0.0.1` (no expuestos a internet).
- [x] **CORS:** Restringido a `solutech.shop` y entorno local.
- [x] **Archivos:** Permisos de `.env` restringidos en producción.

---
**Fecha de actualización:** 2026-01-31
