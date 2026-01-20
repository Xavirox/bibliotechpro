package com.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "EJEMPLAR")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Ejemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EJEMPLAR")
    private Long idEjemplar;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_LIBRO", nullable = false)
    private Libro libro;

    @Column(name = "CODIGO_BARRAS", unique = true, nullable = false)
    private String codigoBarras;

    @Column(name = "ESTADO", nullable = false)
    private String estado; // DISPONIBLE, BLOQUEADO, PRESTADO, BAJA

    @Column(name = "UBICACION")
    private String ubicacion;

    @Version
    private Long version;

    // Getters and Setters
    public Long getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(Long idEjemplar) {
        this.idEjemplar = idEjemplar;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}
