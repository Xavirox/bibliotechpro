package com.biblioteca.service;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.repository.EjemplarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class EjemplarService {

    private static final Logger LOG = LoggerFactory.getLogger(EjemplarService.class);
    private final EjemplarRepository repositorioEjemplar;

    @Transactional(readOnly = true)
    public List<Ejemplar> listarEjemplares(Long idLibro, String estado) {
        if (idLibro != null) {
            return repositorioEjemplar.findByLibroIdLibroWithLibro(idLibro);
        }
        if (estado != null) {
            return repositorioEjemplar.findByEstadoWithLibro(EstadoEjemplar.valueOf(estado));
        }
        return repositorioEjemplar.findAll();
    }

    @Transactional(readOnly = true)
    public Ejemplar buscarEjemplarPorId(@NonNull Long id) {
        return repositorioEjemplar.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejemplar no encontrado con ID: " + id));
    }

    @Transactional
    public void actualizarEstadoEjemplar(@NonNull Long idEjemplar, @NonNull EstadoEjemplar nuevoEstado) {
        Ejemplar ejemplar = buscarEjemplarPorId(idEjemplar);
        ejemplar.setEstado(nuevoEstado);
        repositorioEjemplar.save(ejemplar);
        LOG.debug("Estado del ejemplar {} actualizado a {}", idEjemplar, nuevoEstado);
    }

    @Transactional
    public void corregirConsistencia() {
        LOG.info("Iniciando corrección de consistencia de ejemplares...");
        repositorioEjemplar.fixOrphanedPrestamos();
        repositorioEjemplar.fixOrphanedBloqueos();
        LOG.info("Corrección de consistencia finalizada.");
    }
}
