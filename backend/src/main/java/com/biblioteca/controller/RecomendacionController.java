package com.biblioteca.controller;

import com.biblioteca.dto.RecomendacionDTO;
import com.biblioteca.service.AIGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones")
@Tag(name = "Recomendaciones AI", description = "API para obtener recomendaciones personalizadas de lectura")
public class RecomendacionController {

    private static final Logger LOG = LoggerFactory.getLogger(RecomendacionController.class);
    private final AIGenerationService servicioIA;

    public RecomendacionController(AIGenerationService servicioIA) {
        this.servicioIA = servicioIA;
    }

    @GetMapping("/mias")
    @Operation(summary = "Obtener mis recomendaciones", description = "Obtiene recomendaciones basadas en el historial de lectura del usuario (IA o Fallback)")
    public ResponseEntity<List<RecomendacionDTO>> obtenerMisRecomendaciones() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();

        LOG.info("Solicitando recomendaciones para usuario: {}", usuario);

        List<RecomendacionDTO> resultado = servicioIA.obtenerRecomendacionesPorUsuario(usuario);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping
    @Operation(summary = "Obtener recomendación por categorías", description = "Endpoint utilizado por el Bot para obtener una recomendación textual")
    public ResponseEntity<java.util.Map<String, String>> recomendarPorCategorias(
            @RequestBody java.util.Map<String, List<String>> request) {

        List<String> categorias = request.getOrDefault("categorias", java.util.Collections.emptyList());
        String recomendacionTexto = servicioIA.obtenerRecomendacionTexto(categorias);

        return ResponseEntity.ok(java.util.Map.of("recomendacion", recomendacionTexto));
    }
}
