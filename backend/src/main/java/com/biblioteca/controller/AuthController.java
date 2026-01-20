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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String JWT_COOKIE_NAME = "jwt_token";
    private static final int COOKIE_MAX_AGE_HOURS = 24;

    private final AuthenticationManager authenticationManager;
    private final SocioRepository socioRepository;
    private final JwtTokenProvider jwtUtils;

    /**
     * SEGURIDAD: Flag para forzar cookies seguras en producción.
     * Por defecto es false para desarrollo local sin HTTPS.
     * En producción, configurar app.cookie.secure=true
     */
    @Value("${app.cookie.secure:false}")
    private boolean forceSecureCookie;

    public AuthController(AuthenticationManager authenticationManager,
            SocioRepository socioRepository,
            JwtTokenProvider jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.socioRepository = socioRepository;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y retorna un token JWT en cookie HttpOnly con SameSite=Strict")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Intento de login - usuario: {}", loginRequest.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateToken(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Socio socio = socioRepository.findByUsuario(userDetails.getUsername()).orElseThrow();

            // SEGURIDAD C-02/C-04: Cookie con HttpOnly, Secure automático, y SameSite
            addJwtCookie(response, jwt, request.isSecure());

            // Log sin exponer rol para evitar fingerprinting
            log.info("Login exitoso - userId: {}", socio.getIdSocio());

            // Return user info without the token (token is in cookie)
            return ResponseEntity.ok(new JwtResponse(null, // No token in body
                    socio.getIdSocio(),
                    socio.getUsuario(),
                    socio.getRol()));
        } catch (Exception e) {
            log.warn("Login fallido - usuario: {}", loginRequest.getUsername());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida la cookie JWT")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Clear the JWT cookie using ResponseCookie for proper SameSite support
        boolean isSecure = forceSecureCookie || request.isSecure();

        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(0) // Delete immediately
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("Logout ejecutado");
        return ResponseEntity.ok().body("{\"message\": \"Sesión cerrada correctamente\"}");
    }

    /**
     * SEGURIDAD: Añade la cookie JWT con todas las protecciones:
     * - HttpOnly: No accesible por JavaScript (protege contra XSS)
     * - Secure: Solo enviada por HTTPS (protege contra MITM)
     * - SameSite=Strict: No enviada en requests cross-site (protege contra CSRF)
     * - Path=/: Disponible para toda la aplicación
     * 
     * @param response        HttpServletResponse donde añadir la cookie
     * @param jwt             Token JWT a almacenar
     * @param requestIsSecure Si la request original fue por HTTPS
     */
    private void addJwtCookie(HttpServletResponse response, String jwt, boolean requestIsSecure) {
        // Secure flag: true si la request es HTTPS O si está forzado por config
        boolean isSecure = forceSecureCookie || requestIsSecure;

        // Usar ResponseCookie de Spring para soporte completo de SameSite
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, jwt)
                .httpOnly(true) // Protección XSS
                .secure(isSecure) // Solo HTTPS en producción
                .path("/")
                .maxAge(java.util.Objects.requireNonNull(Duration.ofHours(COOKIE_MAX_AGE_HOURS)))
                .sameSite("Strict") // Protección CSRF fuerte
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
