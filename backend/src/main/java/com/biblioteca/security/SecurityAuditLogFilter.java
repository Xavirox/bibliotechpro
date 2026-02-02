package com.biblioteca.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SEGURIDAD S-01: Filtro de Auditoría para el log de seguridad.
 * Registra accesos a zonas sensibles y ayuda a detectar ataques de fuerza bruta
 * o escaneo.
 */
@Component
public class SecurityAuditLogFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityAuditLogFilter.class);

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();

        // Solo auditamos POST/PUT/DELETE o accesos a /api/auth e /actuator
        boolean pathSensible = path.contains("/api/auth") || path.contains("/actuator") || path.contains("/api/socios");
        boolean operacionMutante = !"GET".equalsIgnoreCase(method);

        if (pathSensible || operacionMutante) {
            filterChain.doFilter(request, response);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = (auth != null) ? auth.getName() : "ANONYMOUS";
            int status = response.getStatus();

            if (status >= 400) {
                LOG.warn("[AUDIT] [ALERTA] Usuario: {} | IP: {} | Metodo: {} | Path: {} | Status: {}",
                        usuario, ip, method, path, status);
            } else if (path.contains("/api/auth/login") && status == 200) {
                LOG.info("[AUDIT] [LOGIN] Usuario: {} | IP: {} | Status: OK", usuario, ip);
            } else {
                LOG.info("[AUDIT] [ACCESO] Usuario: {} | IP: {} | Metodo: {} | Path: {} | Status: {}",
                        usuario, ip, method, path, status);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
