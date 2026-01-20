package com.biblioteca.service;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.Prestamo;
import com.biblioteca.model.Socio;
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
 * Servicio para gestión de préstamos de libros.
 * 
 * <h2>ARQUITECTURA: Triggers Oracle actúan como segunda línea de
 * validación</h2>
 * <p>
 * La lógica de negocio está implementada en DOS capas:
 * </p>
 * 
 * <h3>1. Esta capa Java (validación principal):</h3>
 * <ul>
 * <li>Valida disponibilidad del ejemplar</li>
 * <li>Proporciona mensajes de error claros para el frontend</li>
 * <li>Gestiona transaccionalidad Spring</li>
 * </ul>
 * 
 * <h3>2. Triggers Oracle (validación de seguridad en DB):</h3>
 * <ul>
 * <li>TRG_VALIDAR_PRESTAMO: Verifica penalización, límite de préstamos, estado
 * ejemplar</li>
 * <li>TRG_ACTUALIZAR_ESTADOS_PRESTAMO: Actualiza estado del ejemplar y
 * bloqueo</li>
 * <li>TRG_DEVOLUCION_PRESTAMO: Libera ejemplar al devolver</li>
 * </ul>
 * 
 * <p>
 * <strong>DECISIÓN DE DISEÑO:</strong> Se mantiene duplicación controlada para:
 * </p>
 * <ul>
 * <li>Mejor UX con mensajes de error claros desde Java</li>
 * <li>Integridad garantizada a nivel de BD (triggers)</li>
 * <li>Cumplimiento de requisitos del módulo ASIR (uso de triggers)</li>
 * </ul>
 * 
 * @see BloqueoService
 */
@Service
public class PrestamoService {

    private static final Logger log = LoggerFactory.getLogger(PrestamoService.class);

    private final PrestamoRepository prestamoRepository;
    private final SocioRepository socioRepository;
    private final EjemplarRepository ejemplarRepository;

    public PrestamoService(PrestamoRepository prestamoRepository,
            SocioRepository socioRepository,
            EjemplarRepository ejemplarRepository) {
        this.prestamoRepository = prestamoRepository;
        this.socioRepository = socioRepository;
        this.ejemplarRepository = ejemplarRepository;
    }

    /**
     * Creates a new loan for a member.
     * This operation is transactional: if any step fails, all changes are rolled
     * back.
     */
    @Transactional
    public Prestamo crearPrestamo(@NonNull Long idSocio, @NonNull Long idEjemplar) {
        log.info("Creando préstamo - socio: {}, ejemplar: {}", idSocio, idEjemplar);

        Socio socio = socioRepository.findById(idSocio)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + idSocio));

        // VALIDACIÓN: Socio penalizado
        if (socio.getPenalizacionHasta() != null && socio.getPenalizacionHasta().after(new Date())) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            throw new IllegalStateException(
                    "El socio está penalizado hasta " + sdf.format(socio.getPenalizacionHasta()));
        }

        // VALIDACIÓN: Límite de préstamos activos
        long prestamosActivos = prestamoRepository.findBySocioIdSocio(idSocio).stream()
                .filter(p -> "ACTIVO".equals(p.getEstado()))
                .count();
        int maxPrestamos = socio.getMaxPrestamosActivos() != null ? socio.getMaxPrestamosActivos() : 3;
        if (prestamosActivos >= maxPrestamos) {
            throw new IllegalStateException(
                    "El socio ha alcanzado el límite máximo de préstamos activos (" + maxPrestamos + ")");
        }

        Ejemplar ejemplar = ejemplarRepository.findById(idEjemplar)
                .orElseThrow(() -> new IllegalArgumentException("Ejemplar no encontrado con ID: " + idEjemplar));

        if (!"DISPONIBLE".equals(ejemplar.getEstado())) {
            throw new IllegalStateException("El ejemplar no está disponible. Estado actual: " + ejemplar.getEstado());
        }

        // Create the loan
        Prestamo prestamo = new Prestamo();
        prestamo.setSocio(socio);
        prestamo.setEjemplar(ejemplar);
        prestamo.setFechaPrestamo(new Date());
        prestamo.setFechaPrevistaDevolucion(new Date(System.currentTimeMillis() + (15L * 86400000L))); // +15 days
        prestamo.setEstado("ACTIVO");

        // Update the copy status
        ejemplar.setEstado("PRESTADO");

        // Save both in a single transaction
        ejemplarRepository.save(ejemplar);
        Prestamo savedPrestamo = prestamoRepository.save(prestamo);

        log.info("Préstamo creado exitosamente - id: {}", savedPrestamo.getIdPrestamo());
        return savedPrestamo;
    }

    /**
     * Returns a loan with ownership verification.
     * This operation is transactional: updates both loan and copy status
     * atomically.
     * 
     * @param idPrestamo      ID del préstamo a devolver
     * @param username        Usuario que intenta devolver
     * @param esBibliotecario Si true, puede devolver cualquier préstamo
     * @throws SecurityException si el usuario no tiene permiso para devolver este
     *                           préstamo
     */
    @Transactional
    public void devolverPrestamo(@NonNull Long idPrestamo, String username, boolean esBibliotecario) {
        log.info("Devolviendo préstamo - id: {}, usuario: {}, esBibliotecario: {}", idPrestamo, username,
                esBibliotecario);

        Prestamo prestamo = prestamoRepository.findById(idPrestamo)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + idPrestamo));

        // SEGURIDAD: Verificar propiedad del préstamo
        if (!esBibliotecario && !prestamo.getSocio().getUsuario().equals(username)) {
            log.warn("Intento de devolver préstamo ajeno - préstamo: {}, dueño: {}, solicitante: {}",
                    idPrestamo, prestamo.getSocio().getUsuario(), username);
            throw new SecurityException("No puedes devolver un préstamo que no es tuyo");
        }

        if (!"ACTIVO".equals(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo no está activo. Estado actual: " + prestamo.getEstado());
        }

        // Update loan
        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaDevolucionReal(new Date());

        // Update copy status
        Ejemplar ejemplar = prestamo.getEjemplar();
        ejemplar.setEstado("DISPONIBLE");

        // Save both atomically
        prestamoRepository.save(prestamo);
        ejemplarRepository.save(ejemplar);

        log.info("Préstamo devuelto exitosamente - id: {}", idPrestamo);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> getAllPrestamos(String estado) {
        // OPTIMIZACIÓN: Usar JOIN FETCH para evitar N+1 y errores de serialización
        if (estado != null && !estado.isEmpty()) {
            return prestamoRepository.findByEstadoWithDetails(estado);
        }
        return prestamoRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> getMisPrestamos(String username) {
        Long idSocio = socioRepository.findByUsuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username))
                .getIdSocio();
        // OPTIMIZACIÓN: Usar JOIN FETCH para evitar N+1 y errores de serialización
        return prestamoRepository.findBySocioIdSocioWithDetails(idSocio);
    }
}
