package com.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "LIBRO")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_LIBRO")
    private Long idLibro;

    @NotBlank(message = "El ISBN es obligatorio")
    @Pattern(regexp = "^(\\d{10}|\\d{13})$", message = "ISBN debe tener 10 o 13 dígitos")
    @Column(name = "ISBN", unique = true, nullable = false)
    private String isbn;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    @Column(name = "TITULO", nullable = false)
    private String titulo;

    @NotBlank(message = "El autor es obligatorio")
    @Size(max = 150, message = "El autor no puede superar 150 caracteres")
    @Column(name = "AUTOR", nullable = false)
    private String autor;

    @Size(max = 50, message = "La categoría no puede superar 50 caracteres")
    @Column(name = "CATEGORIA")
    private String categoria;

    @Min(value = 1000, message = "El año debe ser posterior al año 1000")
    @Max(value = 2100, message = "El año no puede ser posterior a 2100")
    @Column(name = "ANIO")
    private Integer anio;

    @org.hibernate.annotations.Formula("(SELECT COUNT(*) FROM EJEMPLAR e WHERE e.ID_LIBRO = ID_LIBRO AND e.ESTADO = 'DISPONIBLE')")
    private Long disponibles;

    public Long getDisponibles() {
        return disponibles != null ? disponibles : 0L;
    }
}
