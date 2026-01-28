package com.biblioteca.service;

import com.biblioteca.events.PrestamoDevueltoEvent;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.model.EstadoPrestamo;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.PrestamoRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private static final Logger LOG = LoggerFactory.getLogger(PrestamoService.class);
    private static final long DIAS_DURACION_PRESTAMO = 15L;

    private final PrestamoRepository repositorioPrestamo;
    private final SocioService servicioSocio;
    private final EjemplarService servicioEjemplar;
    private final ApplicationEventPublisher publicadorEventos;
    private final WebhookService servicioWebhook;
    private final EntityManager gestorEntidades;

    /**
     * Registra un nuevo préstamo tras validar las reglas de negocio.
     */
    @Transactional
    public Prestamo crearPrestamo(Long idSocio, Long idEjemplar) {
        Objects.requireNonNull(idSocio, "El ID del socio es requerido.");
        Objects.requireNonNull(idEjemplar, "El ID del ejemplar es requerido.");

        LOG.info("Iniciando préstamo - Socio ID: {}, Ejemplar ID: {}", idSocio, idEjemplar);

        Socio socio = buscarSocio(idSocio);
        Ejemplar ejemplar = buscarEjemplar(idEjemplar);

        validarRequisitosPrestamo(socio, ejemplar);

        Prestamo prestamo = construirPrestamoInicial(socio, ejemplar);

        return persistirYNotificarPrestamo(prestamo);
    }

    @Transactional
    public void devolverPrestamo(Long idPrestamo, String usuarioSolicitante, boolean esAutoridad) {
        Objects.requireNonNull(idPrestamo, "El ID del préstamo es requerido.");
        if (usuarioSolicitante == null || usuarioSolicitante.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario solicitante no puede ser nulo o vacío.");
        }

        LOG.info("Procesando devolución - Préstamo ID: {}", idPrestamo);

        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        validarPermisosDevolucion(prestamo, usuarioSolicitante, esAutoridad);

        procesarDevolucionInterna(prestamo);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> obtenerTodosLosPrestamos(String estado) {
        if (estado != null) {
            return repositorioPrestamo.findByEstadoWithDetails(EstadoPrestamo.valueOf(estado));
        }
        return repositorioPrestamo.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> obtenerPrestamosDeUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario no puede ser nulo o vacío.");
        }
        Socio socio = servicioSocio.buscarPorUsuario(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuario));
        return repositorioPrestamo.findBySocioIdSocioWithDetails(socio.getIdSocio());
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODOS PRIVADOS (Lógica de Negocio y Validación)
    // ------------------------------------------------------------------------------------------------

    private Prestamo construirPrestamoInicial(Socio socio, Ejemplar ejemplar) {
        return Prestamo.builder()
                .socio(socio)
                .ejemplar(ejemplar)
                .estado(EstadoPrestamo.ACTIVO)
                .fechaPrestamo(new Date())
                .fechaPrevistaDevolucion(Date.from(Instant.now().plus(DIAS_DURACION_PRESTAMO, ChronoUnit.DAYS)))
                .build();
    }

    private Prestamo persistirYNotificarPrestamo(Prestamo prestamo) {
        try {
            @SuppressWarnings("null")
            Prestamo prestamoPersistido = repositorioPrestamo.save(prestamo);
            Objects.requireNonNull(prestamoPersistido);

            // Usar servicio para lógica centralizada (SRP)
            servicioEjemplar.actualizarEstadoEjemplar(Objects.requireNonNull(prestamo.getEjemplar().getIdEjemplar()),
                    EstadoEjemplar.PRESTADO);

            // Asegurar que la persistencia se complete antes de eventos externos
            gestorEntidades.flush();
            gestorEntidades.refresh(prestamoPersistido);

            servicioWebhook.notificarNuevoPrestamo(
                    prestamo.getSocio().getNombre(),
                    prestamo.getEjemplar().getLibro().getTitulo());

            return prestamoPersistido;

        } catch (DataIntegrityViolationException e) {
            LOG.error("Error de integridad al crear préstamo: {}", e.getMessage());
            throw new IllegalStateException("Conflicto de datos al procesar el préstamo (posible duplicado).", e);
        }
    }

    private void procesarDevolucionInterna(Prestamo prestamo) {
        finalizarPrestamo(prestamo);

        // Usar servicio para lógica centralizada (SRP)
        servicioEjemplar.actualizarEstadoEjemplar(Objects.requireNonNull(prestamo.getEjemplar().getIdEjemplar()),
                EstadoEjemplar.DISPONIBLE);

        gestorEntidades.flush();
        gestorEntidades.refresh(prestamo);

        publicadorEventos.publishEvent(new PrestamoDevueltoEvent(this, prestamo));
    }

    private void validarRequisitosPrestamo(Socio socio, Ejemplar ejemplar) {
        if (!esEjemplarDisponible(ejemplar)) {
            throw new IllegalStateException("El ejemplar no está disponible (Estado: " + ejemplar.getEstado() + ")");
        }
        validarLimitePrestamos(socio);
        validarLibroDuplicado(socio, ejemplar);
    }

    private boolean esEjemplarDisponible(Ejemplar ejemplar) {
        return ejemplar.getEstado() == EstadoEjemplar.DISPONIBLE || ejemplar.getEstado() == EstadoEjemplar.BLOQUEADO;
    }

    private void validarLimitePrestamos(Socio socio) {
        long prestamosActivos = repositorioPrestamo.countBySocioIdSocioAndEstado(socio.getIdSocio(),
                EstadoPrestamo.ACTIVO);
        if (prestamosActivos >= socio.getMaxPrestamosActivos()) {
            throw new IllegalStateException(
                    String.format("Límite de préstamos alcanzado (%d).", socio.getMaxPrestamosActivos()));
        }
    }

    private void validarLibroDuplicado(Socio socio, Ejemplar ejemplar) {
        boolean tieneLibroDuplicado = repositorioPrestamo.existsBySocioIdSocioAndEjemplarLibroIdLibroAndEstado(
                socio.getIdSocio(), ejemplar.getLibro().getIdLibro(), EstadoPrestamo.ACTIVO);

        if (tieneLibroDuplicado) {
            throw new IllegalStateException(
                    String.format("El socio ya tiene prestado el libro '%s'.", ejemplar.getLibro().getTitulo()));
        }
    }

    private void validarPermisosDevolucion(Prestamo prestamo, String usuario, boolean esAutoridad) {
        if (esAutoridad)
            return;

        boolean esPropietario = prestamo.getSocio() != null &&
                prestamo.getSocio().getUsuario().equals(usuario);

        if (!esPropietario) {
            throw new SecurityException("No tiene permisos para devolver este préstamo.");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODOS PRIVADOS (Acceso a Datos y Helpers)
    // ------------------------------------------------------------------------------------------------

    private void finalizarPrestamo(Prestamo prestamo) {
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(new Date());
        repositorioPrestamo.save(prestamo);
    }

    private Socio buscarSocio(Long id) {
        Objects.requireNonNull(id, "El ID del socio es requerido.");
        return servicioSocio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + id));
    }

    private Ejemplar buscarEjemplar(Long id) {
        Objects.requireNonNull(id, "El ID del ejemplar es requerido.");
        Ejemplar ejemplar = servicioEjemplar.buscarEjemplarPorId(id);
        if (ejemplar == null) {
            throw new IllegalArgumentException("Ejemplar no encontrado con ID: " + id);
        }
        return ejemplar;
    }

    private Prestamo buscarPrestamoActivo(Long id) {
        Prestamo prestamo = repositorioPrestamo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado."));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new IllegalStateException("El préstamo ya ha sido devuelto.");
        }
        return prestamo;
    }
}
