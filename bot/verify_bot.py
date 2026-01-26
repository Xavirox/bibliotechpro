import asyncio
import os
from telegram import Bot

async def main():
    token = "8227061124:AAGFQ-rvbEPEJnofN2rAI70omxzBWnKJMbs"
    chat_id = "-4870856336"
    bot = Bot(token)
    try:
        await bot.send_message(chat_id=chat_id, text="🧪 *Prueba Directa:* El bot tiene acceso a este grupo.\nSi ves esto, el problema es n8n.", parse_mode='Markdown')
        print("✅ Mensaje enviado correctamente")
    except Exception as e:
        print(f"❌ Error al enviar mensaje: {e}")

if __name__ == "__main__":
    asyncio.run(main())
