
package com.biblioteca.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.biblioteca.dto.RecomendacionDTO;
import com.biblioteca.model.Libro;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String DEFAULT_AI_URL = "http://ai-service:8000/api/recommend";

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final SocioRepository socioRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:" + DEFAULT_AI_URL + "}")
    private String aiServiceUrl;

    public GeminiService(PrestamoRepository prestamoRepository,
            LibroRepository libroRepository,
            SocioRepository socioRepository,
            ObjectMapper objectMapper) {
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
        this.socioRepository = socioRepository;
        this.objectMapper = objectMapper;

        // Configuración de timeouts directamente en el RestTemplate para evitar leaks
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // 2s conexión
        factory.setReadTimeout(8000); // 8s lectura (Gemini puede tardar)
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Obtiene recomendaciones para un usuario por su nombre de usuario.
     * Garantiza que NUNCA lanza excepción hacia el controlador.
     */
    public List<RecomendacionDTO> getRecomendacionesForUser(String username) {
        try {
            Socio socio = socioRepository.findByUsuario(username)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
            return getRecomendaciones(socio.getIdSocio());
        } catch (Exception e) {
            logger.error("Error crítico obteniendo recomendaciones para {}: {}", username, e.getMessage());
            return getFallbackRecommendations(Collections.emptyList());
        }
    }

    public List<RecomendacionDTO> getRecomendaciones(Long idSocio) {
        List<Prestamo> prestamos = prestamoRepository.findBySocioIdSocioWithDetails(idSocio);

        try {
            // Preparar datos para la IA
            List<Map<String, String>> history = prestamos.stream()
                    .map(p -> {
                        Libro l = p.getEjemplar().getLibro();
                        return Map.of(
                                "titulo", l.getTitulo() != null ? l.getTitulo() : "Desconocido",
                                "categoria", l.getCategoria() != null ? l.getCategoria() : "General",
                                "autor", l.getAutor() != null ? l.getAutor() : "Anónimo");
                    })
                    .distinct()
                    .collect(Collectors.toList());

            List<Libro> randomLibros = libroRepository.findRandomBooks(30);
            List<Map<String, String>> catalog = randomLibros.stream()
                    .map(l -> Map.of(
                            "titulo", l.getTitulo() != null ? l.getTitulo() : "Desconocido",
                            "categoria", l.getCategoria() != null ? l.getCategoria() : "General",
                            "autor", l.getAutor() != null ? l.getAutor() : "Anónimo"))
                    .collect(Collectors.toList());

            // Llamada al microservicio de IA
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "history", history,
                    "catalog", catalog,
                    "user_id", String.valueOf(idSocio));

            java.util.Objects.requireNonNull(aiServiceUrl, "AI Service URL no configurada");
            ResponseEntity<String> response = restTemplate.postForEntity(aiServiceUrl, new HttpEntity<>(body, headers),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<RecomendacionDTO> aiRecs = parseAiResponse(response.getBody());
                if (aiRecs != null && !aiRecs.isEmpty()) {
                    return aiRecs;
                }
            }

        } catch (Exception e) {
            logger.warn("IA no disponible o error en comunicación ({}). Usando fallback algorítmico.", e.getMessage());
        }

        return getFallbackRecommendations(prestamos);
    }

    /**
     * Parsea la respuesta del microservicio de IA asegurando el formato esperado.
     */
    private List<RecomendacionDTO> parseAiResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode recs = root.path("recomendaciones");

            List<RecomendacionDTO> result = new ArrayList<>();
            if (recs.isArray()) {
                for (JsonNode node : recs) {
                    result.add(new RecomendacionDTO(
                            node.path("titulo").asText("Sin título"),
                            node.path("motivo").asText("Basado en tus gustos.")));
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Error parseando respuesta de IA: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fallback algorítmico LOCAL. Se activa si:
     * 1. El microservicio de IA está caído.
     * 2. No hay API Key configurada.
     * 3. Se ha superado la cuota de Gemini.
     */
    private List<RecomendacionDTO> getFallbackRecommendations(List<Prestamo> prestamos) {
        logger.info("Generando recomendaciones locales (Safety Fallback)");

        Set<String> catFavoritas = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getCategoria())
                .collect(Collectors.toSet());

        Set<Long> leidosIds = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getIdLibro())
                .collect(Collectors.toSet());

        List<Libro> todos = libroRepository.findAll();
        Collections.shuffle(todos);

        return todos.stream()
                .filter(l -> !leidosIds.contains(l.getIdLibro()))
                .limit(3)
                .map(l -> {
                    String razon = catFavoritas.contains(l.getCategoria())
                            ? "Como te gusta " + l.getCategoria() + ", creemos que este libro te encantará."
                            : "Una sugerencia especial de nuestra biblioteca para ti.";
                    return new RecomendacionDTO(l.getTitulo(), razon);
                })
                .collect(Collectors.toList());
    }
}
