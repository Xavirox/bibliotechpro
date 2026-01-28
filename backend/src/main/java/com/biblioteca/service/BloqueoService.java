package com.biblioteca.service;

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
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class BloqueoService {

    private static final Logger LOG = LoggerFactory.getLogger(BloqueoService.class);

    private final BloqueoRepository repositorioBloqueo;
    private final SocioService servicioSocio;
    private final EjemplarService servicioEjemplar;
    private final PrestamoRepository repositorioPrestamo;
    private final WebhookService servicioWebhook;
    private final EntityManager gestorEntidades;

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

    @Transactional
    public Prestamo formalizarBloqueo(Long idBloqueo, String usuario, boolean esBibliotecario) {
        Objects.requireNonNull(idBloqueo, "El ID del bloqueo es requerido.");
        validarEntrada(usuario, "El usuario no puede ser nulo o vacío.");

        LOG.info("Formalizando bloqueo - ID: {}", idBloqueo);

        Bloqueo bloqueo = buscarBloqueo(idBloqueo);
        validarPropiedad(bloqueo, usuario, esBibliotecario);
        validarEstadoActivo(bloqueo);

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

        long reservasActivas = repositorioBloqueo.countActiveBloqueosBySocio(socio.getIdSocio(), EstadoBloqueo.ACTIVO,
                new Date());
        if (reservasActivas > 0) {
            throw new IllegalStateException("El usuario ya tiene una reserva activa. Máximo permitido: 1.");
        }
    }

    private void validarEstadoActivo(Bloqueo bloqueo) {
        if (bloqueo.getEstado() != EstadoBloqueo.ACTIVO) {
            throw new IllegalStateException("El bloqueo no está activo.");
        }
    }

    private Bloqueo construirBloqueoInicial(Socio socio, Ejemplar ejemplar) {
        return Bloqueo.builder()
                .socio(socio)
                .ejemplar(ejemplar)
                .fechaInicio(new Date())
                .fechaFin(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .estado(EstadoBloqueo.ACTIVO)
                .build();
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
                servicioWebhook.notificarNuevaReserva(socio.getUsuario(), bloqueo.getEjemplar().getLibro().getTitulo());
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
        prestamo.setFechaPrevistaDevolucion(Date.from(Instant.now().plus(15, ChronoUnit.DAYS)));
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
}
