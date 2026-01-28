#!/bin/bash
# Script para importar workflows de n8n automáticamente al contenedor Docker (Linux Version)
# Uso: chmod +x import_n8n.sh && ./import_n8n.sh

echo -e "\e[36m🚀 Iniciando importación de workflows a n8n...\e[0m"

CONTAINER_NAME="bibliotech-n8n-vps"
WORKFLOWS_DIR="/home/node/workflows"

# Lista de workflows a importar
WORKFLOWS=(
    "workflow_nueva_reserva.json"
    "workflow_nuevo_prestamo.json"
    "workflow_devolucion_tarde.json"
    "workflow_recomendaciones_semanales.json"
)

# Verificar si el contenedor está corriendo
if [ ! "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    echo -e "\e[31m❌ Error: El contenedor '$CONTAINER_NAME' no está en ejecución.\e[0m"
    echo -e "\e[33mAsegúrate de haber desplegado el stack con: docker compose -f docker-compose-vps.yml up -d\e[0m"
    exit 1
fi

for wf in "${WORKFLOWS[@]}"; do
    echo -n "Importing $wf... "
    
    # Ejecutar comando de importación dentro del contenedor
    # Usamos n8n import:workflow --input=...
    if docker exec "$CONTAINER_NAME" n8n import:workflow --input="$WORKFLOWS_DIR/$wf" > /dev/null 2>&1; then
        echo -e "\e[32m✅ OK\e[0m"
    else
        echo -e "\e[31m❌ Error\e[0m"
        # Mostrar error detallado si falla
        docker exec "$CONTAINER_NAME" n8n import:workflow --input="$WORKFLOWS_DIR/$wf"
    fi
done

echo -e "\n\e[36m✨ Importación completada. Recuerda activar los workflows en la interfaz de n8n.\e[0m"
