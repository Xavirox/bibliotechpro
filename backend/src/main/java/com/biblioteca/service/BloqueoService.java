package com.biblioteca.service;

import com.biblioteca.config.LibraryPolicyProperties;
import com.biblioteca.model.EstadoBloqueo;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.model.EstadoPrestamo;
import com.biblioteca.model.Bloqueo;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.BloqueoRepository;
import com.biblioteca.repository.PrestamoRepository;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BloqueoService {

    private static final Logger LOG = LoggerFactory.getLogger(BloqueoService.class);

    private final BloqueoRepository repositorioBloqueo;
    private final SocioService servicioSocio;
    private final EjemplarService servicioEjemplar;
    private final PrestamoRepository repositorioPrestamo;
    private final NotificationService servicioNotificaciones;
    private final EntityManager gestorEntidades;
    private final LibraryPolicyProperties libraryPolicy;

    public BloqueoService(
            BloqueoRepository repositorioBloqueo,
            SocioService servicioSocio,
            EjemplarService servicioEjemplar,
            PrestamoRepository repositorioPrestamo,
            NotificationService servicioNotificaciones,
            EntityManager gestorEntidades,
            LibraryPolicyProperties libraryPolicy) {
        this.repositorioBloqueo = repositorioBloqueo;
        this.servicioSocio = servicioSocio;
        this.servicioEjemplar = servicioEjemplar;
        this.repositorioPrestamo = repositorioPrestamo;
        this.servicioNotificaciones = servicioNotificaciones;
        this.gestorEntidades = gestorEntidades;
        this.libraryPolicy = libraryPolicy;
    }

    /**
     * Crea una nueva reserva (bloqueo) para un usuario sobre un ejemplar.
     * <p>
     * Reglas de negocio aplicadas:
     * 1. El ejemplar debe estar DISPONIBLE.
     * 2. El usuario no puede tener bloquear más de 1 libro simultáneamente.
     * 3. La suma de préstamos activos + reservas no puede superar el límite del
     * usuario.
     *
     * @param usuario    El nombre de usuario (username) del socio.
     * @param idEjemplar El ID del ejemplar a reservar.
     * @return La entidad Bloqueo creada y persistida.
     * @throws IllegalStateException Si no cumple las reglas de negocio.
     */
    @Transactional
    public Bloqueo crearBloqueo(String usuario, Long idEjemplar) {
        validarEntrada(usuario, "El usuario no puede ser nulo o vacío.");
        Objects.requireNonNull(idEjemplar, "El ID del ejemplar es requerido.");

        LOG.info("Creando bloqueo - Usuario: {}, Ejemplar ID: {}", usuario, idEjemplar);

        Socio socio = buscarSocio(usuario);
        Ejemplar ejemplar = buscarEjemplar(idEjemplar);

        validarRequisitosBloqueo(socio, ejemplar);

        Bloqueo bloqueo = construirBloqueoInicial(socio, ejemplar);

        return persistirYNotificarBloqueo(bloqueo, socio);
    }

    /**
     * Cancela una reserva activa, liberando el ejemplar para otros usuarios.
     *
     * @param idBloqueo ID de la reserva a cancelar.
     * @param usuario   Usuario que solicita la cancelación (para verificación de
     *                  seguridad).
     */
    @Transactional
    public void cancelarBloqueo(Long idBloqueo, String usuario) {
        Objects.requireNonNull(idBloqueo, "El ID del bloqueo es requerido.");
        validarEntrada(usuario, "El usuario no puede ser nulo o vacío.");

        LOG.info("Cancelando bloqueo - ID: {}", idBloqueo);

        Bloqueo bloqueo = buscarBloqueo(idBloqueo);
        validarPropiedad(bloqueo, usuario, false);
        validarEstadoActivo(bloqueo);

        ejecutarCancelacion(bloqueo);
    }

    /**
     * Formaliza una reserva, convirtiéndola en un préstamo activo.
     * <p>
     * Este proceso:
     * 1. Verifica que el usuario no haya excedido su límite de préstamos desde que
     * hizo la reserva.
     * 2. Crea un nuevo Préstamo.
     * 3. Marca la Reserva como CONVERTIDA y el Ejemplar como PRESTADO.
     *
     * @param idBloqueo       ID de la reserva.
     * @param usuario         Usuario que realiza la acción.
     * @param esBibliotecario True si es un bibliotecario (salta chequeo de
     *                        propiedad).
     * @return El nuevo Préstamo creado.
     */
    @Transactional
    public Prestamo formalizarBloqueo(Long idBloqueo, String usuario, boolean esBibliotecario) {
        Objects.requireNonNull(idBloqueo, "El ID del bloqueo es requerido.");
        validarEntrada(usuario, "El usuario no puede ser nulo o vacío.");

        LOG.info("Formalizando bloqueo - ID: {}", idBloqueo);

        Bloqueo bloqueo = buscarBloqueo(idBloqueo);
        validarPropiedad(bloqueo, usuario, esBibliotecario);
        validarEstadoActivo(bloqueo);

        // Anti-Gravity Update: Ensure strict limit enforcement even during conversion
        validarLimiteParaFormalizacion(bloqueo.getSocio());

        return convertirBloqueoAPrestamo(bloqueo);
    }

    @Transactional
    public void limpiarBloqueosExpirados() {
        LOG.info("Ejecutando limpieza manual de bloqueos expirados");
        repositorioBloqueo.ejecutarLimpiezaBloqueos();
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> obtenerBloqueosDeUsuario(String usuario) {
        validarEntrada(usuario, "El usuario no puede ser nulo o vacío.");
        Socio socio = buscarSocio(usuario);
        return repositorioBloqueo.findActiveBloqueosBySocioWithDetails(socio.getIdSocio(), EstadoBloqueo.ACTIVO,
                new Date());
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> obtenerBloqueosActivos() {
        return repositorioBloqueo.findActiveBloqueosWithDetails(EstadoBloqueo.ACTIVO, new Date());
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODOS PRIVADOS (Lógica de Negocio y Validación)
    // ------------------------------------------------------------------------------------------------

    private void validarRequisitosBloqueo(Socio socio, Ejemplar ejemplar) {
        if (ejemplar.getEstado() != EstadoEjemplar.DISPONIBLE) {
            throw new IllegalStateException("El ejemplar no está disponible (Estado: " + ejemplar.getEstado() + ")");
        }

        Long reservasActivasObj = repositorioBloqueo.countActiveBloqueosBySocio(socio.getIdSocio(),
                EstadoBloqueo.ACTIVO,
                new Date());
        long reservasActivas = (reservasActivasObj != null) ? reservasActivasObj : 0L;

        Long prestamosActivosObj = repositorioPrestamo.countBySocioIdSocioAndEstado(socio.getIdSocio(),
                EstadoPrestamo.ACTIVO);
        long prestamosActivos = (prestamosActivosObj != null) ? prestamosActivosObj : 0L;

        // Anti-Gravity Fix: Prevent NPE if maxPrestamosActivos is null in legacy users
        Integer maxPrestamos = socio.getMaxPrestamosActivos();
        if (maxPrestamos == null) {
            LOG.warn("Usuario {} tiene maxPrestamosActivos NULL. Usando defecto 2.", socio.getUsuario());
            maxPrestamos = 2; // Default safe limit
        }

        LOG.info("Validando reserva: User={}, Max={}, Prestamos={}, Reservas={}",
                socio.getUsuario(), maxPrestamos, prestamosActivos, reservasActivas);

        if ((reservasActivas + prestamosActivos) >= maxPrestamos) {
            throw new IllegalStateException(
                    String.format("Límite de lecturas activas alcanzado (%d). Préstamos: %d, Reservas: %d",
                            maxPrestamos, prestamosActivos, reservasActivas));
        }
    }

    private void validarEstadoActivo(Bloqueo bloqueo) {
        if (bloqueo.getEstado() != EstadoBloqueo.ACTIVO) {
            throw new IllegalStateException("El bloqueo no está activo.");
        }
    }

    private Bloqueo construirBloqueoInicial(Socio socio, Ejemplar ejemplar) {
        return new Bloqueo(
                socio,
                ejemplar,
                new Date(),
                Date.from(Instant.now().plus(libraryPolicy.getReservaHoras(), ChronoUnit.HOURS)),
                EstadoBloqueo.ACTIVO);
    }

    private Bloqueo persistirYNotificarBloqueo(Bloqueo bloqueo, Socio socio) {
        try {
            @SuppressWarnings("null")
            Bloqueo bloqueoPersistido = repositorioBloqueo.save(bloqueo);
            Objects.requireNonNull(bloqueoPersistido);

            servicioEjemplar.actualizarEstadoEjemplar(Objects.requireNonNull(bloqueo.getEjemplar().getIdEjemplar()),
                    EstadoEjemplar.BLOQUEADO);

            gestorEntidades.flush();
            gestorEntidades.refresh(bloqueoPersistido);

            try {
                servicioNotificaciones.notificarNuevaReserva(socio.getUsuario(),
                        bloqueo.getEjemplar().getLibro().getTitulo());
            } catch (Exception e) {
                LOG.error("Fallo al enviar notificación de webhook, pero el bloqueo continúa: {}", e.getMessage());
            }
            return bloqueoPersistido;

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            LOG.error("Error de integridad al crear bloqueo: {}", e.getMessage());
            throw new IllegalStateException(
                    "El ejemplar ya no está disponible o se ha alcanzado el límite de reservas.", e);
        }
    }

    private void ejecutarCancelacion(Bloqueo bloqueo) {
        bloqueo.setEstado(EstadoBloqueo.CANCELADO);

        servicioEjemplar.actualizarEstadoEjemplar(Objects.requireNonNull(bloqueo.getEjemplar().getIdEjemplar()),
                EstadoEjemplar.DISPONIBLE);

        repositorioBloqueo.save(bloqueo);
        gestorEntidades.flush();
    }

    private Prestamo convertirBloqueoAPrestamo(Bloqueo bloqueo) {
        Prestamo prestamo = new Prestamo();
        prestamo.setSocio(bloqueo.getSocio());
        prestamo.setEjemplar(bloqueo.getEjemplar());
        prestamo.setFechaPrestamo(new Date());
        prestamo.setFechaPrevistaDevolucion(
                Date.from(Instant.now().plus(libraryPolicy.getPrestamoDias(), ChronoUnit.DAYS)));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setBloqueo(bloqueo);

        bloqueo.setEstado(EstadoBloqueo.CONVERTIDO);
        servicioEjemplar.actualizarEstadoEjemplar(Objects.requireNonNull(bloqueo.getEjemplar().getIdEjemplar()),
                EstadoEjemplar.PRESTADO);

        Prestamo prestamoPersistido = repositorioPrestamo.save(prestamo);
        repositorioBloqueo.save(bloqueo);

        gestorEntidades.flush();
        gestorEntidades.refresh(prestamoPersistido);

        return prestamoPersistido;
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODOS PRIVADOS (Acceso a Datos y Helpers)
    // ------------------------------------------------------------------------------------------------

    private Socio buscarSocio(String usuario) {
        return servicioSocio.buscarPorUsuario(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuario));
    }

    private Ejemplar buscarEjemplar(Long id) {
        Objects.requireNonNull(id, "El ID del ejemplar es requerido.");
        Ejemplar ejemplar = servicioEjemplar.buscarEjemplarPorId(id);
        if (ejemplar == null) {
            throw new IllegalArgumentException("Ejemplar no encontrado con ID: " + id);
        }
        return ejemplar;
    }

    private Bloqueo buscarBloqueo(Long id) {
        Objects.requireNonNull(id, "El ID del bloqueo es requerido.");
        return repositorioBloqueo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueo no encontrado con ID: " + id));
    }

    private void validarPropiedad(Bloqueo bloqueo, String usuario, boolean esBibliotecario) {
        if (!esBibliotecario) {
            if (bloqueo.getSocio() == null) {
                throw new IllegalStateException("Datos corruptos: El bloqueo no tiene socio asociado.");
            }
            if (!bloqueo.getSocio().getUsuario().equals(usuario)) {
                throw new SecurityException("No tienes permiso sobre este bloqueo.");
            }
        }
    }

    private void validarEntrada(String valor, String mensajeError) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    private void validarLimiteParaFormalizacion(Socio socio) {
        long prestamosActivos = repositorioPrestamo.countBySocioIdSocioAndEstado(socio.getIdSocio(),
                EstadoPrestamo.ACTIVO);
        long bloqueosActivos = repositorioBloqueo.countActiveBloqueosBySocio(socio.getIdSocio(),
                EstadoBloqueo.ACTIVO, new Date());

        // Al formalizar, reduciremos 1 bloqueo y sumaremos 1 préstamo, así que el total
        // (P+B) se mantiene igual.
        // Sin embargo, si el usuario YA excede el límite (ej. se bajó el límite
        // global), no debería poder formalizar.
        // Opcional: Permitir la formalización si (P + B) <= Max, asumiendo que el
        // bloqueo actual ya cuenta.
        // Pero si (P + B) > Max, entonces bloqueamos.

        if ((prestamosActivos + bloqueosActivos) > socio.getMaxPrestamosActivos()) {
            throw new IllegalStateException(
                    String.format(
                            "No se puede formalizar el préstamo: Límite de lecturas activas excedido (%d). Actuales: %d",
                            socio.getMaxPrestamosActivos(), (prestamosActivos + bloqueosActivos)));
        }
    }
}
