package com.biblioteca.controller;

import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import com.biblioteca.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionController {

    private static final Logger logger = LoggerFactory.getLogger(RecomendacionController.class);

    @Autowired
    GeminiService geminiService;

    @Autowired
    SocioRepository socioRepository;

    @GetMapping("/mias")
    public ResponseEntity<String> getMisRecomendaciones() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            logger.info("Generating recommendations for user: {}", username);

            Socio socio = socioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String result = geminiService.getRecomendaciones(socio.getIdSocio());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error generating recommendations", e);
            return ResponseEntity.ok("[{\"titulo\": \"Error\", \"razon\": \"" +
                    e.getMessage().replace("\"", "'") + "\"}]");
        }
    }
}
