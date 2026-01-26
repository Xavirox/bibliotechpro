# 🤖 Bot de Telegram - BiblioTech Pro

Bot profesional de Telegram para consultas del catálogo, recomendaciones automáticas y notificaciones.

## 📋 Características

| Funcionalidad | Descripción |
|---------------|-------------|
| **📚 Catálogo** | Consulta y búsqueda de libros en tiempo real |
| **🤖 IA** | Recomendaciones personalizadas con Gemini AI |
| **⏰ Automático** | Recomendaciones cada hora a suscriptores |
| **🔔 Suscripciones** | Sistema de notificaciones opt-in |
| **📊 n8n** | Integración con workflows de automatización |

---

## 🎮 Comandos Disponibles

```
/start      - Bienvenida + tu Chat ID
/catalogo   - Ver todos los libros
/buscar X   - Buscar por título/autor
/categorias - Filtrar por categoría
/recomendar - Obtener recomendación IA ahora

/suscribir   - Activar notificaciones cada hora
/desuscribir - Desactivar notificaciones

/id    - Mostrar tu Chat ID
/ayuda - Lista de comandos
/about - Info del bot
```

---

## ⚙️ Configuración en VPS

### 1. Obtener Token de @BotFather

1. Abre Telegram y busca `@BotFather`
2. Envía `/newbot`
3. Sigue las instrucciones y guarda el token

### 2. Configurar Variables de Entorno

Edita `.env` en el VPS:

```bash
# Token del bot
TELEGRAM_BOT_TOKEN=tu_token_aqui

# Intervalo de recomendaciones (horas)
RECOMMENDATION_INTERVAL=1

# Chat ID admin (opcional, para alertas)
TELEGRAM_ADMIN_CHAT_ID=123456789
```

### 3. Desplegar

```bash
docker compose -f docker-compose-vps.yml up -d telegram-bot
```

### 4. Verificar Logs

```bash
docker logs -f bibliotech-telegram-bot
```

---

## 🆔 Obtener Chat ID

El Chat ID es necesario para:
- Recibir notificaciones admin
- Configurar workflows de n8n

**Cómo obtenerlo:**

1. Envía `/start` al bot
2. El bot responderá con tu Chat ID
3. O usa el comando `/id`

**Tipos de Chat ID:**
- **Positivo** (ej: `123456789`) → Chat privado
- **Negativo** (ej: `-1001234567890`) → Grupo

---

## 🔗 Integración con n8n

El bot puede recibir eventos desde n8n para:
- Notificar nuevas reservas
- Alertar devoluciones tardías
- Enviar recordatorios

### Webhook de n8n → Bot

Configura en n8n un nodo Telegram con las credenciales del bot.

### Bot → n8n

El bot puede disparar webhooks a n8n cuando:
- Un usuario se suscribe
- Se solicita una acción especial

---

## 📁 Archivos del Bot

```
bot/
├── bibliotech_bot.py   # Código principal
├── subscriptions.py    # Gestión de suscripciones
├── requirements.txt    # Dependencias Python
└── Dockerfile          # Imagen Docker
```

---

## 🐛 Troubleshooting

### Bot no responde

```bash
# Verificar que está corriendo
docker ps | grep telegram-bot

# Ver logs de errores
docker logs bibliotech-telegram-bot --tail 50
```

### Error de token

Verifica que `TELEGRAM_BOT_TOKEN` esté configurado correctamente en `.env`

### No llegan recomendaciones

1. Verifica que estás suscrito (`/suscribir`)
2. Comprueba logs del scheduler
3. Verifica conexión con el backend

---

## 📊 Métricas

El bot muestra estadísticas con `/about`:
- Número de suscriptores activos
- Intervalo de recomendaciones configurado
