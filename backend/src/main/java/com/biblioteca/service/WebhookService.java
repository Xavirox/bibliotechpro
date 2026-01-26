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
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    @Value("${n8n.webhook.base-url:http://n8n:5678/webhook/}")
    private String n8nBaseUrl;

    private static final String ENDPOINT_DEVOLUCION_TARDE = "webhook-devolucion-tarde-v3";
    private static final String ENDPOINT_NUEVA_RESERVA = "webhook-nueva-reserva-v3";
    private static final String ENDPOINT_NUEVO_PRESTAMO = "webhook-nuevo-prestamo-v3";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void notificarDevolucionTardia(Map<String, Object> payload) {
        enviarAWebhook(payload, buildUrl(ENDPOINT_DEVOLUCION_TARDE));
    }

    public void notificarNuevaReserva(String usuario, String tituloLibro) {
        Map<String, Object> payload = Map.of(
                "tipo", "RESERVA",
                "usuario", usuario,
                "libro", tituloLibro,
                "timestamp", System.currentTimeMillis());
        enviarAWebhook(payload, buildUrl(ENDPOINT_NUEVA_RESERVA));
    }

    public void notificarNuevoPrestamo(String usuario, String tituloLibro) {
        Map<String, Object> payload = Map.of(
                "tipo", "PRESTAMO",
                "usuario", usuario,
                "libro", tituloLibro,
                "timestamp", System.currentTimeMillis());
        enviarAWebhook(payload, buildUrl(ENDPOINT_NUEVO_PRESTAMO));
    }

    private String buildUrl(String endpoint) {
        String base = n8nBaseUrl.endsWith("/") ? n8nBaseUrl : n8nBaseUrl + "/";
        return base + endpoint;
    }

    private void enviarAWebhook(Map<String, Object> payload, String targetUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                if (payload != null && payload.size() > 50) {
                    log.warn("Payload de webhook demasiado grande. Rechazado.");
                    return;
                }

                String jsonBody = objectMapper.writeValueAsString(payload);
                log.info("Enviando webhook a n8n: {}", targetUrl);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                log.info("Webhook enviado correctamente a {}. Código: {}", targetUrl,
                                        response.statusCode());
                            } else {
                                log.warn("n8n respondió con error en {}: {} - {}", targetUrl, response.statusCode(),
                                        response.body());
                            }
                        })
                        .exceptionally(e -> {
                            log.error("Error al conectar con n8n en {}: {}", targetUrl, e.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                log.error("Error preparando el webhook para n8n", e);
            }
        });
    }
}
