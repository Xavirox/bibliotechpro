package com.biblioteca.service;

import com.biblioteca.model.Bloqueo;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.BloqueoRepository;
import com.biblioteca.repository.EjemplarRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
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

    private static final Logger log = LoggerFactory.getLogger(BloqueoService.class);

    private final BloqueoRepository bloqueoRepository;
    private final SocioRepository socioRepository;
    private final EjemplarRepository ejemplarRepository;
    private final PrestamoRepository prestamoRepository;
    private final WebhookService webhookService;
    private final EntityManager entityManager;

    public BloqueoService(BloqueoRepository bloqueoRepository, SocioRepository socioRepository,
            EjemplarRepository ejemplarRepository, PrestamoRepository prestamoRepository,
            WebhookService webhookService, EntityManager entityManager) {
        this.bloqueoRepository = bloqueoRepository;
        this.socioRepository = socioRepository;
        this.ejemplarRepository = ejemplarRepository;
        this.prestamoRepository = prestamoRepository;
        this.webhookService = webhookService;
        this.entityManager = entityManager;
    }

    @Transactional
    public Bloqueo crearBloqueo(String username, Long idEjemplar) {
        log.info("Creando bloqueo - usuario: {}, ejemplar: {}", username, idEjemplar);

        Socio socio = findSocioOrThrow(username);
        Ejemplar ejemplar = findEjemplarOrThrow(idEjemplar);

        // Validación preventiva
        if (!"DISPONIBLE".equals(ejemplar.getEstado())) {
            throw new IllegalStateException(
                    "El ejemplar no está disponible para bloqueo (Estado: " + ejemplar.getEstado() + ")");
        }

        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setSocio(socio);
        bloqueo.setEjemplar(ejemplar);
        bloqueo.setFechaInicio(new Date());
        bloqueo.setFechaFin(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        bloqueo.setEstado("ACTIVO");

        try {
            // Persistir Bloqueo
            Bloqueo saved = bloqueoRepository.save(bloqueo);

            // Actualizar estado del Ejemplar
            ejemplar.setEstado("BLOQUEADO");
            ejemplarRepository.save(ejemplar); // Guardado explícito para asegurar persistencia

            entityManager.flush();
            entityManager.refresh(saved);

            webhookService.notificarNuevaReserva(username, ejemplar.getLibro().getTitulo());
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Error de integridad al crear bloqueo: {}", e.getMessage());
            throw new IllegalStateException(
                    "El ejemplar ya no está disponible o se ha alcanzado el límite de reservas.", e);
        }
    }

    @Transactional
    public void cancelarBloqueo(Long idBloqueo, String username) {
        log.info("Cancelando bloqueo - id: {}", idBloqueo);

        Bloqueo bloqueo = findBloqueoOrThrow(idBloqueo);
        validarPropiedad(bloqueo, username, false);

        if (!"ACTIVO".equals(bloqueo.getEstado())) {
            throw new IllegalStateException("El bloqueo no está activo");
        }

        bloqueo.setEstado("CANCELADO");

        // Liberar ejemplar
        Ejemplar ejemplar = bloqueo.getEjemplar();
        if (ejemplar != null) {
            ejemplar.setEstado("DISPONIBLE");
            ejemplarRepository.save(ejemplar);
        }

        bloqueoRepository.save(bloqueo);

        entityManager.flush();
    }

    @Transactional
    public Prestamo formalizarBloqueo(Long idBloqueo, String username, boolean esBibliotecario) {
        log.info("Formalizando bloqueo - id: {}", idBloqueo);

        Bloqueo bloqueo = findBloqueoOrThrow(idBloqueo);
        validarPropiedad(bloqueo, username, esBibliotecario);

        if (!"ACTIVO".equals(bloqueo.getEstado())) {
            throw new IllegalStateException("El bloqueo no está activo");
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setSocio(bloqueo.getSocio());
        prestamo.setEjemplar(bloqueo.getEjemplar());
        prestamo.setFechaPrestamo(new Date());
        prestamo.setFechaPrevistaDevolucion(Date.from(Instant.now().plus(15, ChronoUnit.DAYS)));
        prestamo.setEstado("ACTIVO");
        prestamo.setBloqueo(bloqueo);

        // Actualizar estados
        bloqueo.setEstado("CONVERTIDO");
        bloqueo.getEjemplar().setEstado("PRESTADO");

        Prestamo saved = prestamoRepository.save(prestamo);
        bloqueoRepository.save(bloqueo);

        entityManager.flush();
        entityManager.refresh(saved);

        return saved;
    }

    @Transactional
    public void limpiarBloqueosExpirados() {
        log.info("Ejecutando limpieza manual de bloqueos expirados");
        bloqueoRepository.ejecutarLimpiezaBloqueos();
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> getMisBloqueos(String username) {
        Socio socio = findSocioOrThrow(username);
        return bloqueoRepository.findActiveBloqueosBySocioWithDetails(socio.getIdSocio(), "ACTIVO", new Date());
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> getBloqueosActivos() {
        return bloqueoRepository.findByEstadoWithDetails("ACTIVO").stream()
                .filter(b -> b.getFechaFin() != null && b.getFechaFin().after(new Date()))
                .toList();
    }

    // --- Private Helpers ---

    private Socio findSocioOrThrow(String username) {
        return socioRepository.findByUsuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    private Ejemplar findEjemplarOrThrow(Long id) {
        Objects.requireNonNull(id, "El ID del ejemplar no puede ser nulo");
        return ejemplarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejemplar no encontrado: " + id));
    }

    private Bloqueo findBloqueoOrThrow(Long id) {
        Objects.requireNonNull(id, "El ID del bloqueo no puede ser nulo");
        return bloqueoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueo no encontrado: " + id));
    }

    private void validarPropiedad(Bloqueo bloqueo, String username, boolean esBibliotecario) {
        if (!esBibliotecario && (bloqueo.getSocio() == null || !bloqueo.getSocio().getUsuario().equals(username))) {
            throw new SecurityException("No tienes permiso sobre este bloqueo");
        }
    }
}
