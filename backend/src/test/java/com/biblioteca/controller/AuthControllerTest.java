package com.biblioteca.controller;

import com.biblioteca.dto.LoginRequest;
import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import com.biblioteca.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Tests")
@SuppressWarnings("null")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthenticationManager authenticationManager;

        @MockitoBean
        private SocioRepository socioRepository;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

        // Security beans for context
        @MockitoBean
        private com.biblioteca.security.JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private com.biblioteca.security.CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private com.biblioteca.config.JwtProperties jwtProperties;

        @MockitoBean
        private com.biblioteca.security.RateLimitingFilter rateLimitingFilter;

        private Socio testSocio;

        @BeforeEach
        void setUp() {
                testSocio = new Socio();
                testSocio.setIdSocio(1L);
                testSocio.setUsuario("testuser");
                testSocio.setNombre("Test User");
                testSocio.setRol("SOCIO");
                testSocio.setPasswordHash("$2a$10$hashedpassword");
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_Success() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("password123");

                User userDetails = new User("testuser", "password",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SOCIO")));

                Authentication auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(auth);
                when(socioRepository.findByUsuario("testuser")).thenReturn(Optional.of(testSocio));
                when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("mock-jwt-token");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(header().exists("Set-Cookie"))
                                .andExpect(header().string("Set-Cookie",
                                                org.hamcrest.Matchers.containsString("jwt_token=mock-jwt-token")))
                                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.rol").value("SOCIO"));
        }

        @Test
        @DisplayName("Should return 401 with invalid credentials")
        void login_BadCredentials() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("wrongpassword");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 with empty username")
        void login_EmptyUsername() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("");
                loginRequest.setPassword("password123");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 with empty password")
        void login_EmptyPassword() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest());
        }
}
