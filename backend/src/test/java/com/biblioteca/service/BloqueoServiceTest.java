package com.biblioteca.service;

import com.biblioteca.model.EstadoBloqueo;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.model.EstadoPrestamo;
import com.biblioteca.model.Bloqueo;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Libro;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.BloqueoRepository;
import com.biblioteca.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BloqueoService Unit Tests")
@SuppressWarnings("null")
class BloqueoServiceTest {

    @Mock
    private BloqueoRepository bloqueoRepository;

    @Mock
    private SocioService socioService; // Changed from Repository

    @Mock
    private EjemplarService ejemplarService; // Changed from Repository

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private WebhookService webhookService;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private BloqueoService bloqueoService;

    private Socio socio;
    private Ejemplar ejemplar;
    private Libro libro;

    @BeforeEach
    void setUp() {
        libro = new Libro();
        libro.setIdLibro(1L);
        libro.setTitulo("Test Book");
        libro.setAutor("Test Author");

        socio = new Socio();
        socio.setIdSocio(1L);
        socio.setUsuario("testuser");
        socio.setNombre("Test User");

        ejemplar = new Ejemplar();
        ejemplar.setIdEjemplar(1L);
        ejemplar.setLibro(libro);
        ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
    }

    @Test
    @DisplayName("Should create bloqueo successfully when ejemplar is available")
    void crearBloqueo_Success() {
        // Arrange
        when(socioService.buscarPorUsuario("testuser")).thenReturn(Optional.of(socio));
        when(ejemplarService.buscarEjemplarPorId(1L)).thenReturn(ejemplar);

        when(bloqueoRepository.save(any(Bloqueo.class))).thenAnswer(invocation -> {
            Bloqueo b = invocation.getArgument(0);
            b.setIdBloqueo(1L);
            return b;
        });

        // Act
        Bloqueo result = bloqueoService.crearBloqueo("testuser", 1L);

        // Assert
        assertNotNull(result);
        assertEquals(EstadoBloqueo.ACTIVO, result.getEstado());
        assertEquals(socio, result.getSocio());
        assertEquals(ejemplar, result.getEjemplar());
        assertNotNull(result.getFechaInicio());
        assertNotNull(result.getFechaFin());

        // Verify service update call
        verify(ejemplarService).actualizarEstadoEjemplar(1L, EstadoEjemplar.BLOQUEADO);
        verify(bloqueoRepository).save(any(Bloqueo.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user already has an active bloqueo")
    void crearBloqueo_AlreadyHasActive_ThrowsException() {
        // Arrange
        when(socioService.buscarPorUsuario("testuser")).thenReturn(Optional.of(socio));
        when(ejemplarService.buscarEjemplarPorId(1L)).thenReturn(ejemplar);
        // Mock que ya tiene 1 reserva activa
        when(bloqueoRepository.countActiveBloqueosBySocio(eq(1L), eq(EstadoBloqueo.ACTIVO), any(Date.class)))
                .thenReturn(1L);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bloqueoService.crearBloqueo("testuser", 1L));

        assertTrue(exception.getMessage().contains("ya tiene una reserva activa"));

        // Verificar que NO se intentó guardar nada
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when DataIntegrityViolationException occurs (race condition)")
    void crearBloqueo_RaceCondition_ThrowsIllegalStateException() {
        // Arrange
        when(socioService.buscarPorUsuario("testuser")).thenReturn(Optional.of(socio));
        when(ejemplarService.buscarEjemplarPorId(1L)).thenReturn(ejemplar);

        // Simular que el repositorio lanza la excepcion de integridad
        when(bloqueoRepository.save(any(Bloqueo.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Constraint violation"));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bloqueoService.crearBloqueo("testuser", 1L));

        assertTrue(exception.getMessage().contains("límite de reservas")
                || exception.getMessage().contains("ya no está disponible"));

        // Verificar que se intentó guardar
        verify(bloqueoRepository).save(any(Bloqueo.class));
    }

    @Test
    @DisplayName("Should throw exception when ejemplar is not available")
    void crearBloqueo_EjemplarNotAvailable() {
        // Arrange
        ejemplar.setEstado(EstadoEjemplar.PRESTADO);
        when(socioService.buscarPorUsuario("testuser")).thenReturn(Optional.of(socio));
        when(ejemplarService.buscarEjemplarPorId(1L)).thenReturn(ejemplar);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bloqueoService.crearBloqueo("testuser", 1L));

        assertTrue(exception.getMessage().contains("no está disponible"));
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void crearBloqueo_UserNotFound() {
        // Arrange
        when(socioService.buscarPorUsuario("unknownuser")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bloqueoService.crearBloqueo("unknownuser", 1L));

        assertTrue(exception.getMessage().contains("no encontrado"));
    }

    @Test
    @DisplayName("Should throw exception when ejemplar not found")
    void crearBloqueo_EjemplarNotFound() {
        // Arrange
        when(socioService.buscarPorUsuario("testuser")).thenReturn(Optional.of(socio));
        // Use unchecked exception for consistency with service's
        // IllegalArgumentException from orElseThrow
        when(ejemplarService.buscarEjemplarPorId(99L))
                .thenThrow(new IllegalArgumentException("Ejemplar no encontrado"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bloqueoService.crearBloqueo("testuser", 99L));

        assertTrue(exception.getMessage().contains("no encontrado"));
    }

    @Test
    @DisplayName("Should cancel bloqueo successfully")
    void cancelarBloqueo_Success() {
        // Arrange
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.ACTIVO);
        bloqueo.setSocio(socio);
        bloqueo.setEjemplar(ejemplar);

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Act
        bloqueoService.cancelarBloqueo(1L, "testuser");

        // Assert
        assertEquals(EstadoBloqueo.CANCELADO, bloqueo.getEstado());

        verify(ejemplarService).actualizarEstadoEjemplar(1L, EstadoEjemplar.DISPONIBLE);
        verify(bloqueoRepository).save(bloqueo);
    }

    @Test
    @DisplayName("Should throw exception when canceling other user's bloqueo")
    void cancelarBloqueo_NotOwner() {
        // Arrange
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.ACTIVO);
        bloqueo.setSocio(socio); // owned by "testuser"
        bloqueo.setEjemplar(ejemplar);

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> bloqueoService.cancelarBloqueo(1L, "otheruser"));
    }

    @Test
    @DisplayName("Should throw exception when canceling non-active bloqueo")
    void cancelarBloqueo_NotActive() {
        // Arrange
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.CANCELADO);
        bloqueo.setSocio(socio);
        bloqueo.setEjemplar(ejemplar);

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bloqueoService.cancelarBloqueo(1L, "testuser"));

        assertTrue(exception.getMessage().contains("no está activo"));
    }

    @Test
    @DisplayName("Should formalize bloqueo into prestamo when owner")
    void formalizarBloqueo_Success() {
        // Arrange
        ejemplar.setEstado(EstadoEjemplar.BLOQUEADO); // Ejemplar debe estar bloqueado
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.ACTIVO);
        bloqueo.setSocio(socio);
        bloqueo.setEjemplar(ejemplar);
        bloqueo.setFechaInicio(new Date());
        bloqueo.setFechaFin(new Date(System.currentTimeMillis() + 86400000));

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Mock validation checks
        socio.setMaxPrestamosActivos(5);
        when(prestamoRepository.countBySocioIdSocioAndEstado(eq(1L), eq(EstadoPrestamo.ACTIVO))).thenReturn(0L);
        when(bloqueoRepository.countActiveBloqueosBySocio(eq(1L), eq(EstadoBloqueo.ACTIVO), any(Date.class)))
                .thenReturn(1L);

        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> {
            Prestamo p = invocation.getArgument(0);
            p.setIdPrestamo(1L);
            return p;
        });

        // Act - Usuario propietario puede formalizar su propio bloqueo
        Prestamo result = bloqueoService.formalizarBloqueo(1L, "testuser", false);

        // Assert
        assertNotNull(result);
        assertEquals(EstadoPrestamo.ACTIVO, result.getEstado());
        assertEquals(socio, result.getSocio());
        assertEquals(ejemplar, result.getEjemplar());

        verify(ejemplarService).actualizarEstadoEjemplar(1L, EstadoEjemplar.PRESTADO);
        verify(prestamoRepository).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Should formalize any bloqueo when user is bibliotecario")
    void formalizarBloqueo_Bibliotecario_Success() {
        // Arrange
        ejemplar.setEstado(EstadoEjemplar.BLOQUEADO);
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.ACTIVO);
        bloqueo.setSocio(socio); // Pertenece a "testuser"
        bloqueo.setEjemplar(ejemplar);
        bloqueo.setFechaInicio(new Date());
        bloqueo.setFechaFin(new Date(System.currentTimeMillis() + 86400000));

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Mock validation checks
        socio.setMaxPrestamosActivos(5);
        when(prestamoRepository.countBySocioIdSocioAndEstado(eq(1L), eq(EstadoPrestamo.ACTIVO))).thenReturn(0L);
        when(bloqueoRepository.countActiveBloqueosBySocio(eq(1L), eq(EstadoBloqueo.ACTIVO), any(Date.class)))
                .thenReturn(1L);

        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> {
            Prestamo p = invocation.getArgument(0);
            p.setIdPrestamo(1L);
            return p;
        });

        // Act - Bibliotecario puede formalizar bloqueo de otro usuario
        Prestamo result = bloqueoService.formalizarBloqueo(1L, "bibliotecario", true);

        // Assert
        assertNotNull(result);
        verify(prestamoRepository).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Should throw SecurityException when formalizing other user's bloqueo")
    void formalizarBloqueo_NotOwner_ThrowsSecurityException() {
        // Arrange
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.ACTIVO);
        bloqueo.setSocio(socio); // Pertenece a "testuser"
        bloqueo.setEjemplar(ejemplar);

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Act & Assert - Usuario no-bibliotecario no puede formalizar bloqueo ajeno
        assertThrows(SecurityException.class,
                () -> bloqueoService.formalizarBloqueo(1L, "otheruser", false));
    }

    @Test
    @DisplayName("Should throw exception when formalizing non-active bloqueo")
    void formalizarBloqueo_NotActive() {
        // Arrange
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.EXPIRADO);
        bloqueo.setSocio(socio);

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        // Act & Assert - Con bibliotecario para evitar el chequeo de ownership
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bloqueoService.formalizarBloqueo(1L, "bibliotecario", true));

        assertTrue(exception.getMessage().contains("no está activo"));
    }

    @Test
    @DisplayName("Should throw exception when creating bloqueo with null username")
    void crearBloqueo_NullUser_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bloqueoService.crearBloqueo(null, 1L));
    }

    @Test
    @DisplayName("Should throw exception when creating bloqueo with empty username")
    void crearBloqueo_EmptyUser_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bloqueoService.crearBloqueo("", 1L));
    }

    @Test
    @DisplayName("Should use repository active blocks query")
    void obtenerBloqueosActivos_UsesRepository() {
        // Act
        bloqueoService.obtenerBloqueosActivos();

        // Assert
        verify(bloqueoRepository).findActiveBloqueosWithDetails(eq(EstadoBloqueo.ACTIVO), any(Date.class));
    }
}
