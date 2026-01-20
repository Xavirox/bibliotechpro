package com.biblioteca.service;

import com.biblioteca.model.Bloqueo;
import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.BloqueoRepository;
import com.biblioteca.repository.EjemplarRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Servicio para gestión de bloqueos (reservas de 24h).
 * 
 * <h2>ARQUITECTURA: Responsabilidades compartidas con triggers Oracle</h2>
 * <p>
 * Este proyecto utiliza AMBOS mecanismos para la lógica de negocio:
 * </p>
 * 
 * <h3>Triggers Oracle (db/triggers.sql):</h3>
 * <ul>
 * <li>TRG_VALIDAR_BLOQUEO: Valida límite de 1 bloqueo activo por socio</li>
 * <li>TRG_ACTUALIZAR_EJEMPLAR_BLOQUEO: Cambia estado ejemplar a BLOQUEADO</li>
 * <li>TRG_VALIDAR_PRESTAMO: Valida penalización, límite préstamos</li>
 * <li>TRG_ACTUALIZAR_ESTADOS_PRESTAMO: Actualiza estados al crear préstamo</li>
 * </ul>
 * 
 * <h3>Servicios Java (esta clase):</h3>
 * <ul>
 * <li>Validación previa para dar mensajes de error amigables</li>
 * <li>Transaccionalidad y rollback</li>
 * <li>Logging estructurado</li>
 * <li>Lógica de negocio no cubierta por triggers</li>
 * </ul>
 * 
 * <p>
 * <strong>NOTA:</strong> Algunas validaciones están duplicadas intencionalmente
 * para proporcionar mejor UX. El trigger es la última línea de defensa.
 * </p>
 * 
 * @see com.biblioteca.service.PrestamoService
 */
@Service
public class BloqueoService {

    private static final Logger log = LoggerFactory.getLogger(BloqueoService.class);

    private final BloqueoRepository bloqueoRepository;
    private final SocioRepository socioRepository;
    private final EjemplarRepository ejemplarRepository;
    private final PrestamoRepository prestamoRepository;

    public BloqueoService(BloqueoRepository bloqueoRepository,
            SocioRepository socioRepository,
            EjemplarRepository ejemplarRepository,
            PrestamoRepository prestamoRepository) {
        this.bloqueoRepository = bloqueoRepository;
        this.socioRepository = socioRepository;
        this.ejemplarRepository = ejemplarRepository;
        this.prestamoRepository = prestamoRepository;
    }

    /**
     * Creates a reservation (block) for a copy.
     * Transactional: updates both Bloqueo and Ejemplar atomically.
     */
    @Transactional
    public Bloqueo crearBloqueo(String username, @NonNull Long idEjemplar) {
        log.info("Creando bloqueo - usuario: {}, ejemplar: {}", username, idEjemplar);

        Socio socio = socioRepository.findByUsuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        // VALIDACIÓN: Límite de 1 bloqueo activo por socio (con verificación de
        // expiración real)
        java.util.Date now = new java.util.Date();
        long bloqueosActivos = bloqueoRepository.findBySocioIdSocio(socio.getIdSocio()).stream()
                .filter(b -> "ACTIVO".equals(b.getEstado()))
                .filter(b -> b.getFechaFin() != null && b.getFechaFin().after(now))
                .count();

        if (bloqueosActivos > 0) {
            throw new IllegalStateException(
                    "Ya tienes una reserva activa. Cancélala o espera a que expire antes de hacer otra.");
        }

        Ejemplar ejemplar = ejemplarRepository.findById(idEjemplar)
                .orElseThrow(() -> new IllegalArgumentException("Ejemplar no encontrado con ID: " + idEjemplar));

        if (!"DISPONIBLE".equals(ejemplar.getEstado())) {
            throw new IllegalStateException("El ejemplar no está disponible. Estado actual: " + ejemplar.getEstado());
        }

        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setSocio(socio);
        bloqueo.setEjemplar(ejemplar);
        bloqueo.setFechaInicio(new Date());
        bloqueo.setFechaFin(new Date(System.currentTimeMillis() + 86400000)); // +1 day
        bloqueo.setEstado("ACTIVO");

        // Update copy status
        ejemplar.setEstado("BLOQUEADO");

        // Save atomically
        ejemplarRepository.save(ejemplar);

        try {
            Bloqueo savedBloqueo = bloqueoRepository.save(bloqueo);
            log.info("Bloqueo creado exitosamente - id: {}", savedBloqueo.getIdBloqueo());
            return savedBloqueo;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // SEGURIDAD: Capturar violación del índice único condicional (double-submit
            // race condition)
            log.warn("Violación de integridad al crear bloqueo (posible duplicado concurrente) - usuario: {}",
                    username);
            throw new IllegalStateException("Ya tienes una reserva activa. (Error de concurrencia detectado)");
        }
    }

    /**
     * Cancels a reservation.
     * Transactional: updates both Bloqueo and Ejemplar atomically.
     */
    @Transactional
    public void cancelarBloqueo(@NonNull Long idBloqueo, String username) {
        log.info("Cancelando bloqueo - id: {}, usuario: {}", idBloqueo, username);

        Bloqueo bloqueo = bloqueoRepository.findById(idBloqueo)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueo no encontrado con ID: " + idBloqueo));

        // Verify ownership
        if (!bloqueo.getSocio().getUsuario().equals(username)) {
            throw new SecurityException("No tienes permiso para cancelar este bloqueo");
        }

        if (!"ACTIVO".equals(bloqueo.getEstado())) {
            throw new IllegalStateException("El bloqueo no está activo. Estado actual: " + bloqueo.getEstado());
        }

        bloqueo.setEstado("CANCELADO");
        Ejemplar ejemplar = bloqueo.getEjemplar();
        ejemplar.setEstado("DISPONIBLE");

        // Save atomically
        bloqueoRepository.save(bloqueo);
        ejemplarRepository.save(ejemplar);

        log.info("Bloqueo cancelado exitosamente - id: {}", idBloqueo);
    }

    /**
     * Converts a reservation to a loan (formalize).
     * Transactional: creates Prestamo and updates Bloqueo atomically.
     * 
     * SEGURIDAD H-01: Verifica propiedad del bloqueo.
     * 
     * @param idBloqueo       ID del bloqueo a formalizar
     * @param username        Usuario que intenta formalizar
     * @param esBibliotecario Si true, puede formalizar cualquier bloqueo
     * @throws SecurityException si el usuario no tiene permiso
     */
    @Transactional
    public Prestamo formalizarBloqueo(@NonNull Long idBloqueo, String username, boolean esBibliotecario) {
        log.info("Formalizando bloqueo a préstamo - id: {}, usuario: {}", idBloqueo, username);

        Bloqueo bloqueo = bloqueoRepository.findById(idBloqueo)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + idBloqueo));

        // SEGURIDAD H-01: Verificar propiedad del bloqueo
        if (!esBibliotecario && !bloqueo.getSocio().getUsuario().equals(username)) {
            log.warn("Intento de formalizar bloqueo ajeno - bloqueo: {}, dueño: {}, solicitante: {}",
                    idBloqueo, bloqueo.getSocio().getUsuario(), username);
            throw new SecurityException("No puedes formalizar una reserva que no es tuya");
        }

        if (!"ACTIVO".equals(bloqueo.getEstado())) {
            throw new IllegalStateException("La reserva no está activa. Estado actual: " + bloqueo.getEstado());
        }

        // SEGURIDAD C-03: Verificar expiración en tiempo real
        Date now = new Date();
        if (bloqueo.getFechaFin() != null && bloqueo.getFechaFin().before(now)) {
            log.warn("Intento de formalizar bloqueo expirado - id: {}, fechaFin: {}", idBloqueo, bloqueo.getFechaFin());
            throw new IllegalStateException("La reserva ha expirado. Por favor, crea una nueva reserva.");
        }

        // SEGURIDAD C-03: Verificar consistencia del estado del ejemplar
        Ejemplar ejemplar = bloqueo.getEjemplar();
        if (!"BLOQUEADO".equals(ejemplar.getEstado())) {
            log.warn("Estado inconsistente del ejemplar al formalizar - bloqueo: {}, estadoEjemplar: {}",
                    idBloqueo, ejemplar.getEstado());
            throw new IllegalStateException(
                    "El ejemplar no está en estado bloqueado. Estado actual: " + ejemplar.getEstado());
        }

        // Create the loan
        Prestamo prestamo = new Prestamo();
        prestamo.setSocio(bloqueo.getSocio());
        prestamo.setEjemplar(bloqueo.getEjemplar());
        prestamo.setFechaPrestamo(new Date());
        prestamo.setFechaPrevistaDevolucion(new Date(System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000)));
        prestamo.setEstado("ACTIVO");
        prestamo.setBloqueo(bloqueo);

        // The trigger will update Bloqueo and Ejemplar states, but we save the loan
        Prestamo savedPrestamo = prestamoRepository.save(prestamo);

        log.info("Bloqueo formalizado exitosamente - préstamo id: {}", savedPrestamo.getIdPrestamo());
        return savedPrestamo;
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> getMisBloqueos(String username) {
        Socio socio = socioRepository.findByUsuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        java.util.Date now = new java.util.Date();
        return bloqueoRepository.findBySocioIdSocio(socio.getIdSocio()).stream()
                .filter(b -> "ACTIVO".equals(b.getEstado()))
                // SEGURIDAD: Verificar expiración en tiempo real (no solo depender del job
                // nocturno)
                .filter(b -> b.getFechaFin() != null && b.getFechaFin().after(now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> getBloqueosActivos() {
        java.util.Date now = new java.util.Date();
        // SEGURIDAD: Solo devolver bloqueos que NO hayan expirado
        // OPTIMIZACIÓN: Usar JOIN FETCH para evitar N+1 y errores de serialización
        return bloqueoRepository.findByEstadoWithDetails("ACTIVO").stream()
                .filter(b -> b.getFechaFin() != null && b.getFechaFin().after(now))
                .toList();
    }

    /**
     * Recovery endpoint: Manually trigger the cleanup job.
     * Uses DB stored procedure.
     */
    @Transactional
    public void limpiarBloqueosExpirados() {
        log.info("Ejecutando limpieza manual de bloqueos expirados...");
        bloqueoRepository.ejecutarLimpiezaBloqueos();
        log.info("Limpieza manual finalizada.");
    }
}
