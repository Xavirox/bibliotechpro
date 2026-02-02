from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel, Field
from typing import List, Optional
import os
from google import genai
from google.genai import types
from dotenv import load_dotenv
import logging
import json

# Cargar variables de entorno
load_dotenv()

# Configurar Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Inicializar FastAPI
app = FastAPI(
    title="Servicio de IA BiblioTech",
    description="Microservicio para OCR y Recomendaciones usando Gemini",
    version="1.0.0"
)

# Configurar Cliente Gemini
CLAVE_API_GEMINI = os.getenv("GEMINI_API_KEY")
cliente_ia = None

if not CLAVE_API_GEMINI:
    logger.warning("GEMINI_API_KEY no establecida. Las funciones de IA fallarán.")
else:
    try:
        cliente_ia = genai.Client(api_key=CLAVE_API_GEMINI)
    except Exception as e:
         logger.error(f"Error al inicializar cliente Gemini: {e}")

# --- Modelos ---

class InformacionLibro(BaseModel):
    titulo: str
    autor: str
    categoria: str

class SolicitudRecomendacion(BaseModel):
    historial: List[InformacionLibro]
    catalogo: List[InformacionLibro]
    id_usuario: Optional[str] = None

class RecomendacionLibro(BaseModel):
    titulo: str
    autor: str
    categoria: str
    motivo: str

class RespuestaRecomendacion(BaseModel):
    recomendaciones: List[RecomendacionLibro]

# --- Rutas ---

@app.get("/health")
async def verificar_salud():
    return {"estado": "ok", "servicio": "servicio-ia"}

@app.post("/api/recomendar", response_model=RespuestaRecomendacion)
async def obtener_recomendaciones(solicitud: SolicitudRecomendacion):
    """
    Genera recomendaciones de libros usando Gemini con un prompt de bibliotecario especializado.
    """
    try:
        if not cliente_ia:
             raise HTTPException(status_code=503, detail="Servicio de IA no disponible (Falta Configuración)")

        # Serializar entrada a JSON para el prompt
        historial_json = json.dumps([l.dict() for l in solicitud.historial], ensure_ascii=False)
        catalogo_json = json.dumps([l.dict() for l in solicitud.catalogo], ensure_ascii=False)
        
        AI_TEMPERATURE = float(os.getenv("AI_TEMPERATURE", "0.3"))
        AI_MAX_RECS = int(os.getenv("AI_MAX_RECS", "5"))

        prompt = f"""
Eres un bibliotecario experto con décadas de experiencia curando lecturas personalizadas. Tu objetivo es recomendar la próxima lectura ideal basándote SOLO en los patrones de lectura previos y el catálogo disponible.

INSTRUCCIONES CRÍTICAS:
1. Analiza el [HISTORIAL_DE_LECTURA] para identificar:
   - Géneros preferidos.
   - Autores recurrentes.
   - Temas o tonos (ej. oscuro, educativo, ligero).
2. Busca coincidencias en el [CATALOGO_DISPONIBLE] que encajen con estos patrones.
3. NO inventes libros. Solo recomienda libros que existan EXACTAMENTE en el catálogo.
4. NO uses datos personales. El usuario es anónimo.
5. Selecciona un MÁXIMO de {AI_MAX_RECS} recomendaciones.
6. Tu respuesta debe ser EXCLUSIVAMENTE un objeto JSON válido, sin texto antes ni después.

FORMATO DE RESPUESTA (JSON Schema):
{{
  "recomendaciones": [
    {{
      "titulo": "String (Exactamente como en el catálogo)",
      "autor": "String",
      "categoria": "String",
      "motivo": "String (Breve explicación de 1 frase conectando con el historial: 'Porque te gustó X, disfrutarás Y por Z')"
    }}
  ]
}}

[HISTORIAL_DE_LECTURA]:
{historial_json}

[CATALOGO_DISPONIBLE]:
{catalogo_json}
        """
        
        # Configurar generación para respuesta JSON
        configuracion = types.GenerateContentConfig(
            temperature=AI_TEMPERATURE,
            response_mime_type="application/json",
            response_schema=RespuestaRecomendacion
        )

        respuesta = cliente_ia.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt,
            config=configuracion
        )
        
        # Parsear respuesta
        try:
             recomendaciones = json.loads(respuesta.text)
             return recomendaciones
        except json.JSONDecodeError:
             logger.error(f"Error al parsear JSON de Gemini: {respuesta.text}")
             raise HTTPException(status_code=500, detail="IA devolvió un formato inválido")

    except Exception as e:
        logger.error(f"Error generando recomendaciones: {e}")
        raise HTTPException(status_code=500, detail=f"Fallo en Procesamiento de IA: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
