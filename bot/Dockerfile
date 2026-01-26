FROM python:3.11-slim

WORKDIR /app

# Crear directorio para datos persistentes
RUN mkdir -p /app/data

# Instalar dependencias
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código de la aplicación
COPY *.py .

# Variable de entorno para archivo de suscripciones
ENV SUBSCRIPTIONS_FILE=/app/data/subscriptions.json

CMD ["python", "bibliotech_bot.py"]
