# ============================================================
# BiblioTech Pro - Python Antigravity Service
# Dockerfile Optimizado para FastAPI
# ============================================================
# Características:
#   - Imagen Alpine ultraligera
#   - Multi-stage para reducir tamaño final
#   - Usuario no-root para seguridad
#   - Healthcheck integrado
# ============================================================

# ===========================================
# STAGE 1: BUILD (instalar dependencias)
# ===========================================
FROM python:3.11-slim AS builder

WORKDIR /app

# Instalar dependencias de compilación si son necesarias
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copiar requirements e instalar en virtualenv
COPY requirements.txt .
RUN python -m venv /opt/venv && \
    /opt/venv/bin/pip install --no-cache-dir --upgrade pip && \
    /opt/venv/bin/pip install --no-cache-dir -r requirements.txt

# ===========================================
# STAGE 2: RUNTIME (imagen final ligera)
# ===========================================
FROM python:3.11-slim

LABEL maintainer="Xavier Aerox <xavier@bibliotech.pro>"
LABEL description="BiblioTech Pro AI/Antigravity Service"
LABEL version="1.0.0"

WORKDIR /app

# Variables de entorno para Python
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH"

# Instalar solo curl para healthcheck (sin paquetes de dev)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Copiar virtualenv desde builder
COPY --from=builder /opt/venv /opt/venv

# Copiar código de la aplicación
COPY ./app ./app

# Crear usuario no-root
RUN useradd --create-home --shell /bin/bash appuser && \
    chown -R appuser:appuser /app

USER appuser

# Puerto
EXPOSE 8000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Comando de inicio (producción con workers)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
