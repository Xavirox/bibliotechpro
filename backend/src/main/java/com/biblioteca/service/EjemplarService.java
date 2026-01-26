package com.biblioteca.service;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.repository.EjemplarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EjemplarService {

    private static final Logger log = LoggerFactory.getLogger(EjemplarService.class);
    private final EjemplarRepository ejemplarRepository;

    public EjemplarService(EjemplarRepository ejemplarRepository) {
        this.ejemplarRepository = ejemplarRepository;
    }

    @Transactional(readOnly = true)
    public List<Ejemplar> getEjemplares(Long idLibro, String estado) {
        if (idLibro != null) {
            return ejemplarRepository.findByLibroIdLibroWithLibro(idLibro);
        }
        if (estado != null) {
            return ejemplarRepository.findByEstadoWithLibro(estado);
        }
        return ejemplarRepository.findAll();
    }

    @Transactional
    public void corregirConsistencia() {
        log.info("Iniciando corrección de consistencia de ejemplares...");
        ejemplarRepository.fixOrphanedPrestamos();
        ejemplarRepository.fixOrphanedBloqueos();
        log.info("Corrección de consistencia finalizada.");
    }
}
