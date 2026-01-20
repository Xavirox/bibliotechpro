#!/usr/bin/env python3
"""
BiblioTech Pro - Bot de Telegram
================================
Bot que permite consultar el catálogo de la biblioteca y obtener
recomendaciones personalizadas usando IA (Gemini).

Comandos disponibles:
    /start - Mensaje de bienvenida
    /ayuda - Lista de comandos
    /catalogo - Ver libros disponibles
    /buscar <término> - Buscar en el catálogo
    /recomendar - Obtener recomendación IA
    /disponibilidad <titulo> - Verificar disponibilidad

Autor: Xavier Aerox
Versión: 1.0.0
"""

import os
import logging
import asyncio
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import (
    Application,
    CommandHandler,
    MessageHandler,
    CallbackQueryHandler,
    ContextTypes,
    filters,
)
import requests

# =============================================================================
# CONFIGURACIÓN
# =============================================================================

# Token del bot (obtener de @BotFather)
BOT_TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "TU_TOKEN_AQUI")

# URL de la API del backend (cambiar según entorno)
API_BASE_URL = os.environ.get("API_URL", "http://localhost:9091/api")

# Configurar logging
logging.basicConfig(
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# =============================================================================
# FUNCIONES DE LA API
# =============================================================================

def get_libros(search_term=None, categoria=None):
    """Obtiene libros del catálogo."""
    try:
        params = {}
        if search_term:
            params["search"] = search_term
        if categoria:
            params["categoria"] = categoria
        
        response = requests.get(f"{API_BASE_URL}/libros", params=params, timeout=10)
        if response.status_code == 200:
            data = response.json()
            # Manejar paginación
            if isinstance(data, dict) and "content" in data:
                return data["content"]
            return data
        return []
    except Exception as e:
        logger.error(f"Error al obtener libros: {e}")
        return []

def get_recomendacion():
    """Obtiene recomendación de la IA."""
    try:
        # Esta llamada usaría tu endpoint de Gemini
        response = requests.post(
            f"{API_BASE_URL}/recomendaciones",
            json={"categorias": ["Novela", "Ciencia Ficción", "Fantasía"]},
            timeout=30
        )
        if response.status_code == 200:
            return response.json().get("recomendacion", "No hay recomendaciones disponibles.")
        return "No pude conectar con el servicio de recomendaciones."
    except Exception as e:
        logger.error(f"Error al obtener recomendación: {e}")
        return "Error al obtener recomendaciones. Inténtalo más tarde."

# =============================================================================
# HANDLERS DE COMANDOS
# =============================================================================

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Mensaje de bienvenida."""
    user = update.effective_user
    welcome_message = f"""
📚 *¡Bienvenido/a a BiblioTech Pro, {user.first_name}!*

Soy el asistente virtual de la biblioteca. Puedo ayudarte a:

• 📖 Consultar el catálogo de libros
• 🔍 Buscar títulos específicos  
• 🤖 Recomendarte lecturas con IA
• ✅ Verificar disponibilidad

*Comandos disponibles:*
/catalogo - Ver libros disponibles
/buscar <término> - Buscar en catálogo
/recomendar - Obtener sugerencia IA
/ayuda - Ver todos los comandos

¡Pregúntame lo que necesites! 📕
"""
    await update.message.reply_text(welcome_message, parse_mode="Markdown")

async def ayuda(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Muestra la lista de comandos."""
    help_text = """
📋 *Comandos Disponibles*

📚 *Catálogo:*
/catalogo - Ver todos los libros
/buscar <término> - Buscar por título/autor
/categorias - Ver categorías disponibles

🤖 *Inteligencia Artificial:*
/recomendar - Obtener recomendación personalizada

ℹ️ *Información:*
/ayuda - Mostrar esta ayuda
/about - Sobre BiblioTech Pro

También puedes escribir directamente el nombre de un libro para buscarlo.
"""
    await update.message.reply_text(help_text, parse_mode="Markdown")

async def catalogo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Muestra el catálogo de libros."""
    await update.message.reply_text("🔄 Consultando catálogo...")
    
    libros = get_libros()
    
    if not libros:
        await update.message.reply_text("📭 No hay libros disponibles en este momento.")
        return
    
    # Crear mensaje con los libros (máximo 10)
    mensaje = "📚 *Catálogo de BiblioTech Pro*\n\n"
    
    for i, libro in enumerate(libros[:10], 1):
        titulo = libro.get("titulo", "Sin título")
        autor = libro.get("autor", "Desconocido")
        categoria = libro.get("categoria", "General")
        
        mensaje += f"{i}. *{titulo}*\n"
        mensaje += f"   ✍️ {autor}\n"
        mensaje += f"   📂 {categoria}\n\n"
    
    if len(libros) > 10:
        mensaje += f"\n_...y {len(libros) - 10} libros más._\n"
        mensaje += "Usa /buscar <término> para filtrar."
    
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def buscar(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Busca libros por término."""
    if not context.args:
        await update.message.reply_text(
            "🔍 *Uso:* /buscar <término>\n\nEjemplo: `/buscar Tolkien`",
            parse_mode="Markdown"
        )
        return
    
    termino = " ".join(context.args)
    await update.message.reply_text(f"🔍 Buscando '{termino}'...")
    
    libros = get_libros(search_term=termino)
    
    if not libros:
        await update.message.reply_text(
            f"📭 No encontré resultados para '{termino}'.\n"
            "Prueba con otro término."
        )
        return
    
    mensaje = f"📚 *Resultados para '{termino}':*\n\n"
    
    for libro in libros[:5]:
        titulo = libro.get("titulo", "Sin título")
        autor = libro.get("autor", "Desconocido")
        mensaje += f"• *{titulo}* - {autor}\n"
    
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def recomendar(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Obtiene una recomendación de la IA."""
    await update.message.reply_text("🤖 Consultando a la IA para recomendarte algo especial...")
    
    recomendacion = get_recomendacion()
    
    mensaje = f"""
🤖 *Recomendación Personalizada*

{recomendacion}

_Generado por Gemini AI_
"""
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def categorias(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Muestra las categorías disponibles."""
    keyboard = [
        [
            InlineKeyboardButton("📖 Novela", callback_data="cat_Novela"),
            InlineKeyboardButton("🚀 Ciencia Ficción", callback_data="cat_Ciencia Ficción"),
        ],
        [
            InlineKeyboardButton("🧙 Fantasía", callback_data="cat_Fantasía"),
            InlineKeyboardButton("💻 Tecnología", callback_data="cat_Tecnología"),
        ],
        [
            InlineKeyboardButton("📜 Historia", callback_data="cat_Historia"),
            InlineKeyboardButton("👤 Biografía", callback_data="cat_Biografía"),
        ],
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    await update.message.reply_text(
        "📂 *Selecciona una categoría:*",
        reply_markup=reply_markup,
        parse_mode="Markdown"
    )

async def button_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Maneja los clicks en botones."""
    query = update.callback_query
    await query.answer()
    
    if query.data.startswith("cat_"):
        categoria = query.data[4:]
        libros = get_libros(categoria=categoria)
        
        if not libros:
            await query.edit_message_text(f"📭 No hay libros en la categoría '{categoria}'.")
            return
        
        mensaje = f"📚 *Libros de {categoria}:*\n\n"
        for libro in libros[:5]:
            mensaje += f"• *{libro.get('titulo')}* - {libro.get('autor')}\n"
        
        await query.edit_message_text(mensaje, parse_mode="Markdown")

async def about(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Información sobre el bot."""
    about_text = """
📚 *BiblioTech Pro Bot*
Versión 1.0.0

Desarrollado como proyecto para el curso ASIR.

*Tecnologías:*
• Python + python-telegram-bot
• Spring Boot (Backend API)
• Oracle Database
• Google Gemini AI

*Autor:* Xavier Aerox
*Curso:* 2025-2026
"""
    await update.message.reply_text(about_text, parse_mode="Markdown")

async def mensaje_texto(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Maneja mensajes de texto libres como búsquedas."""
    texto = update.message.text
    
    # Intentar buscar el texto como título de libro
    libros = get_libros(search_term=texto)
    
    if libros:
        libro = libros[0]
        mensaje = f"""
📖 *{libro.get('titulo')}*

✍️ *Autor:* {libro.get('autor', 'Desconocido')}
📂 *Categoría:* {libro.get('categoria', 'General')}
📅 *Año:* {libro.get('anio', 'N/A')}

Usa /catalogo para ver más libros.
"""
        await update.message.reply_text(mensaje, parse_mode="Markdown")
    else:
        await update.message.reply_text(
            f"🤔 No encontré '{texto}' en el catálogo.\n"
            "Prueba con /catalogo o /buscar <término>"
        )

# =============================================================================
# MAIN
# =============================================================================

def main():
    """Inicia el bot."""
    if BOT_TOKEN == "TU_TOKEN_AQUI":
        print("❌ ERROR: Configura TELEGRAM_BOT_TOKEN en las variables de entorno")
        print("   Obtén un token de @BotFather en Telegram")
        return
    
    print("🤖 Iniciando BiblioTech Pro Bot...")
    print(f"📡 API URL: {API_BASE_URL}")
    
    # Crear aplicación
    application = Application.builder().token(BOT_TOKEN).build()
    
    # Registrar handlers
    application.add_handler(CommandHandler("start", start))
    application.add_handler(CommandHandler("ayuda", ayuda))
    application.add_handler(CommandHandler("help", ayuda))
    application.add_handler(CommandHandler("catalogo", catalogo))
    application.add_handler(CommandHandler("buscar", buscar))
    application.add_handler(CommandHandler("recomendar", recomendar))
    application.add_handler(CommandHandler("categorias", categorias))
    application.add_handler(CommandHandler("about", about))
    application.add_handler(CallbackQueryHandler(button_callback))
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, mensaje_texto))
    
    # Iniciar bot
    print("✅ Bot iniciado. Presiona Ctrl+C para detener.")
    application.run_polling(allowed_updates=Update.ALL_TYPES)

if __name__ == "__main__":
    main()
