package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends RuntimeException {
    private final HttpStatus status;

    public AuthenticationException(String message) {
        super(message);
        if (message == "Email already exists"){
            this.status = HttpStatus.CONFLICT;
        }else
            this.status = HttpStatus.UNAUTHORIZED;
    }

    public AuthenticationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
