package com.biblioteca.dto;

import java.util.Date;

public record PrestamoDTO(
        Long idPrestamo,
        String tituloLibro,
        String usuario,
        Date fechaPrestamo,
        Date fechaPrevistaDevolucion,
        Date fechaDevolucionReal,
        String estado,
        boolean estaVencido,
        long diasRestantes,
        String badgeClass) {

    public static PrestamoDTO fromEntity(com.biblioteca.model.Prestamo p) {
        Date now = new Date();
        long diff = p.getFechaPrevistaDevolucion().getTime() - now.getTime();
        long days = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
        boolean vencido = days < 0 && com.biblioteca.model.EstadoPrestamo.ACTIVO == p.getEstado();

        String badge = "badge-success";
        if (vencido)
            badge = "badge-danger";
        else if (days < 3 && com.biblioteca.model.EstadoPrestamo.ACTIVO == p.getEstado())
            badge = "badge-warning";
        else if (com.biblioteca.model.EstadoPrestamo.DEVUELTO == p.getEstado())
            badge = "badge-info";

        return new PrestamoDTO(
                p.getIdPrestamo(),
                p.getEjemplar().getLibro().getTitulo(),
                p.getSocio().getUsuario(),
                p.getFechaPrestamo(),
                p.getFechaPrevistaDevolucion(),
                p.getFechaDevolucionReal(),
                p.getEstado().name(),
                vencido,
                days,
                badge);
    }
}
