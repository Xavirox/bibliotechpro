package com.biblioteca.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/libros/**").permitAll() // Catalog is public
                        // SEGURIDAD: Endpoint de usuarios para login - solo en desarrollo
                        // En producción, usar login con campo de texto
                        // SEGURIDAD: Endpoint de usuarios para login - solo en desarrollo
                        // En producción, usar login con campo de texto.
                        .requestMatchers("/api/socios/public").permitAll()
                        // SEGURIDAD: Endpoint de debug solo accesible para administradores
                        .requestMatchers("/api/socios/fix-consistency").hasRole("ADMIN")
                        // SEGURIDAD H-01: Swagger/OpenAPI
                        // ⚠️ PRODUCCIÓN: Restringido a administradores para evitar reconocimiento
                        .requestMatchers("/swagger-ui/**").hasRole("ADMIN")
                        .requestMatchers("/swagger-ui.html").hasRole("ADMIN")
                        .requestMatchers("/v3/api-docs/**").hasRole("ADMIN")
                        // SEGURIDAD: Actuator - solo health es público, resto requiere ADMIN
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ejemplares/**").permitAll()
                        .requestMatchers("/api/ejemplares/**").authenticated()
                        // SEGURIDAD H-03: Lista de bloqueos activos solo para bibliotecarios
                        .requestMatchers(HttpMethod.GET, "/api/bloqueos/activos").hasRole("BIBLIOTECARIO")
                        // SEGURIDAD: Endpoint de mantenimiento manual
                        .requestMatchers(HttpMethod.POST, "/api/bloqueos/cleanup").hasRole("ADMIN")
                        .requestMatchers("/api/bloqueos/**").hasAnyRole("SOCIO", "BIBLIOTECARIO")
                        .requestMatchers("/api/prestamos/mis-prestamos").hasRole("SOCIO")
                        .requestMatchers("/api/prestamos/*/devolver").hasAnyRole("SOCIO", "BIBLIOTECARIO")
                        .requestMatchers("/api/prestamos/**").hasRole("BIBLIOTECARIO")
                        .requestMatchers("/api/recomendaciones/**").hasRole("SOCIO")
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // SEGURIDAD: Solo permitir orígenes de desarrollo conocidos
        // En producción, usar variable de entorno para configurar el dominio
        configuration.setAllowedOrigins(java.util.Arrays.asList(
                "http://localhost:5500", // VS Code Live Server
                "http://127.0.0.1:5500", // VS Code Live Server (IP)
                "http://localhost:3000", // React/Vite dev server
                "http://localhost:8000", // Python http.server (abrir_proyecto.bat)
                "http://localhost:8080", // Alternativo
                "http://127.0.0.1:8080",
                "http://asir.javiergimenez.es:9142", // VPS Frontend Port
                "http://asir.javiergimenez.es")); // VPS Public Host
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight por 1 hora
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
