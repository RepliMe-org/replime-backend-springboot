package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends RuntimeException {
    private final HttpStatus status;

    public ResourceConflictException(String message) {
        super(message);
        this.status = HttpStatus.CONFLICT;
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.CONFLICT;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

