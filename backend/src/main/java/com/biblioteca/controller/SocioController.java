package com.biblioteca.controller;

import com.biblioteca.repository.SocioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/socios")
@Tag(name = "Socios", description = "API para gestión de socios y perfiles")
public class SocioController {

    private final SocioRepository socioRepository;

    public SocioController(SocioRepository socioRepository) {
        this.socioRepository = socioRepository;
    }

    /**
     * ADVERTENCIA DE SEGURIDAD: Este endpoint expone la lista de usuarios.
     * Solo debería usarse en entornos de desarrollo/demo.
     * En producción, usar un formulario de login con campo de texto libre.
     */
    @GetMapping("/public")
    @Operation(summary = "Listar socios (público)", description = "⚠️ SOLO DESARROLLO: Endpoint para facilitar el login en modo demo. Debe estar deshabilitado o restringido en producción.")
    public List<Map<String, String>> getAllSociosPublic() {
        // SEGURIDAD: No exponer el rol para evitar identificar cuentas de alto
        // privilegio
        return socioRepository.findAll().stream()
                .map(s -> Map.of(
                        "username", s.getUsuario(),
                        "nombre", s.getNombre() != null ? s.getNombre() : s.getUsuario()))
                .collect(Collectors.toList());
    }
}
