package com.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "PRESTAMO")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PRESTAMO")
    private Long idPrestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SOCIO", nullable = false)
    private Socio socio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_EJEMPLAR", nullable = false)
    private Ejemplar ejemplar;

    @Column(name = "FECHA_PRESTAMO", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fechaPrestamo;

    @Column(name = "FECHA_PREVISTA_DEVOLUCION", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fechaPrevistaDevolucion;

    @Column(name = "FECHA_DEVOLUCION_REAL")
    @Temporal(TemporalType.DATE)
    private Date fechaDevolucionReal;

    @Column(name = "ESTADO", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoPrestamo estado;

    @OneToOne
    @JoinColumn(name = "ID_BLOQUEO")
    private Bloqueo bloqueo;

    // Constructores
    public Prestamo() {
    }

    public Prestamo(Socio socio, Ejemplar ejemplar, EstadoPrestamo estado, Date fechaPrestamo,
            Date fechaPrevistaDevolucion) {
        this.socio = socio;
        this.ejemplar = ejemplar;
        this.estado = estado;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaPrevistaDevolucion = fechaPrevistaDevolucion;
    }

    // Getters y Setters
    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public Ejemplar getEjemplar() {
        return ejemplar;
    }

    public void setEjemplar(Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
    }

    public Date getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(Date fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public Date getFechaPrevistaDevolucion() {
        return fechaPrevistaDevolucion;
    }

    public void setFechaPrevistaDevolucion(Date fechaPrevistaDevolucion) {
        this.fechaPrevistaDevolucion = fechaPrevistaDevolucion;
    }

    public Date getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(Date fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public Bloqueo getBloqueo() {
        return bloqueo;
    }

    public void setBloqueo(Bloqueo bloqueo) {
        this.bloqueo = bloqueo;
    }
}
