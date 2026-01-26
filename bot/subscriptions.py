#!/usr/bin/env python3
"""
Gestión de suscripciones para notificaciones del bot.
Usa archivo JSON para persistencia simple.
"""

import json
import os
import asyncio
from typing import Dict, List, Optional
from dataclasses import dataclass, asdict
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

# Ruta del archivo de suscripciones
SUBSCRIPTIONS_FILE = os.environ.get("SUBSCRIPTIONS_FILE", "/app/data/subscriptions.json")


@dataclass
class Subscriber:
    """Modelo de un suscriptor."""
    chat_id: int
    username: str
    subscribed_at: str
    notifications_enabled: bool = True


class SubscriptionManager:
    """Gestiona las suscripciones de usuarios a notificaciones."""
    
    def __init__(self, file_path: str = SUBSCRIPTIONS_FILE):
        self.file_path = file_path
        self._subscribers: Dict[int, Subscriber] = {}
        self._lock = asyncio.Lock()
        self._ensure_directory()
        self._load_sync()  # Carga inicial síncrona
    
    def _ensure_directory(self) -> None:
        """Crea el directorio si no existe."""
        directory = os.path.dirname(self.file_path)
        if directory and not os.path.exists(directory):
            try:
                os.makedirs(directory, exist_ok=True)
                logger.info(f"Directorio creado: {directory}")
            except Exception as e:
                logger.warning(f"No se pudo crear directorio: {e}")
                # Fallback a directorio actual
                self.file_path = "subscriptions.json"
    
    def _load_sync(self) -> None:
        """Carga suscripciones de forma síncrona (para inicialización)."""
        if not os.path.exists(self.file_path):
            logger.info("Archivo de suscripciones no existe, iniciando vacío")
            return
        
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                for chat_id_str, sub_data in data.items():
                    chat_id = int(chat_id_str)
                    self._subscribers[chat_id] = Subscriber(**sub_data)
            logger.info(f"Cargados {len(self._subscribers)} suscriptores")
        except Exception as e:
            logger.error(f"Error cargando suscripciones: {e}")
    
    async def _save(self) -> None:
        """Guarda suscripciones a disco de forma asíncrona."""
        async with self._lock:
            try:
                data = {
                    str(chat_id): asdict(sub) 
                    for chat_id, sub in self._subscribers.items()
                }
                # Usar escritura síncrona envuelta en executor para evitar dependencias
                loop = asyncio.get_event_loop()
                await loop.run_in_executor(None, self._write_file, data)
                logger.debug(f"Suscripciones guardadas: {len(data)}")
            except Exception as e:
                logger.error(f"Error guardando suscripciones: {e}")
    
    def _write_file(self, data: dict) -> None:
        """Escribe archivo de forma síncrona (para usar en executor)."""
        with open(self.file_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
    
    async def subscribe(self, chat_id: int, username: str) -> bool:
        """
        Suscribe un usuario a las notificaciones.
        
        Returns:
            True si es nueva suscripción, False si ya estaba suscrito.
        """
        if chat_id in self._subscribers:
            # Ya suscrito, solo reactivar si estaba desactivado
            if not self._subscribers[chat_id].notifications_enabled:
                self._subscribers[chat_id].notifications_enabled = True
                await self._save()
                return True
            return False
        
        self._subscribers[chat_id] = Subscriber(
            chat_id=chat_id,
            username=username or "Usuario",
            subscribed_at=datetime.now().isoformat(),
            notifications_enabled=True
        )
        await self._save()
        logger.info(f"Nuevo suscriptor: {username} (chat_id: {chat_id})")
        return True
    
    async def unsubscribe(self, chat_id: int) -> bool:
        """
        Desuscribe un usuario de las notificaciones.
        
        Returns:
            True si se desuscribió, False si no estaba suscrito.
        """
        if chat_id not in self._subscribers:
            return False
        
        self._subscribers[chat_id].notifications_enabled = False
        await self._save()
        logger.info(f"Usuario desuscrito: {chat_id}")
        return True
    
    async def get_active_subscribers(self) -> List[int]:
        """Retorna lista de chat_ids con notificaciones activas."""
        return [
            chat_id 
            for chat_id, sub in self._subscribers.items() 
            if sub.notifications_enabled
        ]
    
    async def is_subscribed(self, chat_id: int) -> bool:
        """Verifica si un usuario está suscrito y activo."""
        sub = self._subscribers.get(chat_id)
        return sub is not None and sub.notifications_enabled
    
    async def get_subscriber_count(self) -> int:
        """Retorna el número de suscriptores activos."""
        return len([s for s in self._subscribers.values() if s.notifications_enabled])


# Instancia global (singleton)
subscription_manager = SubscriptionManager()
