package com.biblioteca.controller;

import com.biblioteca.dto.BloqueoRequest;
import com.biblioteca.model.Bloqueo;
import com.biblioteca.model.Prestamo;
import com.biblioteca.service.BloqueoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/bloqueos")
@Tag(name = "Bloqueos/Reservas", description = "API para gestión de reservas de libros")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    public BloqueoController(BloqueoService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    @PostMapping
    @Operation(summary = "Crear reserva", description = "Reserva un ejemplar por 24 horas")
    public ResponseEntity<?> crearBloqueo(@Valid @RequestBody BloqueoRequest request) {
        // Validación explícita para devolver 400 en lugar de 500
        if (request.getIdEjemplar() == null) {
            return ResponseEntity.badRequest().body("El campo 'idEjemplar' es obligatorio");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Bloqueo bloqueo = bloqueoService.crearBloqueo(username, Objects.requireNonNull(request.getIdEjemplar()));
            return ResponseEntity.ok(bloqueo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mios")
    @Operation(summary = "Mis reservas", description = "Obtiene las reservas activas del usuario autenticado")
    public List<Bloqueo> getMisBloqueos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return bloqueoService.getMisBloqueos(username);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva propia")
    public ResponseEntity<?> cancelarBloqueo(@PathVariable(required = true) Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("ID Bloqueo requerido");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            bloqueoService.cancelarBloqueo(id, username);
            return ResponseEntity.ok("Bloqueo cancelado");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/activos")
    @Operation(summary = "Reservas activas", description = "Lista todas las reservas activas (solo bibliotecarios)")
    public List<Bloqueo> getBloqueosActivos() {
        return bloqueoService.getBloqueosActivos();
    }

    @PostMapping("/{id}/formalizar")
    @Operation(summary = "Formalizar reserva", description = "Convierte una reserva en préstamo (bibliotecarios pueden formalizar cualquiera, socios solo las propias)")
    public ResponseEntity<?> formalizarBloqueo(@PathVariable(required = true) Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("ID requerido");
        }

        try {
            // SEGURIDAD H-01: Obtener usuario y verificar permisos
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            boolean esBibliotecario = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIO")
                            || a.getAuthority().equals("ROLE_ADMIN"));

            Prestamo prestamo = bloqueoService.formalizarBloqueo(id, username, esBibliotecario);
            return ResponseEntity.ok(prestamo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Limpieza manual", description = "Ejecuta el job de limpieza de bloqueos expirados manualmente (Solo ADMIN)")
    public ResponseEntity<?> cleanupBloqueos() {
        // SEGURIDAD: La restricción a ROLE_ADMIN está configurada en SecurityConfig
        bloqueoService.limpiarBloqueosExpirados();
        return ResponseEntity.ok("Limpieza ejecutada");
    }
}
