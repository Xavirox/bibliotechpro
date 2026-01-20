
package com.biblioteca.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biblioteca.model.Libro;
import com.biblioteca.model.Prestamo;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.config.GeminiProperties;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final GeminiProperties geminiProperties;
    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;

    public GeminiService(GeminiProperties geminiProperties, PrestamoRepository prestamoRepository,
            LibroRepository libroRepository) {
        this.geminiProperties = geminiProperties;
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
    }

    public String getRecomendaciones(Long idSocio) {
        // 1. Get user history
        List<Prestamo> prestamos = prestamoRepository.findBySocioIdSocio(idSocio);
        String history = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getTitulo() + " (" + p.getEjemplar().getLibro().getCategoria()
                        + ")")
                .distinct()
                .collect(Collectors.joining(", "));

        if (history.isEmpty()) {
            return "[{\"titulo\": \"Explora\", \"razon\": \"No tienes historial aún. ¡Empieza a leer!\"}]";
        }

        // 2. Get available catalog (titles only to save tokens)
        // 2. Get available catalog (titles only to save tokens)
        // SEGURIDAD: Optimizacion para prevenir DoS (evitar cargar todo el catalogo en
        // memoria)
        List<Libro> libros = libroRepository.findRandomBooks(50);
        String catalog = libros.stream()
                .map(l -> l.getTitulo())
                .limit(50) // Limit to avoid context window issues
                .collect(Collectors.joining(", "));

        // 3. Construct Prompt
        String prompt = "Act as an expert librarian AI. " +
                "User's Reading History (Book Title + Category): [" + history + "]. " +
                "Available Library Catalog: [" + catalog + "]. " +
                "Task: Recommend exactly 3 books from the 'Available Library Catalog' that best match the user's reading taste based on their history. "
                +
                "If the history is empty, recommend 3 popular or classic books from the catalog. " +
                "Output STRICT JSON format (no markdown code blocks) with this structure: " +
                "[{\"titulo\": \"Exact Title from Catalog\", \"razon\": \"A compelling, personalized reason in Spanish explaining why this book fits their taste (approx 1 sentence).\"}]";

        // 4. Call Gemini API
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[] { part });
        requestBody.put("contents", new Object[] { content });

        // Add Generation Config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 500);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Fix: Use URI to prevent RestTemplate from encoding the colon in
            // "gemini-1.5-flash:generateContent"
            java.net.URI uri = java.net.URI.create(GEMINI_URL + "?key=" + geminiProperties.getKey());

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    java.util.Objects.requireNonNull(uri),
                    java.util.Objects.requireNonNull(HttpMethod.POST),
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> responseBody = response.getBody();
            String rawText = extractTextFromResponse(responseBody);

            // Clean up potential Markdown
            if (rawText.startsWith("```json")) {
                rawText = rawText.substring(7);
            }
            if (rawText.startsWith("```")) {
                rawText = rawText.substring(3);
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.substring(0, rawText.length() - 3);
            }

            // SEGURIDAD H-04: Sanitizar valores individuales para XSS sin romper JSON
            return sanitizeJsonRecommendations(rawText.trim());
        } catch (org.springframework.web.client.RestClientResponseException e) {
            logger.error("Gemini API Error Status: {}", e.getStatusCode());
            logger.error("Gemini API Error Body: {}", e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 429) {
                logger.info("API quota exceeded, using algorithmic fallback");
                return getAlgorithmicRecommendations(idSocio, prestamos);
            }
            // SEGURIDAD H-05: No exponer detalles de error al cliente
            // Solo loguear internamente, devolver mensaje genérico
            return "[{\"titulo\": \"Servicio no disponible\", \"razon\": \"No pudimos obtener recomendaciones en este momento. Inténtalo más tarde.\"}]";
        } catch (Exception e) {
            logger.error("Internal Error in GeminiService", e);
            return getAlgorithmicRecommendations(idSocio, prestamos);
        }
    }

    /**
     * Algorithmic fallback for when Gemini API is unavailable.
     * Recommends books based on categories the user has read.
     */
    private String getAlgorithmicRecommendations(Long idSocio, List<Prestamo> prestamos) {
        logger.info("Generating algorithmic recommendations for user {}", idSocio);

        // Get categories from user's reading history
        java.util.Set<String> readCategories = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getCategoria())
                .collect(Collectors.toSet());

        // Get IDs of books already read
        java.util.Set<Long> readBookIds = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getIdLibro())
                .collect(Collectors.toSet());

        // Find unread books in same categories
        List<Libro> allBooks = libroRepository.findAll();
        java.util.Collections.shuffle(allBooks);

        List<Libro> recommendations = allBooks.stream()
                .filter(l -> readCategories.contains(l.getCategoria()))
                .filter(l -> !readBookIds.contains(l.getIdLibro()))
                .limit(3)
                .toList();

        // If not enough books in same categories, add random ones
        List<Libro> finalRecommendations = new java.util.ArrayList<>(recommendations);
        if (finalRecommendations.size() < 3) {
            List<Libro> extras = allBooks.stream()
                    .filter(l -> !readBookIds.contains(l.getIdLibro()))
                    .filter(l -> !finalRecommendations.contains(l))
                    .limit(3 - finalRecommendations.size())
                    .toList();
            finalRecommendations.addAll(extras);
        }

        // Build JSON response
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < finalRecommendations.size(); i++) {
            Libro libro = finalRecommendations.get(i);
            String reason = generateReason(libro, readCategories);
            json.append("{\"titulo\": \"").append(libro.getTitulo())
                    .append("\", \"razon\": \"").append(reason).append("\"}");
            if (i < finalRecommendations.size() - 1)
                json.append(", ");
        }
        json.append("]");

        return json.toString();
    }

    private String generateReason(Libro libro, java.util.Set<String> readCategories) {
        if (readCategories.contains(libro.getCategoria())) {
            return "Basado en tu interés por " + libro.getCategoria() + ", te recomendamos esta obra de "
                    + libro.getAutor() + ".";
        } else {
            return "Una lectura destacada de " + libro.getAutor() + " que podría interesarte.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Invalid response from Gemini");
        }
    }

    /**
     * SEGURIDAD H-04: Sanitiza las recomendaciones de Gemini para prevenir XSS.
     * Utiliza Jackson para parsear y regenerar el JSON de forma segura.
     */
    private String sanitizeJsonRecommendations(String jsonInput) {
        if (jsonInput == null || jsonInput.isEmpty()) {
            return "[]";
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            // 1. Parsear el JSON crudo a una lista de mapas (objetos)
            List<Map<String, String>> recomendaciones = mapper.readValue(jsonInput,
                    new TypeReference<List<Map<String, String>>>() {
                    });

            // 2. Sanitizar cada valor individualmente
            for (Map<String, String> rec : recomendaciones) {
                // Sanitizar titulo y razon
                rec.put("titulo", sanitizeStringValue(rec.get("titulo")));
                rec.put("razon", sanitizeStringValue(rec.get("razon")));

                // Asegurar que no haya claves extrañas inyectadas
                rec.keySet().retainAll(java.util.List.of("titulo", "razon"));
            }

            // 3. Re-serializar a JSON seguro
            return mapper.writeValueAsString(recomendaciones);

        } catch (JsonProcessingException e) {
            logger.warn("Error parsing/sanitizing JSON from Gemini: {}", e.getMessage());
            // En caso de error de parsing (alucinación de la IA o ataque), devolver array
            // vacío o fallback
            // No devolver el input original
            return "[]";
        }
    }

    /**
     * Sanitiza un valor de string individual para prevenir XSS.
     * Solo escapa caracteres peligrosos para HTML, pero NO comillas
     * ya que eso rompería el JSON.
     */
    private String sanitizeStringValue(String value) {
        if (value == null)
            return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        // NO escapamos comillas aquí porque están siendo manejadas por el parser JSON
    }

    /**
     * @deprecated Use sanitizeJsonRecommendations for JSON content.
     *             Kept for reference - this method breaks JSON structure.
     */
    @SuppressWarnings("unused")
    private String sanitizeForXss(String input) {
        if (input == null) {
            return null;
        }
        // Escapar caracteres peligrosos para HTML
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
