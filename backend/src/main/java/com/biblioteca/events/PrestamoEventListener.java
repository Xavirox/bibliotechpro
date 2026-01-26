package com.biblioteca.events;

import com.biblioteca.model.Prestamo;
import com.biblioteca.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class PrestamoEventListener {

    private static final Logger log = LoggerFactory.getLogger(PrestamoEventListener.class);
    private final WebhookService webhookService;

    public PrestamoEventListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Async // Ejecutar en hilo separado para no bloquear la transacción original
    @EventListener
    public void handlePrestamoDevuelto(PrestamoDevueltoEvent event) {
        Prestamo prestamo = event.getPrestamo();

        // Verificar si hay retraso
        if (prestamo.getFechaPrevistaDevolucion() != null && new Date().after(prestamo.getFechaPrevistaDevolucion())) {
            long diff = new Date().getTime() - prestamo.getFechaPrevistaDevolucion().getTime();
            long diasRetraso = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

            if (diasRetraso >= 0) {
                log.info("Detectada devolución tardía (ID: {}). Disparando webhook...", prestamo.getIdPrestamo());

                // Construir payload
                Map<String, Object> payload = new HashMap<>();
                payload.put("evento", "DEVOLUCION_TARDIA");
                payload.put("id_prestamo", prestamo.getIdPrestamo());
                payload.put("usuario", prestamo.getSocio().getUsuario());
                payload.put("email", prestamo.getSocio().getEmail());
                payload.put("nombre_socio", prestamo.getSocio().getNombre());
                payload.put("libro", prestamo.getEjemplar().getLibro().getTitulo());
                payload.put("fecha_prevista", prestamo.getFechaPrevistaDevolucion().toString());
                payload.put("fecha_devolucion", new Date().toString());
                payload.put("dias_retraso", diasRetraso == 0 ? 1 : diasRetraso);

                // Enviar
                webhookService.notificarDevolucionTardia(payload);
            }
        }
    }
}
