package com.biblioteca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para solicitudes de bloqueo/reserva de ejemplares.
 * Incluye validaciones de entrada para seguridad y consistencia de datos.
 */
public record BloqueoRequest(
        @NotNull(message = "El ID del ejemplar es obligatorio") @Positive(message = "El ID del ejemplar debe ser positivo") Long idEjemplar) {
}
