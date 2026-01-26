
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ContextTypes
from subscriptions import subscription_manager
from config import config
from services import LibraryService

# --- Helpers ---

def get_service(context: ContextTypes.DEFAULT_TYPE) -> LibraryService:
    """Helper para obtener el servicio desde bot_data."""
    return context.bot_data["service"]

# --- Command Handlers ---

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Mensaje de bienvenida."""
    if not update.effective_user or not update.message: return

    user = update.effective_user
    chat_id = update.effective_chat.id if update.effective_chat else "N/A"
    
    welcome_message = f"""
📚 *¡Bienvenido/a a BiblioTech Pro, {user.first_name}!*

Soy el asistente virtual de la biblioteca. Puedo ayudarte a:

• 📖 Consultar el catálogo de libros
• 🔍 Buscar títulos específicos  
• 🤖 Recomendarte lecturas con IA
• 🔔 Enviarte notificaciones periódicas

*Comandos disponibles:*
/catalogo - Ver libros disponibles
/buscar <término> - Buscar en catálogo
/recomendar - Obtener sugerencia IA
/suscribir - Recibir recomendaciones cada hora
/desuscribir - Dejar de recibir notificaciones
/id - Ver tu Chat ID

📍 *Tu Chat ID:* `{chat_id}`
"""
    await update.message.reply_text(welcome_message, parse_mode="Markdown")

async def ayuda(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Muestra la lista de comandos."""
    if not update.message: return

    suscriptores = await subscription_manager.get_subscriber_count()
    
    help_text = f"""
📋 *Comandos Disponibles*

📚 *Catálogo:*
/catalogo - Ver todos los libros
/buscar <término> - Buscar por título/autor
/categorias - Ver categorías disponibles

🤖 *Inteligencia Artificial:*
/recomendar - Obtener recomendación ahora

🔔 *Notificaciones:*
/suscribir - Activar recomendaciones cada hora
/desuscribir - Desactivar notificaciones
_({suscriptores} usuarios suscritos)_

ℹ️ *Información:*
/id - Mostrar tu Chat ID
/about - Sobre BiblioTech Pro
"""
    await update.message.reply_text(help_text, parse_mode="Markdown")

async def get_id(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Muestra el Chat ID."""
    if not update.message or not update.effective_chat: return
    
    chat = update.effective_chat
    mensaje = f"🔑 *Chat ID:* `{chat.id}`\n📝 *Tipo:* {chat.type}"
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def suscribir(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Suscribe al usuario."""
    if not update.message or not update.effective_chat: return
    
    chat_id = update.effective_chat.id
    username = update.effective_user.first_name or "Usuario" if update.effective_user else "Usuario"
    
    es_nuevo = await subscription_manager.subscribe(chat_id, username)
    
    if es_nuevo:
        mensaje = f"✅ *¡Suscripción activada!*\nRecibirás recomendaciones cada *{config.RECOMMENDATION_INTERVAL_HOURS} hora(s)*."
    else:
        mensaje = "ℹ️ Ya estás suscrito a las notificaciones."
    
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def desuscribir(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Desuscribe al usuario."""
    if not update.message or not update.effective_chat: return
    
    chat_id = update.effective_chat.id
    fue_desuscrito = await subscription_manager.unsubscribe(chat_id)
    
    mensaje = "🔕 *Suscripción cancelada*" if fue_desuscrito else "ℹ️ No estabas suscrito."
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def catalogo(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Muestra el catálogo."""
    if not update.message: return

    await update.message.reply_text("🔄 Consultando catálogo...")
    
    service = get_service(context)
    libros = await service.get_libros(limit=10)
    
    if not libros:
        await update.message.reply_text("📭 No hay libros disponibles.")
        return
    
    mensaje = "📚 *Catálogo de BiblioTech Pro*\n\n"
    for i, libro in enumerate(libros, 1):
        mensaje += f"{i}. *{libro.titulo}*\n   ✍️ {libro.autor}\n   📂 {libro.categoria}\n\n"
    
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def buscar(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Busca libros."""
    if not update.message: return
    
    if not context.args:
        await update.message.reply_text("🔍 *Uso:* /buscar <término>", parse_mode="Markdown")
        return
    
    termino = " ".join(context.args)
    await update.message.reply_text(f"🔍 Buscando '{termino}'...")
    
    service = get_service(context)
    libros = await service.get_libros(search_term=termino, limit=5)
    
    if not libros:
        await update.message.reply_text(f"📭 Sin resultados para '{termino}'.")
        return
    
    mensaje = f"📚 *Resultados para '{termino}':*\n\n"
    for libro in libros:
        mensaje += f"• *{libro.titulo}* - {libro.autor}\n"
    
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def recomendar(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Obtiene recomendación de IA."""
    if not update.message: return

    await update.message.reply_text("🤖 Consultando a la IA...")
    
    service = get_service(context)
    recomendacion = await service.get_recomendacion(["Novela", "Ciencia Ficción", "Tecnología"])
    
    mensaje = f"🤖 *Recomendación Personalizada*\n\n{recomendacion}\n\n_Generado por Gemini AI_"
    await update.message.reply_text(mensaje, parse_mode="Markdown")

async def categorias(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Muestra categorías."""
    if not update.message: return

    keyboard = [
        [InlineKeyboardButton("📖 Novela", callback_data="cat_Novela"),
         InlineKeyboardButton("🚀 Ciencia Ficción", callback_data="cat_Ciencia Ficción")],
        [InlineKeyboardButton("🧙 Fantasía", callback_data="cat_Fantasía"),
         InlineKeyboardButton("💻 Tecnología", callback_data="cat_Tecnología")],
        [InlineKeyboardButton("📜 Historia", callback_data="cat_Historia"),
         InlineKeyboardButton("👤 Biografía", callback_data="cat_Biografía")],
    ]
    await update.message.reply_text("📂 *Selecciona una categoría:*", reply_markup=InlineKeyboardMarkup(keyboard), parse_mode="Markdown")

async def button_callback(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Maneja botones."""
    if not update.callback_query: return
    
    query = update.callback_query
    await query.answer()
    
    if query.data and query.data.startswith("cat_"):
        categoria = query.data[4:]
        service = get_service(context)
        libros = await service.get_libros(categoria=categoria, limit=5)
        
        if not libros:
            await query.edit_message_text(f"📭 No hay libros en '{categoria}'.")
            return
        
        mensaje = f"📚 *Libros de {categoria}:*\n\n"
        for libro in libros:
            mensaje += f"• *{libro.titulo}* - {libro.autor}\n"
        
        await query.edit_message_text(mensaje, parse_mode="Markdown")

async def about(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Info del bot."""
    if not update.message: return
    suscriptores = await subscription_manager.get_subscriber_count()
    await update.message.reply_text(f"📚 *BiblioTech Pro Bot v3.0*\n📊 Suscriptores: {suscriptores}", parse_mode="Markdown")

async def mensaje_texto(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Maneja texto libre como búsqueda."""
    if not update.message or not update.message.text: return
    
    texto = update.message.text
    service = get_service(context)
    libros = await service.get_libros(search_term=texto, limit=1)
    
    if libros:
        libro = libros[0]
        mensaje = f"📖 *{libro.titulo}*\n✍️ {libro.autor}\n📂 {libro.categoria}"
        await update.message.reply_text(mensaje, parse_mode="Markdown")
    else:
        await update.message.reply_text(f"🤔 No encontré '{texto}'. Prueba /catalogo.")
