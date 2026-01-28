package com.biblioteca.integration;

import com.biblioteca.model.Libro;
import com.biblioteca.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para LibroController.
 * Usa H2 in-memory database via @ActiveProfiles("test").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("LibroController Integration Tests")
class LibroControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private com.biblioteca.repository.EjemplarRepository ejemplarRepository;

    @Autowired
    private com.biblioteca.repository.PrestamoRepository prestamoRepository;

    @Autowired
    private com.biblioteca.repository.BloqueoRepository bloqueoRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/libros";

        // Clear and seed test data in correct order to avoid FK violations
        // ORDER MATTERS: Delete parents last
        prestamoRepository.deleteAllInBatch();
        bloqueoRepository.deleteAllInBatch();
        ejemplarRepository.deleteAllInBatch();
        libroRepository.deleteAllInBatch();

        Libro libro1 = new Libro();
        libro1.setTitulo("El Quijote");
        libro1.setAutor("Miguel de Cervantes");
        libro1.setIsbn("9788437604947");
        libro1.setCategoria("Clásicos");
        libroRepository.save(libro1);

        Libro libro2 = new Libro();
        libro2.setTitulo("Cien años de soledad");
        libro2.setAutor("Gabriel García Márquez");
        libro2.setIsbn("9788437604954");
        libro2.setCategoria("Novela");
        libroRepository.save(libro2);

        Libro libro3 = new Libro();
        libro3.setTitulo("Don Quijote de la Mancha");
        libro3.setAutor("Miguel de Cervantes");
        libro3.setIsbn("9788437604961");
        libro3.setCategoria("Clásicos");
        libroRepository.save(libro3);
    }

    @Test
    @DisplayName("Should get all books without authentication")
    void getAllLibros_Success() {
        ResponseEntity<List<com.biblioteca.dto.LibroDTO>> response = restTemplate.exchange(
                baseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<com.biblioteca.dto.LibroDTO>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<com.biblioteca.dto.LibroDTO> body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.size());
    }

    @Test
    @DisplayName("Should filter books by category")
    void getAllLibros_FilterByCategory() {
        ResponseEntity<List<com.biblioteca.dto.LibroDTO>> response = restTemplate.exchange(
                baseUrl + "?categoria=Clásicos", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<com.biblioteca.dto.LibroDTO>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    @Test
    @DisplayName("Should get paginated books")
    void getLibrosPaginated_Success() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/paginated?pagina=0&tamanio=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = Objects.requireNonNull(response.getBody());

        assertEquals(0, body.get("page"));
        assertEquals(2, body.get("size"));
        assertEquals(3, ((Number) Objects.requireNonNull(body.get("totalElements"))).intValue());
        assertEquals(2, ((Number) Objects.requireNonNull(body.get("totalPages"))).intValue());
        assertTrue((Boolean) Objects.requireNonNull(body.get("first")));
        assertFalse((Boolean) Objects.requireNonNull(body.get("last")));

        List<?> content = (List<?>) body.get("content");
        assertNotNull(content);
        assertEquals(2, content.size());
    }

    @Test
    @DisplayName("Should search books by title or author")
    void getLibrosPaginated_WithSearch() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/paginated?busqueda=Cervantes",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = Objects.requireNonNull(response.getBody());

        List<?> content = (List<?>) body.get("content");
        assertNotNull(content);
        assertEquals(2, content.size()); // Both books by Cervantes
    }

    @Test
    @DisplayName("Should get book by ID")
    void getLibroById_Success() {
        // Get first book's ID
        Libro libro = libroRepository.findAll().get(0);

        ResponseEntity<com.biblioteca.dto.LibroDTO> response = restTemplate.getForEntity(
                baseUrl + "/" + libro.getIdLibro(), com.biblioteca.dto.LibroDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        com.biblioteca.dto.LibroDTO responseBody = Objects.requireNonNull(response.getBody());
        assertEquals(libro.getTitulo(), responseBody.titulo());
    }

    @Test
    @DisplayName("Should return 404 for non-existent book")
    void getLibroById_NotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/999999", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Health endpoint should be accessible")
    void healthEndpoint_Success() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/health",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = Objects.requireNonNull(response.getBody());
        assertEquals("UP", body.get("status"));
    }
}
