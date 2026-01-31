package com.biblioteca.service;

import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SocioService {

    private static final Logger LOG = LoggerFactory.getLogger(SocioService.class);
    private final SocioRepository repositorioSocio;

    public SocioService(SocioRepository repositorioSocio) {
        this.repositorioSocio = repositorioSocio;
    }

    /**
     * Recupera todos los socios registrados en el sistema.
     *
     * @return Lista completa de entidades Socio.
     */
    @Transactional(readOnly = true)
    public List<Socio> obtenerTodosLosSocios() {
        return repositorioSocio.findAll();
    }

    /**
     * Recupera todos los socios y los mapea a DTOs para evitar exponer la entidad
     * completa.
     *
     * @return Lista de SocioDTO.
     */
    @Transactional(readOnly = true)
    public List<com.biblioteca.dto.SocioDTO> obtenerTodosLosSociosDTO() {
        return repositorioSocio.findAll().stream()
                .map(this::mapearADTO)
                .toList();
    }

    /**
     * Busca un socio por su nombre de usuario.
     *
     * @param usuario Nombre de usuario a buscar.
     * @return Optional conteniendo el Socio si existe.
     */
    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorUsuario(String usuario) {
        return repositorioSocio.findByUsuario(usuario);
    }

    /**
     * Busca un socio por su ID primario.
     *
     * @param id ID del socio.
     * @return Optional conteniendo el Socio si existe.
     */
    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorId(@NonNull Long id) {
        return repositorioSocio.findById(id);
    }

    /**
     * Aplica una penalización temporal a un socio, impidiéndole realizar nuevos
     * préstamos/reservas.
     *
     * @param idSocio ID del socio a penalizar.
     * @param dias    Número de días de penalización a partir de hoy.
     * @throws IllegalArgumentException Si el socio no existe o el ID es nulo.
     */
    @Transactional
    public void penalizarSocio(@NonNull Long idSocio, int dias) {
        validarId(idSocio);
        Socio socio = buscarSocioPorIdOError(idSocio);

        Date fechaPenalizacion = Date.from(Instant.now().plus(dias, ChronoUnit.DAYS));
        socio.setPenalizacionHasta(fechaPenalizacion);

        repositorioSocio.save(socio);
        LOG.info("Socio '{}' (ID: {}) penalizado por {} días. Hasta: {}",
                socio.getUsuario(), idSocio, dias, fechaPenalizacion);
    }

    /**
     * Actualiza el límite máximo de préstamos activos permitidos para un socio
     * específico.
     *
     * @param idSocio      ID del socio.
     * @param maxPrestamos Nuevo límite máximo de préstamos.
     * @return El socio actualizado.
     * @throws IllegalArgumentException Si el socio no existe.
     */
    @Transactional
    public Socio actualizarLimitePrestamos(@NonNull Long idSocio, Integer maxPrestamos) {
        validarId(idSocio);
        if (maxPrestamos != null && maxPrestamos < 0) {
            throw new IllegalArgumentException("El límite de préstamos no puede ser negativo");
        }

        Socio socio = buscarSocioPorIdOError(idSocio);

        Integer limiteAnterior = socio.getMaxPrestamosActivos();
        socio.setMaxPrestamosActivos(maxPrestamos);

        Socio guardado = repositorioSocio.save(socio);
        LOG.info("Límite de préstamos actualizado para socio '{}': {} -> {}",
                socio.getUsuario(), limiteAnterior, maxPrestamos);

        return guardado;
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODOS PRIVADOS
    // ------------------------------------------------------------------------------------------------

    private void validarId(Long id) {
        Objects.requireNonNull(id, "El ID del socio no puede ser nulo");
    }

    private Socio buscarSocioPorIdOError(@NonNull Long id) {
        return repositorioSocio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + id));
    }

    private com.biblioteca.dto.SocioDTO mapearADTO(Socio socio) {
        return new com.biblioteca.dto.SocioDTO(
                socio.getUsuario(),
                socio.getNombre(),
                socio.getEmail(),
                socio.getRol(),
                socio.getMaxPrestamosActivos());
    }
}
