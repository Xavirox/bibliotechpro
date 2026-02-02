package com.biblioteca.service;

import com.biblioteca.config.LibraryPolicyProperties;
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
    private SocioService socioService;

    @Mock
    private EjemplarService ejemplarService;

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LibraryPolicyProperties libraryPolicy;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private BloqueoService bloqueoService;

    private Socio socio;
    private Ejemplar ejemplar;
    private Libro libro;

    @BeforeEach
    void setUp() {
        // Setup internal policy defaults
        lenient().when(libraryPolicy.getPrestamoDias()).thenReturn(15L);
        lenient().when(libraryPolicy.getReservaHoras()).thenReturn(24);

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
        verify(ejemplarService).actualizarEstadoEjemplar(1L, EstadoEjemplar.BLOQUEADO);
        verify(bloqueoRepository).save(any(Bloqueo.class));
    }

    @Test
    @DisplayName("Should formalize bloqueo into prestamo when owner")
    void formalizarBloqueo_Success() {
        // Arrange
        ejemplar.setEstado(EstadoEjemplar.BLOQUEADO);
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setIdBloqueo(1L);
        bloqueo.setEstado(EstadoBloqueo.ACTIVO);
        bloqueo.setSocio(socio);
        bloqueo.setEjemplar(ejemplar);
        bloqueo.setFechaInicio(new Date());
        bloqueo.setFechaFin(new Date(System.currentTimeMillis() + 86400000));

        when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

        socio.setMaxPrestamosActivos(5);
        when(prestamoRepository.countBySocioIdSocioAndEstado(eq(1L), eq(EstadoPrestamo.ACTIVO))).thenReturn(0L);
        when(bloqueoRepository.countActiveBloqueosBySocio(eq(1L), eq(EstadoBloqueo.ACTIVO), any(Date.class)))
                .thenReturn(1L);

        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> {
            Prestamo p = invocation.getArgument(0);
            p.setIdPrestamo(1L);
            return p;
        });

        // Act
        Prestamo result = bloqueoService.formalizarBloqueo(1L, "testuser", false);

        // Assert
        assertNotNull(result);
        assertEquals(EstadoPrestamo.ACTIVO, result.getEstado());
        verify(ejemplarService).actualizarEstadoEjemplar(1L, EstadoEjemplar.PRESTADO);
        verify(prestamoRepository).save(any(Prestamo.class));
    }
}
