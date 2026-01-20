@echo off
chcp 65001 >nul
echo ===================================================
echo   INICIANDO BIBLIOTECA WEB - AMBIENTE COMPLETO
echo ===================================================
echo.

REM 0. Cargar variables de entorno desde backend/.env
if exist "backend\.env" (
    echo [INFO] Cargando variables de entorno...
    for /f "usebackq tokens=1,* delims==" %%a in ("backend\.env") do (
        REM Ignorar lineas que empiezan con #
        echo %%a | findstr /b "#" >nul || (
            set "%%a=%%b"
        )
    )
    echo [OK] Variables cargadas.
) else (
    echo [WARN] No se encontro backend\.env
)

REM 1. Verificar si el Backend esta corriendo
netstat -an | findstr ":9091 " >nul
if %errorlevel% neq 0 (
    echo [INFO] El Backend no parece estar corriendo ^(Puerto 9091 libre^).
    echo [INFO] Iniciando Backend en una nueva ventana...
    start "Biblioteca Backend" cmd /k "cd backend && powershell -ExecutionPolicy Bypass -File start.ps1"
    
    echo [ESPERA] Esperando 20 segundos a que el Backend arranque...
    timeout /t 20 /nobreak
) else (
    echo [OK] Backend detectado en puerto 9091.
)

REM 2. Verificar si Nginx esta en el PATH o en el directorio
echo [INFO] Buscando Nginx...
where nginx >nul 2>&1
if %errorlevel% equ 0 (
    set "NGINX_CMD=nginx"
    goto :nginx_found
)

REM Buscar en ubicaciones comunes de Windows
if exist "C:\nginx\nginx.exe" (
    set "NGINX_CMD=C:\nginx\nginx.exe"
    goto :nginx_found
)
if exist "C:\Program Files\nginx\nginx.exe" (
    set "NGINX_CMD=C:\Program Files\nginx\nginx.exe"
    goto :nginx_found
)

REM Si no se encuentra Nginx, usar Python como fallback
echo [WARN] Nginx no encontrado. Usando servidor Python como alternativa.
echo [INFO] Para mejor rendimiento, instala Nginx en C:\nginx
goto :python_fallback

:nginx_found
echo [OK] Nginx encontrado: %NGINX_CMD%
netstat -an | findstr ":8000 " >nul
if %errorlevel% neq 0 (
    echo [INFO] Iniciando servidor Nginx en puerto 8000...
    cd frontend
    
    REM Crear directorio de logs si no existe
    if not exist "logs" mkdir logs
    
    REM Iniciar Nginx con nuestra configuración
    start "Biblioteca Frontend (Nginx)" cmd /k ""%NGINX_CMD%" -c "%cd%\nginx.conf" -p "%cd%""
    cd ..
    timeout /t 2 /nobreak
) else (
    echo [OK] Servidor Frontend detectado en puerto 8000.
)
goto :open_browser

:python_fallback
netstat -an | findstr ":8000 " >nul
if %errorlevel% neq 0 (
    echo [INFO] Iniciando Servidor Web Frontend (Python)...
    start "Biblioteca Frontend" cmd /k "cd frontend && python -m http.server 8000"
    timeout /t 2 /nobreak
) else (
    echo [OK] Servidor Frontend detectado en puerto 8000.
)

:open_browser
REM 3. Esperar un momento y abrir navegador
echo [INFO] Abriendo navegador...
timeout /t 3 /nobreak >nul
start http://localhost:8000

echo.
echo ===================================================
echo   TODO LISTO
echo   Backend:  http://localhost:9091/swagger-ui.html
echo   Frontend: http://localhost:8000
echo   Si la web no carga, espera unos segundos y recarga.
echo ===================================================
echo.
echo [TIP] Para detener Nginx: nginx -s quit
echo [TIP] Para recargar config: nginx -s reload
pause
