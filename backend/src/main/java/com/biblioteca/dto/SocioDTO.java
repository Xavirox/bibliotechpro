package com.biblioteca.dto;

public class SocioDTO {
    private String usuario;
    private String nombre;
    private String email;
    private String rol;
    private Integer maxPrestamosActivos;

    // Constructor vacío
    public SocioDTO() {
    }

    // Constructor completo
    public SocioDTO(String usuario, String nombre, String email, String rol, Integer maxPrestamosActivos) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.maxPrestamosActivos = maxPrestamosActivos;
    }

    // Getters y Setters
    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Integer getMaxPrestamosActivos() {
        return maxPrestamosActivos;
    }

    public void setMaxPrestamosActivos(Integer maxPrestamosActivos) {
        this.maxPrestamosActivos = maxPrestamosActivos;
    }
}
