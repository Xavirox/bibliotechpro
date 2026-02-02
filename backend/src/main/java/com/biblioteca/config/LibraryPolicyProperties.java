package com.biblioteca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.policy")
public class LibraryPolicyProperties {

    /**
     * Duración estándar de un préstamo en días.
     */
    private long prestamoDias = 15;

    /**
     * Tiempo de validez de una reserva (bloqueo) en horas.
     */
    private int reservaHoras = 24;

    public long getPrestamoDias() {
        return prestamoDias;
    }

    public void setPrestamoDias(long prestamoDias) {
        this.prestamoDias = prestamoDias;
    }

    public int getReservaHoras() {
        return reservaHoras;
    }

    public void setReservaHoras(int reservaHoras) {
        this.reservaHoras = reservaHoras;
    }
}
