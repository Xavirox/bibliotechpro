package com.biblioteca.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {

        private final TelegramNotificationService telegramService;

        public NotificationService(TelegramNotificationService telegramService) {
                this.telegramService = telegramService;
        }

        public void notificarDevolucionTardia(Map<String, Object> datos) {
                String mensaje = String.format(
                                "⚠️ *Devolución Tardía*\n\n" +
                                                "👤 *Usuario:* %s\n" +
                                                "📘 *Libro:* %s\n" +
                                                "📅 *Prevista:* %s\n" +
                                                "🛑 *Retraso:* %s días",
                                datos.getOrDefault("usuario", "N/A"),
                                datos.getOrDefault("libro", "N/A"),
                                datos.getOrDefault("fecha_prevista", "N/A"),
                                datos.getOrDefault("dias_retraso", "0"));
                telegramService.enviarMensaje(mensaje);
        }

        public void notificarNuevaReserva(String usuario, String tituloLibro) {
                String mensaje = String.format(
                                "📚 *Nueva Reserva*\n\n" +
                                                "👤 *Usuario:* %s\n" +
                                                "📖 *Libro:* %s\n" +
                                                "⏰ *Expira:* 24h",
                                usuario, tituloLibro);
                telegramService.enviarMensaje(mensaje);
        }

        public void notificarNuevoPrestamo(String usuario, String tituloLibro) {
                String mensaje = String.format(
                                "📖 *Nuevo Préstamo*\n\n" +
                                                "👤 *Usuario:* %s\n" +
                                                "📘 *Libro:* %s",
                                usuario, tituloLibro);
                telegramService.enviarMensaje(mensaje);
        }
}
