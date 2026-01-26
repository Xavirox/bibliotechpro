package com.biblioteca.service;

import com.biblioteca.events.PrestamoDevueltoEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private EjemplarRepository ejemplarRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private WebhookService webhookService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private PrestamoService prestamoService;

    private Socio socio;
    private Ejemplar ejemplar;
    private Libro libro;
    private Prestamo prestamoActivo;

    @BeforeEach
    void setUp() {
        // Setup común para datos
        socio = new Socio();
        socio.setIdSocio(1L);
        socio.setUsuario("testuser");
        socio.setMaxPrestamosActivos(3);

        libro = new Libro();
        libro.setTitulo("Clean Code");

        ejemplar = new Ejemplar();
        ejemplar.setIdEjemplar(10L);
        ejemplar.setLibro(libro);
        ejemplar.setEstado("DISPONIBLE");

        prestamoActivo = new Prestamo();
        prestamoActivo.setIdPrestamo(55L);
        prestamoActivo.setSocio(socio);
        prestamoActivo.setEjemplar(ejemplar);
        prestamoActivo.setEstado("ACTIVO");
        prestamoActivo.setFechaPrestamo(new Date());
    }

    @Test
    @DisplayName("Debe crear un préstamo exitosamente (Happy Path)")
    void testCrearPrestamo_HappyPath() {
        // Arrange
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(ejemplarRepository.findById(10L)).thenReturn(Optional.of(ejemplar));
        // No verificamos límites en Java, delegamos a DB

        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> {
            Prestamo p = invocation.getArgument(0);
            p.setIdPrestamo(100L);
            return p;
        });

        // Act
        Prestamo resultado = prestamoService.crearPrestamo(1L, 10L);

        // Assert
        assertNotNull(resultado);
        assertEquals(100L, resultado.getIdPrestamo());
        assertEquals("ACTIVO", resultado.getEstado());
        assertEquals("PRESTADO", ejemplar.getEstado()); // Verificamos que se actualiza en memoria
        verify(prestamoRepository).save(any(Prestamo.class));
        verify(entityManager).refresh(any());
    }

    @Test
    @DisplayName("Falla si el ejemplar no está disponible")
    void testCrearPrestamo_NoDisponible() {
        // Arrange
        ejemplar.setEstado("PRESTADO");
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(ejemplarRepository.findById(10L)).thenReturn(Optional.of(ejemplar));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            prestamoService.crearPrestamo(1L, 10L);
        });
    }

    @Test
    @DisplayName("Captura error de triggers (DataIntegrityViolation)")
    void testCrearPrestamo_TriggerError() {
        // Arrange
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(ejemplarRepository.findById(10L)).thenReturn(Optional.of(ejemplar));

        // Simular error de Trigger
        DataIntegrityViolationException sqlException = new DataIntegrityViolationException("Error wrapper",
                new java.sql.SQLException("ORA-20002: Límite máximo de préstamos alcanzado"));
        when(prestamoRepository.save(any(Prestamo.class))).thenThrow(sqlException);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            prestamoService.crearPrestamo(1L, 10L);
        });

        assertTrue(exception.getMessage().contains("violado reglas de negocio"));
    }

    @Test
    @DisplayName("Debo devolver préstamo exitosamente y disparar evento")
    void testDevolverPrestamo_HappyPath() {
        // Arrange
        ejemplar.setEstado("PRESTADO");
        when(prestamoRepository.findByIdWithDetails(55L)).thenReturn(Optional.of(prestamoActivo));

        // Act
        prestamoService.devolverPrestamo(55L, "testuser", false);

        // Assert
        assertEquals("DEVUELTO", prestamoActivo.getEstado());
        assertNotNull(prestamoActivo.getFechaDevolucionReal());
        assertEquals("DISPONIBLE", ejemplar.getEstado()); // Ahora verificado

        verify(prestamoRepository).save(prestamoActivo);
        verify(ejemplarRepository).save(ejemplar);

        // Verificar que se publica el evento
        ArgumentCaptor<PrestamoDevueltoEvent> eventCaptor = ArgumentCaptor.forClass(PrestamoDevueltoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(55L, eventCaptor.getValue().getPrestamo().getIdPrestamo());
    }

    @Test
    @DisplayName("No puede devolver un préstamo que ya fue devuelto")
    void testDevolverPrestamo_YaDevuelto() {
        // Arrange
        prestamoActivo.setEstado("DEVUELTO");
        when(prestamoRepository.findByIdWithDetails(55L)).thenReturn(Optional.of(prestamoActivo));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            prestamoService.devolverPrestamo(55L, "testuser", false);
        });

        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Usuario no propietario no puede devolver (Security)")
    void testDevolverPrestamo_UsuarioAjeno() {
        // Arrange
        when(prestamoRepository.findByIdWithDetails(55L)).thenReturn(Optional.of(prestamoActivo));

        // Act & Assert
        // "hacker" intenta devolver el libro de "testuser"
        assertThrows(SecurityException.class, () -> {
            prestamoService.devolverPrestamo(55L, "hacker", false);
        });
    }
}
