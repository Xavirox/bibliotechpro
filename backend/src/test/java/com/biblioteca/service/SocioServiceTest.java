package com.biblioteca.service;

import com.biblioteca.dto.SocioDTO;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocioService Unit Tests")
@SuppressWarnings("null")
class SocioServiceTest {

    @Mock
    private SocioRepository repositorioSocio;

    @InjectMocks
    private SocioService socioService;

    @Test
    @DisplayName("Should return all socios mapped to DTOs")
    void obtenerTodosLosSociosDTO_ReturnsList() {
        // Arrange
        Socio socio1 = new Socio();
        socio1.setIdSocio(1L);
        socio1.setUsuario("user1");
        socio1.setNombre("User One");
        socio1.setEmail("user1@example.com");
        socio1.setRol("SOCIO");
        socio1.setMaxPrestamosActivos(3);

        Socio socio2 = new Socio();
        socio2.setIdSocio(2L);
        socio2.setUsuario("user2");
        socio2.setNombre("User Two");

        when(repositorioSocio.findAll()).thenReturn(Arrays.asList(socio1, socio2));

        // Act
        List<SocioDTO> result = socioService.obtenerTodosLosSociosDTO();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).usuario());
        assertEquals("user2", result.get(1).usuario());
    }

    @Test
    @DisplayName("Should penalize socio successfully")
    void penalizarSocio_Success() {
        // Arrange
        Long socioId = 1L;
        int dias = 5;
        Socio socio = new Socio();
        socio.setIdSocio(socioId);
        socio.setUsuario("testuser");

        when(repositorioSocio.findById(socioId)).thenReturn(Optional.of(socio));
        when(repositorioSocio.save(any(Socio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        socioService.penalizarSocio(socioId, dias);

        // Assert
        assertNotNull(socio.getPenalizacionHasta());
        // Simple check that date is approximately 5 days from now
        Instant expected = Instant.now().plus(dias, ChronoUnit.DAYS);
        long diff = Math.abs(socio.getPenalizacionHasta().toInstant().toEpochMilli() - expected.toEpochMilli());
        assertTrue(diff < 5000, "Date should be close to expected (delta 5s)");

        verify(repositorioSocio).save(socio);
    }

    @Test
    @DisplayName("Should throw exception when penalizing non-existent socio")
    void penalizarSocio_NotFound() {
        // Arrange
        when(repositorioSocio.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> socioService.penalizarSocio(99L, 5));

        assertTrue(ex.getMessage().contains("no encontrado"));
        verify(repositorioSocio, never()).save(any(Socio.class));
    }

    @Test
    @DisplayName("Should update loan limit successfully")
    void actualizarLimitePrestamos_Success() {
        // Arrange
        Long socioId = 1L;
        Socio socio = new Socio();
        socio.setIdSocio(socioId);
        socio.setMaxPrestamosActivos(1);

        when(repositorioSocio.findById(socioId)).thenReturn(Optional.of(socio));
        when(repositorioSocio.save(any(Socio.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Socio result = socioService.actualizarLimitePrestamos(socioId, 10);

        // Assert
        assertEquals(10, result.getMaxPrestamosActivos());
        verify(repositorioSocio).save(socio);
    }

    @Test
    @DisplayName("Should throw exception for negative loan limit")
    void actualizarLimitePrestamos_NegativeLimit() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> socioService.actualizarLimitePrestamos(1L, -5));

        verify(repositorioSocio, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should find socio by username when exists")
    void buscarPorUsuario_Success() {
        // Arrange
        String username = "testuser";
        Socio socio = new Socio();
        socio.setUsuario(username);

        when(repositorioSocio.findByUsuario(username)).thenReturn(Optional.of(socio));

        // Act
        Optional<Socio> result = socioService.buscarPorUsuario(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsuario());
    }

    @Test
    @DisplayName("Should return empty optional when socio not found by username")
    void buscarPorUsuario_NotFound() {
        // Arrange
        String username = "unknown";
        when(repositorioSocio.findByUsuario(username)).thenReturn(Optional.empty());

        // Act
        Optional<Socio> result = socioService.buscarPorUsuario(username);

        // Assert
        assertTrue(result.isEmpty());
    }
}
