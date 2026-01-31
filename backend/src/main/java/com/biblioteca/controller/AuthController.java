package com.biblioteca.controller;

import com.biblioteca.dto.JwtResponse;
import com.biblioteca.dto.LoginRequest;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import com.biblioteca.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.biblioteca.config.AppCookieProperties;
import java.time.Duration;

/**
 * Controlador de autenticación con cookies seguras.
 *
 * SEGURIDAD IMPLEMENTADA:
 * - C-02: JWT en cookie HttpOnly (no accesible por JS)
 * - C-04: SameSite=Strict explícito para prevenir CSRF
 * - Secure flag automático basado en request.isSecure() o config
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "API para autenticación de usuarios")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    private static final String NOMBRE_COOKIE_JWT = "jwt_token";
    private static final int DURACION_COOKIE_HORAS = 24;

    private final AuthenticationManager authenticationManager;
    private final SocioRepository repositorioSocio;
    private final JwtTokenProvider proveedorToken;
    private final AppCookieProperties cookieProperties;

    public AuthController(
            AuthenticationManager authenticationManager,
            SocioRepository repositorioSocio,
            JwtTokenProvider proveedorToken,
            AppCookieProperties cookieProperties) {
        this.authenticationManager = authenticationManager;
        this.repositorioSocio = repositorioSocio;
        this.proveedorToken = proveedorToken;
        this.cookieProperties = cookieProperties;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y retorna un token JWT en cookie HttpOnly con SameSite=Strict")
    public ResponseEntity<?> autenticarUsuario(@Valid @RequestBody LoginRequest solicitudLogin,
            HttpServletRequest peticion,
            HttpServletResponse respuesta) {
        LOG.info("Intento de login - usuario: {}", solicitudLogin.username());

        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            solicitudLogin.username(),
                            solicitudLogin.password()));

            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            String jwt = proveedorToken.generateToken(autenticacion);

            UserDetails detallesUsuario = (UserDetails) autenticacion.getPrincipal();
            Socio socio = repositorioSocio.findByUsuario(detallesUsuario.getUsername()).orElseThrow();

            // SEGURIDAD C-02/C-04: Cookie con HttpOnly, Secure automático, y SameSite
            agregarCookieJwt(respuesta, jwt, peticion.isSecure());

            // Log sin exponer rol para evitar fingerprinting
            LOG.info("Login exitoso - userId: {}", socio.getIdSocio());

            // Retornamos info del usuario sin el token (el token va en la cookie)
            return ResponseEntity.ok(new JwtResponse(null, // Token no va en body
                    socio.getIdSocio(),
                    socio.getUsuario(),
                    socio.getRol()));
        } catch (Exception e) {
            LOG.warn("Login fallido - usuario: {}", solicitudLogin.username());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida la cookie JWT")
    public ResponseEntity<?> cerrarSesion(HttpServletRequest peticion, HttpServletResponse respuesta) {
        // Limpiar la cookie JWT usando ResponseCookie para soporte correcto de SameSite
        boolean esSeguro = cookieProperties.isSecure() || peticion.isSecure();

        ResponseCookie cookie = ResponseCookie.from(NOMBRE_COOKIE_JWT, "")
                .httpOnly(true)
                .secure(esSeguro)
                .path("/")
                .maxAge(0) // Eliminar inmediatamente
                .sameSite("Strict")
                .build();

        respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        LOG.info("Logout ejecutado");
        return ResponseEntity.ok().body("{\"message\": \"Sesión cerrada correctamente\"}");
    }

    /**
     * SEGURIDAD: Añade la cookie JWT con todas las protecciones:
     * - HttpOnly: No accesible por JavaScript (protege contra XSS)
     * - Secure: Solo enviada por HTTPS (protege contra MITM)
     * - SameSite=Strict: No enviada en requests cross-site (protege contra CSRF)
     * - Path=/: Disponible para toda la aplicación
     *
     * @param respuesta        HttpServletResponse donde añadir la cookie
     * @param jwt              Token JWT a almacenar
     * @param peticionEsSegura Si la request original fue por HTTPS
     */
    private void agregarCookieJwt(HttpServletResponse respuesta, String jwt, boolean peticionEsSegura) {
        // Secure flag: true si la request es HTTPS O si está forzado por config
        boolean esSeguro = cookieProperties.isSecure() || peticionEsSegura;

        // Usar ResponseCookie de Spring para soporte completo de SameSite
        ResponseCookie cookie = ResponseCookie.from(NOMBRE_COOKIE_JWT, jwt)
                .httpOnly(true) // Protección XSS
                .secure(esSeguro) // Solo HTTPS en producción
                .path("/")
                .maxAge(java.util.Objects.requireNonNull(Duration.ofHours(DURACION_COOKIE_HORAS)))
                .sameSite("Strict") // Protección CSRF fuerte
                .build();

        respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
