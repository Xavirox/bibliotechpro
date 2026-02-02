package com.biblioteca.service;

import com.biblioteca.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class TelegramNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final TelegramProperties telegramProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TelegramNotificationService(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
    }

    /**
     * Envía un mensaje directamente a Telegram usando el Bot API.
     */
    public void enviarMensaje(String texto) {
        String token = telegramProperties.getBotToken();
        String chatId = telegramProperties.getAdminChatId();

        if (token == null || token.isEmpty() || chatId == null || chatId.isEmpty()) {
            LOG.warn("Telegram no configurado. Token o ChatID faltantes.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String urlEnviada = String.format(
                        "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                        token,
                        chatId,
                        URLEncoder.encode(texto, StandardCharsets.UTF_8));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlEnviada))
                        .GET()
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(res -> {
                            if (res.statusCode() == 200) {
                                LOG.info("Mensaje enviado a Telegram correctamente.");
                            } else {
                                LOG.warn("Error enviando a Telegram: Status {} - Res: {}", res.statusCode(),
                                        res.body());
                            }
                        })
                        .exceptionally(ex -> {
                            LOG.error("Fallo al conectar con la API de Telegram: {}", ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                LOG.error("Error al preparar mensaje para Telegram", e);
            }
        });
    }
}
