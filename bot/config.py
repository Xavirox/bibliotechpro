
import os
from dataclasses import dataclass

@dataclass
class Config:
    """Configuración centralizada del Bot."""
    BOT_TOKEN: str = os.environ.get("TELEGRAM_BOT_TOKEN", "")
    API_BASE_URL: str = os.environ.get("API_URL", "http://localhost:9091/api")
    RECOMMENDATION_INTERVAL_HOURS: int = int(os.environ.get("RECOMMENDATION_INTERVAL", "1"))
    N8N_WEBHOOK_URL: str = os.environ.get("N8N_WEBHOOK_URL", "")
    SUBSCRIPTIONS_FILE: str = os.environ.get("SUBSCRIPTIONS_FILE", "subscriptions.json")

    @property
    def is_configured(self) -> bool:
        return bool(self.BOT_TOKEN and self.BOT_TOKEN != "TU_TOKEN_AQUI")

config = Config()
