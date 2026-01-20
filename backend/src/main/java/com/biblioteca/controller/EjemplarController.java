package com.biblioteca.controller;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.repository.EjemplarRepository;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/ejemplares")
@Tag(name = "Ejemplares", description = "API para consulta de ejemplares")
public class EjemplarController {

    private final EjemplarRepository ejemplarRepository;

    public EjemplarController(EjemplarRepository ejemplarRepository) {
        this.ejemplarRepository = ejemplarRepository;
    }

    @GetMapping
    @Operation(summary = "Listar ejemplares", description = "Busca ejemplares por libro o estado")
    public List<Ejemplar> getEjemplares(@RequestParam(required = false) Long idLibro,
            @RequestParam(required = false) String estado) {
        if (idLibro != null) {
            return ejemplarRepository.findByLibroIdLibro(idLibro);
        }
        if (estado != null) {
            // OPTIMIZACIÓN: Usar JOIN FETCH para cargar relación libro
            // El frontend necesita acceder a copy.libro.idLibro para determinar
            // disponibilidad
            return ejemplarRepository.findByEstadoWithLibro(estado);
        }
        return ejemplarRepository.findAll();
    }
}
