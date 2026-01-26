package com.biblioteca.controller;

import com.biblioteca.dto.RecomendacionDTO;
import com.biblioteca.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionController {

    private static final Logger logger = LoggerFactory.getLogger(RecomendacionController.class);
    private final GeminiService geminiService;

    public RecomendacionController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping("/mias")
    public ResponseEntity<List<RecomendacionDTO>> getMisRecomendaciones() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        logger.info("Solicitando recomendaciones para usuario: {}", username);

        // El servicio ya garantiza que nunca lanza excepción y devuelve fallback si
        // falla la IA
        List<RecomendacionDTO> result = geminiService.getRecomendacionesForUser(username);
        return ResponseEntity.ok(result);
    }
}
