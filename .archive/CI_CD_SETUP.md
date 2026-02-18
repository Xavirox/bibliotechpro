# 🚀 Configuración del Despliegue Automático (CI/CD)

Este proyecto incluye un flujo de trabajo de **GitHub Actions** que despliega automáticamente la aplicación en tu VPS cada vez que haces un `git push` a la rama principal.

Para activarlo, debes configurar 3 "Secretos" en tu repositorio de GitHub.

## Pasos para Configurar

1. Ve a tu repositorio en GitHub.
2. Haz clic en la pestaña **Settings** (Configuración).
3. En el menú de la izquierda, baja hasta **Secrets and variables** > **Actions**.
4. Haz clic en el botón verde **New repository secret**.
5. Añade los siguientes 3 secretos:

### 1. `VPS_HOST`
- **Name**: `VPS_HOST`
- **Secret**: `asir.javiergimenez.es`

### 2. `VPS_USER`
- **Name**: `VPS_USER`
- **Secret**: `francisco`

### 3. `VPS_SSH_KEY`
- **Name**: `VPS_SSH_KEY`
- **Secret**: *(Copia todo el contenido de tu archivo de clave privada)*
  > Puedes ver el contenido ejecutando este comando en tu terminal local:
  > ```powershell
  > Get-Content "$env:USERPROFILE\.ssh\vps_key"
  > ```
  > Copia desde `-----BEGIN OPENSSH PRIVATE KEY-----` hasta `-----END OPENSSH PRIVATE KEY-----`.

---

## 🚦 Cómo probarlo

Una vez configurados los secretos:

1. Haz un cambio en tu código (incluso pequeño).
2. Haz commit y push:
   ```bash
   git add .
   git commit -m "Prueba de despliegue automatico"
   git push origin main
   ```
3. Ve a la pestaña **Actions** en GitHub y verás cómo empieza el trabajo "Deploy to VPS".
4. Si sale verde ✅, ¡tu código ya está en vivo en el servidor!
