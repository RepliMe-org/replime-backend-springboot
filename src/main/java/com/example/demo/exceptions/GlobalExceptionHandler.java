package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<?> handleVerificationException(VerificationException ex) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "success", false,
                        "error", ex.getMessage()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "success", false,
                        "error", ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<?> handleResourceConflictException(ResourceConflictException ex) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "success", false,
                        "error", ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "success", false,
                        "error", ex.getMessage())
                );
    }

    @ExceptionHandler(InvalidSourceException.class)
    public ResponseEntity<?> handleInvalidSourceException(InvalidSourceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "success", false,
                        "error", ex.getMessage())
                );
    }

    @ExceptionHandler(TrainingSourceException.class)
    public ResponseEntity<?> handleTrainingSourceException(TrainingSourceException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of(
                        "error", ex.getErrorCode(),
                        "message", ex.getMessage()
                ));
    }
}
