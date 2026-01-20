package com.biblioteca.config;

import com.biblioteca.repository.EjemplarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ConsistenciaRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ConsistenciaRunner.class);

    @Autowired
    EjemplarRepository ejemplarRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("--- EJECUTANDO REPARACIÓN DE CONSISTENCIA DE BASE DE DATOS ---");
        try {
            ejemplarRepository.fixOrphanedPrestamos();
            ejemplarRepository.fixOrphanedBloqueos();
            logger.info("--- REPARACIÓN COMPLETADA CON ÉXITO ---");
        } catch (Exception e) {
            logger.error("--- ERROR EN REPARACIÓN ---", e);
        }
    }
}
