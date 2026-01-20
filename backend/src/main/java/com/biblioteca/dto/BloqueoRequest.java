package com.biblioteca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para solicitudes de bloqueo/reserva de ejemplares.
 * Incluye validaciones de entrada para seguridad y consistencia de datos.
 */
public class BloqueoRequest {

    @NotNull(message = "El ID del ejemplar es obligatorio")
    @Positive(message = "El ID del ejemplar debe ser positivo")
    private Long idEjemplar;

    // Constructors
    public BloqueoRequest() {
    }

    public BloqueoRequest(Long idEjemplar) {
        this.idEjemplar = idEjemplar;
    }

    // Getters and Setters
    public Long getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(Long idEjemplar) {
        this.idEjemplar = idEjemplar;
    }
}
