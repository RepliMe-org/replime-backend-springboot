package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class TrainingSourceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public TrainingSourceException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
