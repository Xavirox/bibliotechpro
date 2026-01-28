package com.biblioteca.controller;

import com.biblioteca.dto.PrestamoRequest;
import com.biblioteca.model.Prestamo;
import com.biblioteca.service.PrestamoService;
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
@RequestMapping("/api/prestamos")
@Tag(name = "Préstamos", description = "API para gestión de préstamos de libros")
public class PrestamoController {

    private final PrestamoService servicioPrestamo;

    public PrestamoController(PrestamoService servicioPrestamo) {
        this.servicioPrestamo = servicioPrestamo;
    }

    @PostMapping
    @Operation(summary = "Crear préstamo", description = "Crea un nuevo préstamo para un socio (solo bibliotecarios)")
    public ResponseEntity<?> crearPrestamo(@Valid @RequestBody PrestamoRequest solicitud) {
        // Validaciones explícitas para devolver 400 en lugar de 500
        if (solicitud.getIdSocio() == null) {
            return ResponseEntity.badRequest().body("El campo 'idSocio' es obligatorio");
        }
        if (solicitud.getIdEjemplar() == null) {
            return ResponseEntity.badRequest().body("El campo 'idEjemplar' es obligatorio");
        }

        try {
            Prestamo prestamo = servicioPrestamo.crearPrestamo(
                    Objects.requireNonNull(solicitud.getIdSocio()),
                    Objects.requireNonNull(solicitud.getIdEjemplar()));
            return ResponseEntity.ok(prestamo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/devolver")
    @Operation(summary = "Devolver préstamo", description = "Marca un préstamo como devuelto")
    public ResponseEntity<?> devolverPrestamo(@PathVariable(required = true) Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("ID requerido");
        }

        try {
            // SEGURIDAD: Verificar que el usuario puede devolver este préstamo
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = auth.getName();
            boolean esBibliotecario = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIO")
                            || a.getAuthority().equals("ROLE_ADMIN"));

            servicioPrestamo.devolverPrestamo(id, usuario, esBibliotecario);
            return ResponseEntity.ok("Préstamo devuelto correctamente");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Listar préstamos", description = "Obtiene todos los préstamos, opcionalmente filtrados por estado")
    public List<com.biblioteca.dto.PrestamoDTO> listarPrestamos(@RequestParam(required = false) String estado) {
        return servicioPrestamo.obtenerTodosLosPrestamos(estado).stream()
                .map(com.biblioteca.dto.PrestamoDTO::fromEntity)
                .toList();
    }

    @GetMapping("/mis-prestamos")
    @Operation(summary = "Mis préstamos", description = "Obtiene los préstamos del usuario autenticado")
    public List<com.biblioteca.dto.PrestamoDTO> listarMisPrestamos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();
        return servicioPrestamo.obtenerPrestamosDeUsuario(usuario).stream()
                .map(com.biblioteca.dto.PrestamoDTO::fromEntity)
                .toList();
    }
}
