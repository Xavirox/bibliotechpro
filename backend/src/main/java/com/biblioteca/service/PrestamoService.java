package com.biblioteca.service;

import com.biblioteca.events.PrestamoDevueltoEvent;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.EjemplarRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class PrestamoService {

    private static final Logger log = LoggerFactory.getLogger(PrestamoService.class);
    private static final long DIAS_PRESTAMO = 15L;

    private final PrestamoRepository prestamoRepository;
    private final SocioRepository socioRepository;
    private final EjemplarRepository ejemplarRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WebhookService webhookService;
    private final jakarta.persistence.EntityManager entityManager;

    public PrestamoService(PrestamoRepository prestamoRepository, SocioRepository socioRepository,
            EjemplarRepository ejemplarRepository, ApplicationEventPublisher eventPublisher,
            WebhookService webhookService, jakarta.persistence.EntityManager entityManager) {
        this.prestamoRepository = prestamoRepository;
        this.socioRepository = socioRepository;
        this.ejemplarRepository = ejemplarRepository;
        this.eventPublisher = eventPublisher;
        this.webhookService = webhookService;
        this.entityManager = entityManager;
    }

    @Transactional
    public Prestamo crearPrestamo(Long idSocio, Long idEjemplar) {
        log.info("Creando préstamo - Socio: {}, Ejemplar: {}", idSocio, idEjemplar);

        Socio socio = buscarSocio(idSocio);
        Ejemplar ejemplar = buscarEjemplar(idEjemplar);

        // Validación preventiva en Java
        if (!"DISPONIBLE".equals(ejemplar.getEstado()) && !"BLOQUEADO".equals(ejemplar.getEstado())) {
            throw new IllegalStateException("El ejemplar no está disponible (Estado: " + ejemplar.getEstado() + ")");
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setSocio(socio);
        prestamo.setEjemplar(ejemplar);
        prestamo.setEstado("ACTIVO");
        prestamo.setFechaPrestamo(new Date());
        prestamo.setFechaPrevistaDevolucion(calculaFechaVencimiento());

        try {
            // Persistir Préstamo
            Prestamo guardado = prestamoRepository.save(prestamo);

            // Actualizar estado del Ejemplar
            ejemplar.setEstado("PRESTADO");
            // Nota: Al estar en una transacción, Hibernate detectará el cambio en
            // 'ejemplar' y lo guardará al finalizar.
            // Aún así, podemos forzar el guardado si lo deseamos, pero no es estrictamente
            // necesario con JPA MANAGED entities.

            entityManager.flush();
            entityManager.refresh(guardado); // Recargar para tener datos frescos de la DB (IDs, defaults)

            webhookService.notificarNuevoPrestamo(socio.getNombre(), ejemplar.getLibro().getTitulo());
            return guardado;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Error de integridad al crear préstamo: {}", e.getMessage());
            throw new IllegalStateException("El ejemplar ya no está disponible o se han violado reglas de negocio.", e);
        }
    }

    @Transactional
    public void devolverPrestamo(Long idPrestamo, String usuarioEjecutor, boolean esAutoridad) {
        log.info("Devolviendo préstamo - ID: {}", idPrestamo);

        Prestamo prestamo = prestamoRepository.findByIdWithDetails(idPrestamo)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado"));

        // Idempotencia: Si ya está devuelto, lanzamos excepción (alineado con tests)
        if ("DEVUELTO".equals(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo ya fue devuelto");
        }

        validarDevolucion(prestamo, usuarioEjecutor, esAutoridad);

        // Actualizar estado
        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaDevolucionReal(new Date());

        // Liberar ejemplar en memoria para consistencia
        Ejemplar ejemplar = prestamo.getEjemplar();
        if (ejemplar != null) {
            ejemplar.setEstado("DISPONIBLE");
            ejemplarRepository.save(ejemplar);
        }

        prestamoRepository.save(prestamo);

        // Sincronizar estado con la DB (Trigger TRG_DEVOLUCION_PRESTAMO)
        entityManager.flush();
        entityManager.refresh(prestamo);

        eventPublisher.publishEvent(new PrestamoDevueltoEvent(this, prestamo));
    }

    private void validarDevolucion(Prestamo prestamo, String usuario, boolean esAutoridad) {
        if (!esAutoridad) {
            boolean esPropietario = prestamo.getSocio() != null && prestamo.getSocio().getUsuario().equals(usuario);
            if (!esPropietario)
                throw new SecurityException("No tienes permiso para devolver este préstamo");
        }
    }

    private Date calculaFechaVencimiento() {
        return Date.from(Instant.now().plus(DIAS_PRESTAMO, ChronoUnit.DAYS));
    }

    private Socio buscarSocio(Long id) {
        Objects.requireNonNull(id, "ID Socio requerido");
        return socioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Socio no existe: " + id));
    }

    private Ejemplar buscarEjemplar(Long id) {
        Objects.requireNonNull(id, "ID Ejemplar requerido");
        return ejemplarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejemplar no existe: " + id));
    }

    @Transactional(readOnly = true)
    public List<Prestamo> getAllPrestamos(String estado) {
        return (estado != null) ? prestamoRepository.findByEstadoWithDetails(estado)
                : prestamoRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> getMisPrestamos(String usuario) {
        return prestamoRepository.findBySocioIdSocioWithDetails(
                socioRepository.findByUsuario(usuario)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario desconocido"))
                        .getIdSocio());
    }
}
