package com.biblioteca.controller;

import com.biblioteca.dto.BloqueoRequest;
import com.biblioteca.model.Bloqueo;
import com.biblioteca.model.Prestamo;
import com.biblioteca.service.BloqueoService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoService servicioBloqueo;

    @PostMapping
    @Operation(summary = "Crear reserva", description = "Reserva un ejemplar por 24 horas")
    public ResponseEntity<?> crearBloqueo(@Valid @RequestBody BloqueoRequest solicitud) {
        // Validación explícita para devolver 400 en lugar de 500
        if (solicitud.idEjemplar() == null) {
            return ResponseEntity.badRequest().body("El campo 'idEjemplar' es obligatorio");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = (auth != null) ? auth.getName() : "anonymous";
            Bloqueo bloqueo = servicioBloqueo.crearBloqueo(Objects.requireNonNull(usuario),
                    Objects.requireNonNull(solicitud.idEjemplar()));
            return ResponseEntity.ok(bloqueo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mios")
    @Operation(summary = "Mis reservas", description = "Obtiene las reservas activas del usuario autenticado")
    public List<Bloqueo> listarMisBloqueos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();
        return servicioBloqueo.obtenerBloqueosDeUsuario(usuario);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva propia")
    public ResponseEntity<?> cancelarBloqueo(@PathVariable(required = true) Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("ID Bloqueo requerido");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = auth.getName();
            servicioBloqueo.cancelarBloqueo(id, usuario);
            return ResponseEntity.ok("Bloqueo cancelado");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/activos")
    @Operation(summary = "Reservas activas", description = "Lista todas las reservas activas (solo bibliotecarios)")
    public List<Bloqueo> listarBloqueosActivos() {
        return servicioBloqueo.obtenerBloqueosActivos();
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
            String usuario = auth.getName();
            boolean esBibliotecario = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIO")
                            || a.getAuthority().equals("ROLE_ADMIN"));

            Prestamo prestamo = servicioBloqueo.formalizarBloqueo(id, usuario, esBibliotecario);
            return ResponseEntity.ok(prestamo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Limpieza manual", description = "Ejecuta el job de limpieza de bloqueos expirados manualmente (Solo ADMIN)")
    public ResponseEntity<?> ejecutarLimpiezaBloqueos() {
        // SEGURIDAD: La restricción a ROLE_ADMIN está configurada en SecurityConfig
        servicioBloqueo.limpiarBloqueosExpirados();
        return ResponseEntity.ok("Limpieza ejecutada");
    }
}
