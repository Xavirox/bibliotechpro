package com.biblioteca.exception;

import com.biblioteca.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la aplicación.
 * Proporciona respuestas de error consistentes y estructuradas usando
 * ErrorResponse DTO.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Maneja errores de validación de entrada (@Valid).
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {

                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.toList());

                logger.warn("Validation error on {}: {}", request.getRequestURI(), errors);

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Error",
                                "Los datos de entrada no son válidos").withDetails(errors)
                                .withPath(request.getRequestURI());

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Maneja errores de usuario no encontrado (autenticación).
         */
        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUsernameNotFound(
                        UsernameNotFoundException ex, HttpServletRequest request) {

                logger.warn("User not found: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                "Usuario no encontrado").withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        /**
         * Maneja errores de credenciales incorrectas.
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(
                        BadCredentialsException ex, HttpServletRequest request) {

                logger.warn("Bad credentials attempt on {}", request.getRequestURI());

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                "Credenciales incorrectas").withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        /**
         * Maneja errores de acceso denegado.
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        AccessDeniedException ex, HttpServletRequest request) {

                logger.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                "No tienes permisos para realizar esta acción").withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        /**
         * Maneja errores de seguridad explícitos (lanzados manualmente).
         */
        @ExceptionHandler(SecurityException.class)
        public ResponseEntity<ErrorResponse> handleSecurityException(
                        SecurityException ex, HttpServletRequest request) {

                logger.warn("Security error on {}: {}", request.getRequestURI(), ex.getMessage());

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                ex.getMessage()).withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        /**
         * Maneja argumentos inválidos (recursos no encontrados, IDs inválidos).
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex, HttpServletRequest request) {

                logger.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage()).withPath(request.getRequestURI());

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Maneja estados inválidos (conflictos de negocio).
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalState(
                        IllegalStateException ex, HttpServletRequest request) {

                logger.warn("Illegal state on {}: {}", request.getRequestURI(), ex.getMessage());

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                ex.getMessage()).withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        /**
         * Maneja excepciones de base de datos, específicamente errores de Oracle.
         * Captura RAISE_APPLICATION_ERROR (ORA-20000 a ORA-20999) y los mapea a 400 Bad
         * Request.
         */
        @ExceptionHandler({ DataAccessException.class, SQLException.class })
        public ResponseEntity<ErrorResponse> handleDatabaseExceptions(Exception ex, HttpServletRequest request) {
                Throwable rootCause = ex;
                while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                        rootCause = rootCause.getCause();
                }

                String message = rootCause.getMessage();
                if (message != null && message.contains("ORA-20")) {
                        // Patrón para capturar el mensaje personalizado de ORA-20xxx
                        Pattern pattern = Pattern.compile("ORA-20\\d{3}:\\s*(.*?)(?:\\n|$|ORA-)", Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(message);

                        if (matcher.find()) {
                                String cleanMessage = matcher.group(1).trim();
                                logger.warn("Oracle Business Error on {}: {}", request.getRequestURI(), cleanMessage);

                                ErrorResponse response = ErrorResponse.of(
                                                HttpStatus.BAD_REQUEST.value(),
                                                "Business Error",
                                                cleanMessage).withPath(request.getRequestURI());
                                return ResponseEntity.badRequest().body(response);
                        }
                }

                logger.error("Database error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Database Error",
                                "Error en la base de datos. Por favor contacte al administrador.")
                                .withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        /**
         * Maneja cualquier excepción no capturada específicamente.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex, HttpServletRequest request) {

                logger.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "Ha ocurrido un error inesperado. Por favor, inténtelo más tarde.")
                                .withPath(request.getRequestURI());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}
