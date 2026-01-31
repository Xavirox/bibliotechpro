package com.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;

@Entity
@Table(name = "SOCIO")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SOCIO")
    private Long idSocio;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    @Column(name = "USUARIO", unique = true, nullable = false)
    private String usuario;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "PASSWORD_HASH", nullable = false)
    private String passwordHash;

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "^(SOCIO|BIBLIOTECARIO|ADMIN)$", message = "Rol debe ser SOCIO, BIBLIOTECARIO o ADMIN")
    @Column(name = "ROL", nullable = false)
    private String rol; // SOCIO, BIBLIOTECARIO, ADMIN

    @Column(name = "PENALIZACION_HASTA")
    @Temporal(TemporalType.TIMESTAMP)
    private Date penalizacionHasta;

    @Min(value = 1, message = "El máximo de préstamos debe ser al menos 1")
    @Max(value = 10, message = "El máximo de préstamos no puede superar 10")
    @Column(name = "MAX_PRESTAMOS_ACTIVOS", nullable = false)
    private Integer maxPrestamosActivos = 1;

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Column(name = "NOMBRE")
    private String nombre;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede superar 100 caracteres")
    @Column(name = "EMAIL")
    private String email;

    // Constructores
    public Socio() {
    }

    // Getters y Setters
    public Long getIdSocio() {
        return idSocio;
    }

    public void setIdSocio(Long idSocio) {
        this.idSocio = idSocio;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Date getPenalizacionHasta() {
        return penalizacionHasta;
    }

    public void setPenalizacionHasta(Date penalizacionHasta) {
        this.penalizacionHasta = penalizacionHasta;
    }

    public Integer getMaxPrestamosActivos() {
        return maxPrestamosActivos;
    }

    public void setMaxPrestamosActivos(Integer maxPrestamosActivos) {
        this.maxPrestamosActivos = maxPrestamosActivos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
