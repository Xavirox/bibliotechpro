
import logging
import asyncio
from datetime import datetime
from telegram.ext import Application
from subscriptions import subscription_manager
from services import LibraryService

logger = logging.getLogger(__name__)

async def job_recomendacion_periodica(telegram_app: Application, service: LibraryService) -> None:
    """
    Job que se ejecuta periódicamente para enviar recomendaciones.
    """
    subscribers = await subscription_manager.get_active_subscribers()
    if not subscribers:
        logger.info("No hay suscriptores activos para recomendaciones")
        return
    
    logger.info(f"Enviando recomendación a {len(subscribers)} suscriptores")
    
    # Obtener datos
    recomendacion = await service.get_recomendacion(["General", "Tecnología", "Novela"])
    libros = await service.get_libros_destacados(limit=3)
    
    hora_actual = datetime.now().strftime("%H:%M")
    mensaje = f"""
📚 *Recomendación BiblioTech* ({hora_actual})

🤖 *Sugerencia de la IA:*
{recomendacion}
"""
    if libros:
        mensaje += "\n📖 *Libros destacados:*\n"
        for libro in libros:
            mensaje += f"• _{libro.titulo}_ - {libro.autor}\n"
    
    mensaje += "\n_Usa /desuscribir para dejar de recibir estas notificaciones_"
    
    # Enviar
    bot = telegram_app.bot
    enviados = 0
    errores = 0
    
    for chat_id in subscribers:
        try:
            await bot.send_message(chat_id=chat_id, text=mensaje, parse_mode="Markdown")
            enviados += 1
            await asyncio.sleep(0.1) # Rate limiting
        except Exception as e:
            logger.error(f"Error enviando a {chat_id}: {e}")
            errores += 1
            
    logger.info(f"Recomendación enviada: {enviados} OK, {errores} errores")
