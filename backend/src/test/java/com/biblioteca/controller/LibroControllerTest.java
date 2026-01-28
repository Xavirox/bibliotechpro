package com.biblioteca.controller;

import com.biblioteca.service.LibroService;
import com.biblioteca.dto.LibroDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LibroController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing
@DisplayName("LibroController Unit Tests")
@SuppressWarnings("null") // Suppress false positive null safety warnings from test framework libraries
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    // Security beans required during context initialization
    @MockitoBean
    private com.biblioteca.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.biblioteca.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private com.biblioteca.security.CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private com.biblioteca.config.JwtProperties jwtProperties;

    private List<LibroDTO> testLibros;

    @BeforeEach
    void setUp() {
        LibroDTO libro1 = new LibroDTO(
                1L, "1234567890123", "Don Quijote", "Cervantes", "Novela", 1605, 5, true);

        LibroDTO libro2 = new LibroDTO(
                2L, "9876543210123", "1984", "George Orwell", "Ciencia Ficción", 1949, 0, false);

        testLibros = Arrays.asList(libro1, libro2);
    }

    @Test
    @DisplayName("Should return all books")
    void getAllLibros_ReturnsAllBooks() throws Exception {
        when(libroService.obtenerTodosLosLibros(null, null, null, null)).thenReturn(testLibros);

        mockMvc.perform(get("/api/libros")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].titulo", is("Don Quijote")))
                .andExpect(jsonPath("$[1].titulo", is("1984")));
    }

    @Test
    @DisplayName("Should filter books by category")
    void getAllLibros_FilterByCategory() throws Exception {
        when(libroService.obtenerTodosLosLibros("Novela", null, null, null))
                .thenReturn(List.of(testLibros.get(0)));

        mockMvc.perform(get("/api/libros")
                .param("categoria", "Novela")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoria", is("Novela")));
    }

    @Test
    @DisplayName("Should return book by ID")
    void getLibroById_ReturnsBook() throws Exception {
        when(libroService.obtenerLibroPorId(1L)).thenReturn(Optional.of(testLibros.get(0)));

        mockMvc.perform(get("/api/libros/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Don Quijote")))
                .andExpect(jsonPath("$.autor", is("Cervantes")));
    }

    @Test
    @DisplayName("Should return 404 when book not found")
    void getLibroById_NotFound() throws Exception {
        when(libroService.obtenerLibroPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/libros/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
