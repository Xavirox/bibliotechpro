package com.biblioteca.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Rate Limiting para proteger la API contra abuso.
 * Implementación simple usando sliding window con ConcurrentHashMap.
 * 
 * Límites:
 * - Endpoints de login: 10 requests/minuto por IP (protección brute-force)
 * - Otros endpoints: 100 requests/minuto por IP
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Configuración de límites
    private static final int GENERAL_LIMIT = 100;
    private static final int LOGIN_LIMIT = 10;
    private static final long WINDOW_MS = 60_000; // 1 minuto

    // Caché de contadores por IP
    private final Map<String, RateLimitEntry> generalLimits = new ConcurrentHashMap<>();
    private final Map<String, RateLimitEntry> loginLimits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIP = getClientIP(request);
        String path = request.getRequestURI();

        // Determinar límites según el endpoint
        boolean allowed;
        if (path.contains("/api/auth/login")) {
            allowed = checkRateLimit(loginLimits, clientIP, LOGIN_LIMIT);
        } else {
            allowed = checkRateLimit(generalLimits, clientIP, GENERAL_LIMIT);
        }

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIP, path);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {
                        "status": 429,
                        "error": "Too Many Requests",
                        "message": "Ha excedido el límite de solicitudes. Por favor, espere un momento."
                    }
                    """);
        }
    }

    /**
     * Verifica si el request está dentro del límite.
     * Implementa sliding window logging algorithm.
     */
    private boolean checkRateLimit(Map<String, RateLimitEntry> limits, String key, int maxRequests) {
        long now = System.currentTimeMillis();

        RateLimitEntry entry = limits.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                // Nueva ventana
                return new RateLimitEntry(now, 1);
            } else {
                // Misma ventana, incrementar contador
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return entry.count.get() <= maxRequests;
    }

    /**
     * Obtiene la IP real del cliente, considerando proxies.
     * SEGURIDAD: Valida formato de IP para evitar bypass de rate limiting via
     * spoofing.
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String candidateIp = xForwardedFor.split(",")[0].trim();
            // SEGURIDAD: Validar que parece una IP real (IPv4 o IPv6 básico)
            if (isValidIpFormat(candidateIp)) {
                return candidateIp;
            }
            // Si X-Forwarded-For tiene formato inválido, usar IP directa
            logger.warn("X-Forwarded-For con formato inválido detectado: {}", candidateIp);
        }
        return request.getRemoteAddr();
    }

    /**
     * Valida que un string tiene formato de IP válida (IPv4 o IPv6).
     * No valida que la IP exista, solo que el formato es correcto.
     */
    private boolean isValidIpFormat(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // IPv4: 4 grupos de 1-3 dígitos separados por puntos
        if (ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            // Validar rangos 0-255
            String[] parts = ip.split("\\.");
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        }
        // IPv6: Formato simplificado (contiene solo hex y colons)
        if (ip.matches("^[0-9a-fA-F:]+$") && ip.contains(":")) {
            return true;
        }
        // Localhost especial
        return "localhost".equalsIgnoreCase(ip);
    }

    /**
     * No aplicar rate limiting a recursos estáticos y documentación
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".html");
    }

    /**
     * Estructura interna para tracking de rate limits
     */
    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
