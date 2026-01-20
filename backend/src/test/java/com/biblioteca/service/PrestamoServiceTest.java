package com.biblioteca.service;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Libro;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.EjemplarRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrestamoService Unit Tests")
@SuppressWarnings("null") // Suppress false positive null safety warnings from Mockito
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private SocioRepository socioRepository;

    @Mock
    private EjemplarRepository ejemplarRepository;

    @InjectMocks
    private PrestamoService prestamoService;

    private Socio socio;
    private Ejemplar ejemplar;
    private Libro libro;

    @BeforeEach
    void setUp() {
        // Setup test data
        libro = new Libro();
        libro.setIdLibro(1L);
        libro.setTitulo("Test Book");
        libro.setAutor("Test Author");
        libro.setIsbn("1234567890123");

        socio = new Socio();
        socio.setIdSocio(1L);
        socio.setUsuario("testuser");
        socio.setNombre("Test User");

        ejemplar = new Ejemplar();
        ejemplar.setIdEjemplar(1L);
        ejemplar.setLibro(libro);
        ejemplar.setEstado("DISPONIBLE");
    }

    @Test
    @DisplayName("Should create loan successfully when copy is available")
    void crearPrestamo_Success() {
        // Arrange
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(ejemplarRepository.findById(1L)).thenReturn(Optional.of(ejemplar));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> {
            Prestamo p = invocation.getArgument(0);
            p.setIdPrestamo(1L);
            return p;
        });

        // Act
        Prestamo result = prestamoService.crearPrestamo(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVO", result.getEstado());
        assertEquals(socio, result.getSocio());
        assertEquals(ejemplar, result.getEjemplar());
        assertEquals("PRESTADO", ejemplar.getEstado());

        verify(ejemplarRepository).save(ejemplar);
        verify(prestamoRepository).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Should throw exception when copy is not available")
    void crearPrestamo_CopyNotAvailable() {
        // Arrange
        ejemplar.setEstado("PRESTADO");
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(ejemplarRepository.findById(1L)).thenReturn(Optional.of(ejemplar));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> prestamoService.crearPrestamo(1L, 1L));

        assertTrue(exception.getMessage().contains("no está disponible"));
        verify(prestamoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when socio not found")
    void crearPrestamo_SocioNotFound() {
        // Arrange
        when(socioRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> prestamoService.crearPrestamo(99L, 1L));

        assertTrue(exception.getMessage().contains("Socio no encontrado"));
    }

    @Test
    @DisplayName("Should return loan successfully when owner returns it")
    void devolverPrestamo_Success_Owner() {
        // Arrange
        Prestamo prestamo = new Prestamo();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado("ACTIVO");
        prestamo.setSocio(socio); // Set owner
        prestamo.setEjemplar(ejemplar);
        ejemplar.setEstado("PRESTADO");

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        // Act - Owner returning their own loan
        prestamoService.devolverPrestamo(1L, "testuser", false);

        // Assert
        assertEquals("DEVUELTO", prestamo.getEstado());
        assertEquals("DISPONIBLE", ejemplar.getEstado());
        assertNotNull(prestamo.getFechaDevolucionReal());

        verify(prestamoRepository).save(prestamo);
        verify(ejemplarRepository).save(ejemplar);
    }

    @Test
    @DisplayName("Should return loan successfully when bibliotecario returns it")
    void devolverPrestamo_Success_Bibliotecario() {
        // Arrange
        Prestamo prestamo = new Prestamo();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado("ACTIVO");
        prestamo.setSocio(socio); // Owner is testuser
        prestamo.setEjemplar(ejemplar);
        ejemplar.setEstado("PRESTADO");

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        // Act - Bibliotecario can return any loan
        prestamoService.devolverPrestamo(1L, "bibliotecario1", true);

        // Assert
        assertEquals("DEVUELTO", prestamo.getEstado());
        assertEquals("DISPONIBLE", ejemplar.getEstado());
    }

    @Test
    @DisplayName("Should throw SecurityException when non-owner tries to return loan")
    void devolverPrestamo_Forbidden_NonOwner() {
        // Arrange
        Prestamo prestamo = new Prestamo();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado("ACTIVO");
        prestamo.setSocio(socio); // Owner is testuser
        prestamo.setEjemplar(ejemplar);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        // Act & Assert - Different user (not bibliotecario) tries to return
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> prestamoService.devolverPrestamo(1L, "otheruser", false));

        assertTrue(exception.getMessage().contains("no es tuyo"));
        verify(prestamoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when returning already returned loan")
    void devolverPrestamo_AlreadyReturned() {
        // Arrange
        Prestamo prestamo = new Prestamo();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado("DEVUELTO");
        prestamo.setSocio(socio);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> prestamoService.devolverPrestamo(1L, "testuser", false));

        assertTrue(exception.getMessage().contains("no está activo"));
    }
}
