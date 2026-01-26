package com.biblioteca.dto;

public record LibroDTO(
        Long id,
        String isbn,
        String titulo,
        String autor,
        String categoria,
        Integer anio,
        long copiasDisponibles,
        boolean estaDisponible) {

    public static LibroDTO fromEntity(com.biblioteca.model.Libro libro, long disponibles) {
        return new LibroDTO(
                libro.getIdLibro(),
                libro.getIsbn(),
                libro.getTitulo(),
                libro.getAutor(),
                libro.getCategoria(),
                libro.getAnio(),
                disponibles,
                disponibles > 0);
    }
}
