package com.biblioteca.exception;

import com.biblioteca.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/test-endpoint");
    }

    @Test
    void handleDatabaseExceptions_ShouldReturnBadRequest_WhenOra20000ErrorOccurs() {
        // Arrange
        String oracleErrorMessage = "ORA-20001: El libro no está disponible\nORA-06512: at \"BILIOTECA.PKG_PRESTAMOS\", line 15";
        SQLException sqlException = new SQLException(oracleErrorMessage);
        Exception exception = new Exception("Wrapper exception", sqlException);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDatabaseExceptions(exception, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Business Error", body.getError());
        assertEquals("El libro no está disponible", body.getMessage());
        assertEquals("/test-endpoint", body.getPath());
    }

    @Test
    void handleDatabaseExceptions_ShouldReturnInternalServerError_WhenGenericDatabaseErrorOccurs() {
        // Arrange
        String genericErrorMessage = "Connection reset";
        SQLException sqlException = new SQLException(genericErrorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDatabaseExceptions(sqlException, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Database Error", body.getError());
        assertEquals("Error en la base de datos. Por favor contacte al administrador.", body.getMessage());
    }

    @Test
    void handleDatabaseExceptions_ShouldHandleDeeplyNestedExceptions() {
        // Arrange
        String oracleErrorMessage = "ORA-20002: Usuario bloqueado";
        SQLException rootCause = new SQLException(oracleErrorMessage);
        RuntimeException wrapper1 = new RuntimeException(rootCause);
        RuntimeException wrapper2 = new RuntimeException(wrapper1);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDatabaseExceptions(wrapper2, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Usuario bloqueado", body.getMessage());
    }
}
