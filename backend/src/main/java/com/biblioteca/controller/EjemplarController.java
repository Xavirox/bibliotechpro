package com.biblioteca.controller;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.service.EjemplarService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/ejemplares")
@Tag(name = "Ejemplares", description = "API para consulta de ejemplares")
public class EjemplarController {

    private final EjemplarService ejemplarService;

    public EjemplarController(EjemplarService ejemplarService) {
        this.ejemplarService = ejemplarService;
    }

    @GetMapping
    @Operation(summary = "Listar ejemplares", description = "Busca ejemplares por libro o estado")
    public List<Ejemplar> getEjemplares(@RequestParam(required = false) Long idLibro,
            @RequestParam(required = false) String estado) {
        return ejemplarService.getEjemplares(idLibro, estado);
    }

    @PostMapping("/fix-consistency")
    @Operation(summary = "Corregir consistencia", description = "Sincroniza estados de ejemplares con préstamos/bloqueos activos (solo ADMIN)")
    public org.springframework.http.ResponseEntity<?> fixConsistency() {
        ejemplarService.corregirConsistencia();
        return org.springframework.http.ResponseEntity.ok("Consistencia corregida");
    }
}
