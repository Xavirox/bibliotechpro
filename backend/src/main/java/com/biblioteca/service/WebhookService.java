package com.biblioteca.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@lombok.RequiredArgsConstructor
public class WebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookService.class);

    @Value("${n8n.webhook.base-url:http://n8n:5678/webhook/}")
    private String urlBaseN8n;

    private static final String ENDPOINT_DEVOLUCION_TARDE = "webhook-devolucion-tarde-v3";
    private static final String ENDPOINT_NUEVA_RESERVA = "webhook-nueva-reserva-v3";
    private static final String ENDPOINT_NUEVO_PRESTAMO = "webhook-nuevo-prestamo-v3";

    private final ObjectMapper objectMapper;
    private final HttpClient clienteHttp = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void notificarDevolucionTardia(Map<String, Object> datos) {
        enviarAWebhook(datos, construirUrl(ENDPOINT_DEVOLUCION_TARDE));
    }

    public void notificarNuevaReserva(String usuario, String tituloLibro) {
        Map<String, Object> datos = Map.of(
                "tipo", "RESERVA",
                "usuario", usuario,
                "libro", tituloLibro,
                "timestamp", System.currentTimeMillis());
        enviarAWebhook(datos, construirUrl(ENDPOINT_NUEVA_RESERVA));
    }

    public void notificarNuevoPrestamo(String usuario, String tituloLibro) {
        Map<String, Object> datos = Map.of(
                "tipo", "PRESTAMO",
                "usuario", usuario,
                "libro", tituloLibro,
                "timestamp", System.currentTimeMillis());
        enviarAWebhook(datos, construirUrl(ENDPOINT_NUEVO_PRESTAMO));
    }

    private String construirUrl(String endpoint) {
        String base = urlBaseN8n.endsWith("/") ? urlBaseN8n : urlBaseN8n + "/";
        return base + endpoint;
    }

    private void enviarAWebhook(Map<String, Object> datos, String urlDestino) {
        CompletableFuture.runAsync(() -> {
            try {
                if (datos != null && datos.size() > 50) {
                    LOG.warn("Payload de webhook demasiado grande. Rechazado.");
                    return;
                }

                String cuerpoJson = objectMapper.writeValueAsString(datos);
                LOG.info("Enviando webhook a n8n: {}", urlDestino);

                HttpRequest solicitud = HttpRequest.newBuilder()
                        .uri(URI.create(urlDestino))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                        .build();

                clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(respuesta -> {
                            if (respuesta.statusCode() >= 200 && respuesta.statusCode() < 300) {
                                LOG.info("Webhook enviado correctamente a {}. Código: {}", urlDestino,
                                        respuesta.statusCode());
                            } else {
                                LOG.warn("n8n respondió con error en {}: {} - {}", urlDestino, respuesta.statusCode(),
                                        respuesta.body());
                            }
                        })
                        .exceptionally(e -> {
                            LOG.error("Error al conectar con n8n en {}: {}", urlDestino, e.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                LOG.error("Error preparando el webhook para n8n", e);
            }
        });
    }
}
