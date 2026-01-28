package com.biblioteca.controller;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.service.EjemplarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ejemplares")
@Tag(name = "Ejemplares", description = "API para consulta de ejemplares")
public class EjemplarController {

    private final EjemplarService servicioEjemplar;

    public EjemplarController(EjemplarService servicioEjemplar) {
        this.servicioEjemplar = servicioEjemplar;
    }

    @GetMapping
    @Operation(summary = "Listar ejemplares", description = "Busca ejemplares por libro o estado")
    public List<Ejemplar> listarEjemplares(@RequestParam(required = false) Long idLibro,
            @RequestParam(required = false) String estado) {
        return servicioEjemplar.listarEjemplares(idLibro, estado);
    }

    @PostMapping("/fix-consistency")
    @Operation(summary = "Corregir consistencia", description = "Sincroniza estados de ejemplares con préstamos/bloqueos activos (solo ADMIN)")
    public ResponseEntity<?> corregirConsistencia() {
        servicioEjemplar.corregirConsistencia();
        return ResponseEntity.ok("Consistencia corregida");
    }
}
