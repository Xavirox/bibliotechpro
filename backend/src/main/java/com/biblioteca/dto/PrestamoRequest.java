package com.biblioteca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para solicitudes de creación de préstamos. (Final)
 * Incluye validaciones de entrada para seguridad y consistencia de datos.
 */
public class PrestamoRequest {

    @NotNull(message = "El ID del socio es obligatorio")
    @Positive(message = "El ID del socio debe ser positivo")
    private Long idSocio;

    @NotNull(message = "El ID del ejemplar es obligatorio")
    @Positive(message = "El ID del ejemplar debe ser positivo")
    private Long idEjemplar;

    // Constructors
    public PrestamoRequest() {
    }

    public PrestamoRequest(Long idSocio, Long idEjemplar) {
        this.idSocio = idSocio;
        this.idEjemplar = idEjemplar;
    }

    // Getters and Setters
    public Long getIdSocio() {
        return idSocio;
    }

    public void setIdSocio(Long idSocio) {
        this.idSocio = idSocio;
    }

    public Long getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(Long idEjemplar) {
        this.idEjemplar = idEjemplar;
    }
}
