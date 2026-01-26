
import logging
import httpx
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, ValidationError
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type, before_sleep_log
from async_lru import alru_cache

# Configure logger
logger = logging.getLogger(__name__)

# --- Models ---

class Libro(BaseModel):
    """Modelo de datos para un libro."""
    id_libro: Optional[int] = Field(None, alias="id")
    isbn: str = Field(default="")
    titulo: str
    autor: str
    categoria: str = Field(default="General")
    anio: Optional[int] = None

class PaginaLibros(BaseModel):
    """Modelo para respuesta paginada."""
    content: List[Libro]
    totalElements: int
    totalPages: int

# --- Service ---

class LibraryService:
    """
    Servicio para interactuar con la API de la Biblioteca y el servicio de Recomendación (IA).
    Incluye optimizaciones: Caché, Retries y Timeouts configurados.
    """

    def __init__(self, api_base_url: str):
        self.api_base_url = api_base_url.rstrip("/")
        # Timeout optimizado: 5s para conectar, 30s para leer
        # Esto evita bloqueos largos si el servidor está caído.
        timeout = httpx.Timeout(30.0, connect=5.0)
        self.http_client = httpx.AsyncClient(timeout=timeout)

    async def close(self):
        """Cierra el cliente HTTP."""
        await self.http_client.aclose()

    # --- Cache Decorator Note ---
    # alru_cache usa hash(self) por defecto. Como LibraryService es Singleton, funciona bien.
    # Cacheamos por 5 minutos (ttl=300) y guardamos hasta 128 consultas distintas.
    
    @alru_cache(maxsize=128, ttl=300)
    @retry(
        retry=retry_if_exception_type((httpx.ConnectError, httpx.ConnectTimeout, httpx.ReadTimeout)),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        before_sleep=before_sleep_log(logger, logging.WARNING)
    )
    async def get_libros(self, search_term: Optional[str] = None, categoria: Optional[str] = None, limit: Optional[int] = None) -> List[Libro]:
        """
        Obtiene libros del catálogo con caché y reintentos automáticos.
        """
        params: Dict[str, Any] = {}
        if search_term:
            params["search"] = search_term
        if categoria:
            params["categoria"] = categoria
        if limit:
            params["size"] = str(limit)

        try:
            response = await self.http_client.get(f"{self.api_base_url}/libros", params=params)
            response.raise_for_status()
            
            data = response.json()
            return self._parse_libros_response(data)

        except httpx.HTTPStatusError as e:
            # 404 no se debe reintentar, pero 5xx sí.
            # Aquí capturamos errores de status para loguear, tenacity maneja los de conexión arriba.
            # Si queremos reintentar 500, deberíamos lanzar una excepción que tenacity capture, 
            # o incluir HTTPStatusError en el retry si el código es >= 500.
            if e.response.status_code >= 500:
                raise e # Dejar que tenacity capture si lo configuramos, o manejarlo como error temporal
            logger.warning(f"Error HTTP {e.response.status_code} al obtener libros: {e}")
            return []
        except (httpx.RequestError, ValidationError, Exception) as e:
            # Estos errores finales son capturados si tenacity se rinde o si no son de conexión
            logger.error(f"Error obteniendo libros (params={params}): {e}")
            raise e # Relanzar para que tenacity funcione correctamente o devolver [] si agotado?
            # Diseño: El decorador retry envuelve la función. Si falla 3 veces, lanzará la excepción.
            # Aquí atrapamos excepciones GENÉRICAS para no romper el bot, pero tenacity necesita ver la excepción.
            # SOLUCIÓN: Quitamos el try/except general para que tenacity haga su trabajo,
            # y envolvemos la llamada en el handler/consumidor o usamos un wrapper seguro.
            # MANTENDRÉ el try-except para devolver lista vacía en fallos finales, 
            # PERO para que tenacity funcione el try/except debe estar FUERA o re-lanzar.
            # Ajuste: Tenacity 'retry' envuelve la ejecución. Si hay try-except DENTRO que traga el error, tenacity NO se entera.
            # Voy a quitar el try-except de conexión y dejar solo validación/status.
            pass
        
        # Implementación corregida para Tenacity:
        # Tenacity debe ver la excepción para reintentar.
        # Si falla todo, queremos devolver lista vacía en lugar de romper. 
        # Pero @retry decora la función, así que si falla 3 veces, lanzará excepción al llamador.
        # El llamador (handler) deberá manejarlo. 
        # O mejor: Hacemos una función interna _fetch_libros decorada y esta pública maneja el error final.
        return []

    # --- Refactor for Tenacity Correctness ---
    
    @alru_cache(maxsize=128, ttl=300)
    async def get_libros_cached(self, search_term: Optional[str] = None, categoria: Optional[str] = None, limit: Optional[int] = None) -> List[Libro]:
        """Wrapper público para la búsqueda con protección de errores finales."""
        try:
            return await self._fetch_libros_retry(search_term, categoria, limit)
        except Exception as e:
            logger.error(f"Fallo definitivo obteniendo libros (tras reintentos): {e}")
            return []

    @retry(
        retry=retry_if_exception_type((httpx.ConnectError, httpx.ConnectTimeout, httpx.ReadTimeout, httpx.PoolTimeout, httpx.NetworkError)),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=5),
        before_sleep=before_sleep_log(logger, logging.WARNING)
    )
    async def _fetch_libros_retry(self, search_term: Optional[str], categoria: Optional[str], limit: Optional[int]) -> List[Libro]:
        """Función interna con lógica de reintento."""
        params: Dict[str, Any] = {}
        if search_term: params["search"] = search_term
        if categoria: params["categoria"] = categoria
        if limit: params["size"] = str(limit)

        response = await self.http_client.get(f"{self.api_base_url}/libros", params=params)
        response.raise_for_status()
        
        data = response.json()
        return self._parse_libros_response(data)

    # Alias para mantener compatibilidad con handlers existentes,
    # reemplazando la implementación original por la versión optimizada.
    get_libros = get_libros_cached

    def _parse_libros_response(self, data: Any) -> List[Libro]:
        if isinstance(data, list):
            return [Libro(**item) for item in data]
        if isinstance(data, dict) and "content" in data:
            return PaginaLibros(**data).content
        return []

    # Recomendaciones: NO cacheamos (queremos variedad), pero SÍ reintentamos conexión.
    @retry(
        retry=retry_if_exception_type((httpx.ConnectError, httpx.ReadTimeout, httpx.NetworkError)),
        stop=stop_after_attempt(2), # Menos intentos para no bloquear UI
        wait=wait_exponential(multiplier=1, min=1, max=3)
    )
    async def get_recomendacion(self, categorias_interes: List[str]) -> str:
        """Obtiene recomendación de la IA con reintentos."""
        try:
            response = await self.http_client.post(
                f"{self.api_base_url}/recomendaciones",
                json={"categorias": categorias_interes},
                timeout=45.0 # Timeout específico para IA
            )
            response.raise_for_status()
            data = response.json()
            return data.get("recomendacion", "No hay recomendaciones disponibles.")
        except Exception as e:
            # Aquí capturamos para logging, pero si es de conexión re-lanzamos para tenacity
            if isinstance(e, (httpx.ConnectError, httpx.ReadTimeout, httpx.NetworkError)):
                raise e 
            logger.error(f"Error IA: {e}")
            return "El servicio de IA no está disponible temporalmente."

    async def get_libros_destacados(self, limit: int = 5) -> List[Libro]:
        """Obtiene libros destacados (usa caché de get_libros)."""
        return await self.get_libros(limit=limit)
