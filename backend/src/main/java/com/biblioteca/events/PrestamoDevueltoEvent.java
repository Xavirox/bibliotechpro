package com.biblioteca.events;

import com.biblioteca.model.Prestamo;
import org.springframework.context.ApplicationEvent;

public class PrestamoDevueltoEvent extends ApplicationEvent {

    private final Prestamo prestamo;

    public PrestamoDevueltoEvent(Object source, Prestamo prestamo) {
        super(source);
        this.prestamo = prestamo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }
}
