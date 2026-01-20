package com.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "LIBRO")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
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

    // Getters and Setters
    public Long getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(Long idLibro) {
        this.idLibro = idLibro;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }
}
