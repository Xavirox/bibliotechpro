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
@lombok.RequiredArgsConstructor
public class SocioService {

    private static final Logger LOG = LoggerFactory.getLogger(SocioService.class);
    private final SocioRepository repositorioSocio;

    @Transactional(readOnly = true)
    public List<Socio> obtenerTodosLosSocios() {
        return repositorioSocio.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorUsuario(String usuario) {
        return repositorioSocio.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Socio> buscarPorId(@NonNull Long id) {
        return repositorioSocio.findById(id);
    }

    @Transactional
    public void penalizarSocio(@NonNull Long idSocio, int dias) {
        Objects.requireNonNull(idSocio, "El ID del socio no puede ser nulo");
        Socio socio = repositorioSocio.findById(idSocio)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + idSocio));

        Date fechaPenalizacion = Date.from(Instant.now().plus(dias, ChronoUnit.DAYS));

        socio.setPenalizacionHasta(fechaPenalizacion);
        repositorioSocio.save(socio);
        LOG.info("Socio {} penalizado hasta {}", socio.getUsuario(), fechaPenalizacion);
    }

    @Transactional
    public Socio actualizarLimitePrestamos(@NonNull Long idSocio, Integer maxPrestamos) {
        Objects.requireNonNull(idSocio, "El ID del socio no puede ser nulo");
        Socio socio = repositorioSocio.findById(idSocio)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + idSocio));

        socio.setMaxPrestamosActivos(maxPrestamos);
        return repositorioSocio.save(socio);
    }
}
