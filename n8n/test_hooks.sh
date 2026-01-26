#!/bin/bash
# Script de prueba de webhooks backend -> n8n -> telegram

echo "🚀 Probando integración Webhook -> n8n -> Telegram"

# 1. Probar Nueva Reserva
echo "--- 1. Probando Nueva Reserva ---"
curl -X POST http://localhost:5678/webhook/nueva-reserva \
  -H "Content-Type: application/json" \
  -d '{
    "usuario": "TestUser",
    "libro": "El Quijote (Test)",
    "timestamp": '$(date +%s)'
  }'
echo -e "\n✅ Webhook de reserva enviado."

# 2. Probar Devolución Tarde
echo -e "\n--- 2. Probando Devolución Tarde ---"
curl -X POST http://localhost:5678/webhook/devolucion-tarde \
  -H "Content-Type: application/json" \
  -d '{
    "usuario": "Socio1",
    "libro": "Harry Potter (Test)",
    "dias_retraso": 3
  }'
echo -e "\n✅ Webhook de devolución tarde enviado."

echo -e "\n✨ Pruebas finalizadas. Revisa el grupo de Telegram."
