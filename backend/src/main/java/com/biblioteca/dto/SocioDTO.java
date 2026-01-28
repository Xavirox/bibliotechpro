package com.biblioteca.dto;

public record SocioDTO(
        String usuario,
        String nombre,
        String email,
        String rol,
        Integer maxPrestamosActivos) {
}
