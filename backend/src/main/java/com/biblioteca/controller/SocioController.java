package com.biblioteca.controller;

import com.biblioteca.service.SocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/socios")
@Tag(name = "Socios", description = "API para gestión de socios y perfiles")
public class SocioController {

    private final SocioService socioService;

    public SocioController(SocioService socioService) {
        this.socioService = socioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener mi perfil", description = "Retorna la información del socio autenticado")
    public ResponseEntity<com.biblioteca.dto.SocioDTO> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        return socioService.findByUsuario(username)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private com.biblioteca.dto.SocioDTO convertToDTO(com.biblioteca.model.Socio socio) {
        return new com.biblioteca.dto.SocioDTO(
                socio.getUsuario(),
                socio.getNombre(),
                socio.getEmail(),
                socio.getRol(),
                socio.getMaxPrestamosActivos());
    }

    /**
     * ADVERTENCIA DE SEGURIDAD: Este endpoint expone la lista de usuarios.
     * Solo debería usarse en entornos de desarrollo/demo.
     */
    @GetMapping("/public")
    @Operation(summary = "Listar socios (público)", description = "⚠️ SOLO DESARROLLO: Endpoint para facilitar el login en modo demo.")
    public List<Map<String, String>> getAllSociosPublic() {
        return socioService.getAllSocios().stream()
                .map(s -> Map.of(
                        "username", s.getUsuario(),
                        "nombre", s.getNombre() != null ? s.getNombre() : s.getUsuario()))
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/penalizar")
    @Operation(summary = "Penalizar socio", description = "Añade una penalización temporal al socio (solo bibliotecarios)")
    public ResponseEntity<?> penalizarSocio(@PathVariable @org.springframework.lang.NonNull Long id,
            @RequestParam(defaultValue = "7") int dias) {
        try {
            socioService.penalizarSocio(id, dias);
            return ResponseEntity.ok("Socio penalizado correctamente por " + dias + " días");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
