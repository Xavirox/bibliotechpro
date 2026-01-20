package com.biblioteca.dto;

public class LoginRequest {
    @jakarta.validation.constraints.NotBlank(message = "El usuario es obligatorio")
    private String username;

    @jakarta.validation.constraints.NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
