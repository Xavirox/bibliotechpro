
#!/usr/bin/env python3
"""
BiblioTech Pro - Bot de Telegram Profesional
=============================================
Entrada principal refactorizada.
"""

import logging
import warnings
from telegram import Update
from telegram.ext import (
    Application,
    CommandHandler,
    MessageHandler,
    CallbackQueryHandler,
    filters,
)
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger

# Módulos propios
from config import config
from services import LibraryService
import handlers
import jobs

# Ignorar advertencia específica de python-telegram-bot si es necesario
warnings.filterwarnings("ignore", category=UserWarning)

# Configurar logging
logging.basicConfig(
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# --- Scheduler Init ---

async def post_init(application: Application) -> None:
    """Configura e inicia el scheduler tras inicializar la App."""
    service: LibraryService = application.bot_data["service"]
    
    scheduler = AsyncIOScheduler(timezone="Europe/Madrid")
    
    # Inyectamos dependencias al job
    scheduler.add_job(
        jobs.job_recomendacion_periodica,
        trigger=IntervalTrigger(hours=config.RECOMMENDATION_INTERVAL_HOURS),
        id="recomendacion_periodica",
        name="Envío de recomendaciones periódicas",
        replace_existing=True,
        args=[application, service] # Inyección de dependencias
    )
    scheduler.start()
    logger.info(f"[SCHEDULER] ✓ Iniciado (Intervalo: {config.RECOMMENDATION_INTERVAL_HOURS}h)")

# --- Main ---

def main() -> None:
    """Función principal."""
    if not config.is_configured:
        logger.error("Token de Telegram no configurado.")
        return

    print("=" * 60)
    print("  📚 BiblioTech Pro Bot v3.1.0 (Clean Code)")
    print("=" * 60)
    
    # Instanciar servicio central
    service = LibraryService(api_base_url=config.API_BASE_URL)

    # Construir aplicación
    application = (
        Application.builder()
        .token(config.BOT_TOKEN)
        .post_init(post_init)
        .build()
    )
    
    # Inyectar servicio en bot_data para acceso en handlers
    application.bot_data["service"] = service

    # Registrar handlers desde módulo externo
    application.add_handler(CommandHandler("start", handlers.start))
    application.add_handler(CommandHandler("ayuda", handlers.ayuda))
    application.add_handler(CommandHandler("help", handlers.ayuda))
    application.add_handler(CommandHandler("id", handlers.get_id))
    application.add_handler(CommandHandler("catalogo", handlers.catalogo))
    application.add_handler(CommandHandler("buscar", handlers.buscar))
    application.add_handler(CommandHandler("recomendar", handlers.recomendar))
    application.add_handler(CommandHandler("categorias", handlers.categorias))
    application.add_handler(CommandHandler("suscribir", handlers.suscribir))
    application.add_handler(CommandHandler("desuscribir", handlers.desuscribir))
    application.add_handler(CommandHandler("about", handlers.about))
    
    application.add_handler(CallbackQueryHandler(handlers.button_callback))
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handlers.mensaje_texto))
    
    print("[BOT] ✓ Iniciando polling...")
    
    try:
        application.run_polling(allowed_updates=Update.ALL_TYPES)
    finally:
        # Cleanup al cerrar
        # Nota: run_polling maneja su propio loop, pero si necesitamos cerrar conex explícitas:
        # await service.close() (difícil de incrustar en run_polling síncrono, 
        # normalmente se hace en un shutdown hook o post_shutdown)
        pass

if __name__ == "__main__":
    main()
