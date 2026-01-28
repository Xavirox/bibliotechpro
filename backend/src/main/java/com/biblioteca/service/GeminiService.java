
package com.biblioteca.service;

import com.biblioteca.dto.RecomendacionDTO;
import com.biblioteca.model.Libro;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiService.class);
    private static final String DEFAULT_AI_URL = "http://ai-service:8000/api/recomendar";

    private final PrestamoRepository repositorioPrestamo;
    private final LibroRepository repositorioLibro;
    private final SocioRepository repositorioSocio;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:" + DEFAULT_AI_URL + "}")
    private String urlServicioIA;

    public GeminiService(PrestamoRepository repositorioPrestamo,
            LibroRepository repositorioLibro,
            SocioRepository repositorioSocio,
            ObjectMapper objectMapper) {
        this.repositorioPrestamo = repositorioPrestamo;
        this.repositorioLibro = repositorioLibro;
        this.repositorioSocio = repositorioSocio;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory fabricaSolicitudes = new SimpleClientHttpRequestFactory();
        fabricaSolicitudes.setConnectTimeout(2000); // 2 segundos
        fabricaSolicitudes.setReadTimeout(8000); // 8 segundos (La IA puede tardar)
        this.restTemplate = new RestTemplate(fabricaSolicitudes);
    }

    /**
     * Obtiene recomendaciones personalizadas para un usuario.
     * Si el servicio de IA falla, retorna recomendaciones basadas en reglas
     * locales.
     */
    public List<RecomendacionDTO> obtenerRecomendacionesPorUsuario(String usuario) {
        try {
            Socio socio = repositorioSocio.findByUsuario(usuario)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuario));
            return obtenerRecomendaciones(socio.getIdSocio());
        } catch (Exception e) {
            LOG.error("Error crítico obteniendo recomendaciones para {}. Usando fallback.", usuario, e);
            return obtenerRecomendacionesLocales(Collections.emptyList());
        }
    }

    public List<RecomendacionDTO> obtenerRecomendaciones(Long idSocio) {
        List<Prestamo> prestamos = repositorioPrestamo.findBySocioIdSocioWithDetails(idSocio);

        try {
            List<Map<String, String>> historial = prestamos.stream()
                    .map(p -> {
                        Libro l = p.getEjemplar().getLibro();
                        return Map.of(
                                "titulo", l.getTitulo() != null ? l.getTitulo() : "Desconocido",
                                "categoria", l.getCategoria() != null ? l.getCategoria() : "General",
                                "autor", l.getAutor() != null ? l.getAutor() : "Anónimo");
                    })
                    .distinct()
                    .collect(Collectors.toList());

            List<Libro> librosAleatorios = repositorioLibro.findRandomBooks(30);
            List<Map<String, String>> catalogo = librosAleatorios.stream()
                    .map(l -> Map.of(
                            "titulo", l.getTitulo() != null ? l.getTitulo() : "Desconocido",
                            "categoria", l.getCategoria() != null ? l.getCategoria() : "General",
                            "autor", l.getAutor() != null ? l.getAutor() : "Anónimo"))
                    .collect(Collectors.toList());

            HttpHeaders cabeceras = new HttpHeaders();
            cabeceras.setContentType(MediaType.APPLICATION_JSON);

            // Actualizado a claves en español para coincidir con el servicio Python
            Map<String, Object> cuerpoPeticion = Map.of(
                    "historial", historial,
                    "catalogo", catalogo,
                    "id_usuario", String.valueOf(idSocio));

            Objects.requireNonNull(urlServicioIA, "La URL del servicio de IA no está configurada");

            ResponseEntity<String> respuesta = restTemplate.postForEntity(
                    urlServicioIA,
                    new HttpEntity<>(cuerpoPeticion, cabeceras),
                    String.class);

            if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
                List<RecomendacionDTO> recomendacionesIA = procesarRespuestaIA(respuesta.getBody());
                if (recomendacionesIA != null && !recomendacionesIA.isEmpty()) {
                    return recomendacionesIA;
                }
            }

        } catch (Exception e) {
            LOG.warn("Servicio de IA no disponible ({}). Cambiando a algoritmo local.", e.getMessage());
        }

        return obtenerRecomendacionesLocales(prestamos);
    }

    private List<RecomendacionDTO> procesarRespuestaIA(String json) {
        try {
            JsonNode raiz = objectMapper.readTree(json);
            JsonNode nodoRecomendaciones = raiz.path("recomendaciones");

            List<RecomendacionDTO> resultado = new ArrayList<>();
            if (nodoRecomendaciones.isArray()) {
                for (JsonNode nodo : nodoRecomendaciones) {
                    resultado.add(new RecomendacionDTO(
                            nodo.path("titulo").asText("Sin título"),
                            nodo.path("motivo").asText("Basado en tus preferencias.")));
                }
            }
            return resultado;
        } catch (Exception e) {
            LOG.error("Error procesando respuesta JSON de la IA: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Algoritmo de respaldo (Safety Fallback) ejecutado localmente cuando la IA no
     * responde.
     */
    private List<RecomendacionDTO> obtenerRecomendacionesLocales(List<Prestamo> prestamos) {
        LOG.info("Generando recomendaciones locales (Fallback)");

        Set<String> categoriasFavoritas = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getCategoria())
                .collect(Collectors.toSet());

        Set<Long> idsLeidos = prestamos.stream()
                .map(p -> p.getEjemplar().getLibro().getIdLibro())
                .collect(Collectors.toSet());

        List<Libro> todosLosLibros = repositorioLibro.findAll();
        Collections.shuffle(todosLosLibros);

        return todosLosLibros.stream()
                .filter(l -> !idsLeidos.contains(l.getIdLibro()))
                .limit(3)
                .map(l -> {
                    String motivo = categoriasFavoritas.contains(l.getCategoria())
                            ? "Te gustó " + l.getCategoria() + ", creemos que disfrutarás este título."
                            : "Sugerencia destacada de nuestra colección.";
                    return new RecomendacionDTO(l.getTitulo(), motivo);
                })
                .collect(Collectors.toList());
    }
}
