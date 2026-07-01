package com.example.demo.exceptions;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleVerificationExceptionUsesExceptionStatusAndStandardErrorBody() {
        ResponseEntity<?> response = handler.handleVerificationException(
                new VerificationException("not verified", HttpStatus.FORBIDDEN));

        assertStandardError(response, HttpStatus.FORBIDDEN, "not verified");
    }

    @Test
    void handleAuthenticationExceptionUsesExceptionStatusAndStandardErrorBody() {
        ResponseEntity<?> response = handler.handleAuthenticationException(
                new AuthenticationException("bad credentials", HttpStatus.UNAUTHORIZED));

        assertStandardError(response, HttpStatus.UNAUTHORIZED, "bad credentials");
    }

    @Test
    void handleResourceConflictExceptionReturnsConflict() {
        ResponseEntity<?> response = handler.handleResourceConflictException(
                new ResourceConflictException("duplicate"));

        assertStandardError(response, HttpStatus.CONFLICT, "duplicate");
    }

    @Test
    void handleNotFoundReturnsNotFound() {
        ResponseEntity<?> response = handler.handleNotFound(
                new ResourceNotFoundException("missing"));

        assertStandardError(response, HttpStatus.NOT_FOUND, "missing");
    }

    @Test
    void handleInvalidSourceExceptionReturnsBadRequest() {
        ResponseEntity<?> response = handler.handleInvalidSourceException(
                new InvalidSourceException("bad source"));

        assertStandardError(response, HttpStatus.BAD_REQUEST, "bad source");
    }

    @Test
    void handleInvalidClassificationExceptionReturnsBadRequest() {
        ResponseEntity<?> response = handler.handleInvalidClassificationException(
                new InvalidClassificationException("bad class"));

        assertStandardError(response, HttpStatus.BAD_REQUEST, "bad class");
    }

    @Test
    void handleTrainingSourceExceptionUsesCodeMessageAndStatus() {
        ResponseEntity<?> response = handler.handleTrainingSourceException(
                new TrainingSourceException("NOT_YOURS", "forbidden", HttpStatus.FORBIDDEN));

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("NOT_YOURS", body.get("error"));
        assertEquals("forbidden", body.get("message"));
    }

    @Test
    void handleTooManyRequestsIncludesNextAvailableAt() {
        LocalDateTime nextAvailableAt = LocalDateTime.of(2026, 7, 1, 12, 0);

        ResponseEntity<?> response = handler.handleTooManyRequests(
                new TooManyRequestsException("try later", nextAvailableAt));

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("try later", body.get("error"));
        assertEquals(nextAvailableAt, body.get("nextAvailableAt"));
        assertEquals(false, body.get("success"));
        assertNotNull(body.get("timestamp"));
    }

    private static void assertStandardError(ResponseEntity<?> response, HttpStatus status, String error) {
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(status, response.getStatusCode());
        assertEquals(error, body.get("error"));
        assertFalse((Boolean) body.get("success"));
        assertNotNull(body.get("timestamp"));
    }
}
