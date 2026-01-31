package com.biblioteca.service;

import com.biblioteca.events.PrestamoDevueltoEvent;
import com.biblioteca.model.EstadoBloqueo;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.model.EstadoPrestamo;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.BloqueoRepository;
import com.biblioteca.repository.PrestamoRepository;

import jakarta.persistence.EntityManager;
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
public class PrestamoService {

    private static final Logger LOG = LoggerFactory.getLogger(PrestamoService.class);
    private static final long DIAS_DURACION_PRESTAMO = 15L;

    private final PrestamoRepository repositorioPrestamo;
    private final BloqueoRepository repositorioBloqueo;
    private final SocioService servicioSocio;
    private final EjemplarService servicioEjemplar;
    private final ApplicationEventPublisher publicadorEventos;
    private final WebhookService servicioWebhook;
    private final EntityManager gestorEntidades;

    public PrestamoService(
            PrestamoRepository repositorioPrestamo,
            BloqueoRepository repositorioBloqueo,
            SocioService servicioSocio,
            EjemplarService servicioEjemplar,
            ApplicationEventPublisher publicadorEventos,
            WebhookService servicioWebhook,
            EntityManager gestorEntidades) {
        this.repositorioPrestamo = repositorioPrestamo;
        this.repositorioBloqueo = repositorioBloqueo;
        this.servicioSocio = servicioSocio;
        this.servicioEjemplar = servicioEjemplar;
        this.publicadorEventos = publicadorEventos;
        this.servicioWebhook = servicioWebhook;
        this.gestorEntidades = gestorEntidades;
    }

    /**
     * Registra un nuevo préstamo para un socio.
     * <p>
     * Se aplican las siguientes validaciones estrictas:
     * 1. El ejemplar debe estar disponible.
     * 2. El socio no debe superar su límite de préstamos activos (por defecto 1).
     * 3. El socio no puede tener ya prestado un ejemplar del mismo libro (evitar
     * duplicados).
     *
     * @param idSocio    Identificador del socio.
     * @param idEjemplar Identificador del ejemplar.
     * @return El préstamo creado.
     * @throws IllegalStateException Si alguna regla de negocio impide el préstamo.
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

    /**
     * Procesa la devolución de un libro prestado.
     *
     * @param idPrestamo         ID del préstamo a finalizar.
     * @param usuarioSolicitante Usuario que intenta realizar la devolución.
     * @param esAutoridad        True si es Admin/Bibliotecario (puede devolver
     *                           libros de otros).
     *                           False si es el propio socio (solo puede devolver
     *                           sus propios libros).
     */
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

    /**
     * Obtiene todos los préstamos del sistema, opcionalmente filtrados por estado.
     *
     * @param estado Estado del préstamo (ACTIVO, DEVUELTO) o null para todos.
     * @return Lista de préstamos.
     */
    @Transactional(readOnly = true)
    public List<Prestamo> obtenerTodosLosPrestamos(String estado) {
        if (estado != null) {
            return repositorioPrestamo.findByEstadoWithDetails(EstadoPrestamo.valueOf(estado));
        }
        return repositorioPrestamo.findAllWithDetails();
    }

    /**
     * Obtiene el historial de préstamos de un usuario específico.
     *
     * @param usuario Nombre de usuario (username).
     * @return Lista de préstamos del usuario.
     */
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
        return new Prestamo(
                socio,
                ejemplar,
                EstadoPrestamo.ACTIVO,
                new Date(),
                Date.from(Instant.now().plus(DIAS_DURACION_PRESTAMO, ChronoUnit.DAYS)));
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

        long bloqueosActivos = repositorioBloqueo.countActiveBloqueosBySocio(socio.getIdSocio(),
                EstadoBloqueo.ACTIVO, new Date());

        if ((prestamosActivos + bloqueosActivos) >= socio.getMaxPrestamosActivos()) {
            throw new IllegalStateException(
                    String.format("Límite de lecturas activas alcanzado (%d). Préstamos: %d, Reservas: %d",
                            socio.getMaxPrestamosActivos(), prestamosActivos, bloqueosActivos));
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
