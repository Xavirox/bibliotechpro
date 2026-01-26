from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Optional
import os
from google import genai
from google.genai import types
from dotenv import load_dotenv
import logging
import json

# Load environment variables
load_dotenv()

# Configure Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI
app = FastAPI(
    title="BiblioTech AI Service",
    description="Microservice for OCR and AI Recommendations using Gemini",
    version="1.0.0"
)

# Configure Gemini Client
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
client = None

if not GEMINI_API_KEY:
    logger.warning("GEMINI_API_KEY not set. AI features will fail.")
else:
    try:
        client = genai.Client(api_key=GEMINI_API_KEY)
    except Exception as e:
         logger.error(f"Failed to initialize Gemini Client: {e}")

# --- Models ---

class BookInfo(BaseModel):
    titulo: str
    autor: str
    categoria: str

class RecommendationRequest(BaseModel):
    history: List[BookInfo]
    catalog: List[BookInfo]
    user_id: Optional[str] = None

class BookRecommendation(BaseModel):
    titulo: str
    autor: str
    categoria: str
    motivo: str

class RecommendationResponse(BaseModel):
    recomendaciones: List[BookRecommendation]

# --- Routes ---

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "ai-service"}

@app.post("/api/recommend", response_model=RecommendationResponse)
async def get_recommendations(request: RecommendationRequest):
    """
    Generates book recommendations using Gemini with the specialized librarian prompt.
    """
    try:
        if not client:
             raise HTTPException(status_code=503, detail="AI Service unavailable (Missing Config)")

        # Serialize input to JSON for the prompt
        history_json = json.dumps([b.dict() for b in request.history], ensure_ascii=False)
        catalog_json = json.dumps([b.dict() for b in request.catalog], ensure_ascii=False)
        
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
5. Selecciona un MÁXIMO de 5 recomendaciones.
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
{history_json}

[CATALOGO_DISPONIBLE]:
{catalog_json}
        """
        
        # Configure generation config for JSON response
        config = types.GenerateContentConfig(
            temperature=0.3,
            response_mime_type="application/json",
            response_schema=RecommendationResponse
        )

        response = client.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt,
            config=config
        )
        
        # Parse response
        try:
             # The SDK with response_mime_type='application/json' should return parsed object or valid json string
             recommendations = json.loads(response.text)
             return recommendations
        except json.JSONDecodeError:
             logger.error(f"Failed to parse JSON from Gemini: {response.text}")
             raise HTTPException(status_code=500, detail="AI returned invalid format")

    except Exception as e:
        logger.error(f"Error generating recommendations: {e}")
        raise HTTPException(status_code=500, detail=f"AI Processing Failed: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
