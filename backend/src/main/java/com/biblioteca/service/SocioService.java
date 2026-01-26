package com.biblioteca.service;

import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SocioService.class);
    private final SocioRepository socioRepository;

    public SocioService(SocioRepository socioRepository) {
        this.socioRepository = socioRepository;
    }

    @Transactional(readOnly = true)
    public List<Socio> getAllSocios() {
        return socioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Socio> findByUsuario(String usuario) {
        return socioRepository.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Socio> findById(@org.springframework.lang.NonNull Long id) {
        return socioRepository.findById(id);
    }

    @Transactional
    public void penalizarSocio(@org.springframework.lang.NonNull Long idSocio, int dias) {
        Objects.requireNonNull(idSocio, "El ID del socio no puede ser nulo");
        Socio socio = socioRepository.findById(idSocio)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + idSocio));

        Date fechaPenalizacion = Date.from(Instant.now().plus(dias, ChronoUnit.DAYS));

        socio.setPenalizacionHasta(fechaPenalizacion);
        socioRepository.save(socio);
        log.info("Socio {} penalizado hasta {}", socio.getUsuario(), fechaPenalizacion);
    }

    @Transactional
    public Socio updateMaxPrestamos(@org.springframework.lang.NonNull Long idSocio, Integer maxPrestamos) {
        Objects.requireNonNull(idSocio, "El ID del socio no puede ser nulo");
        Socio socio = socioRepository.findById(idSocio)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + idSocio));

        socio.setMaxPrestamosActivos(maxPrestamos);
        return socioRepository.save(socio);
    }
}
