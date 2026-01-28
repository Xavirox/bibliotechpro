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

    public static LibroDTO fromEntity(com.biblioteca.model.Libro libro) {
        return new LibroDTO(
                libro.getIdLibro(),
                libro.getIsbn(),
                libro.getTitulo(),
                libro.getAutor(),
                libro.getCategoria(),
                libro.getAnio(),
                libro.getDisponibles(),
                libro.getDisponibles() > 0);
    }

    // Maintain backward compatibility if needed, or remove if unused (I'll keep it
    // for safety but deprecate)
    public static LibroDTO fromEntity(com.biblioteca.model.Libro libro, long disponibles) {
        return fromEntity(libro); // Ignore the passed value and use the one from entity, or just use the new
                                  // method
    }
}
