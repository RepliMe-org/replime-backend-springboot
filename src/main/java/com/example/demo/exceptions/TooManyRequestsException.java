package com.example.demo.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TooManyRequestsException extends RuntimeException {
    private final LocalDateTime nextAvailableAt;

    public TooManyRequestsException(String message, LocalDateTime nextAvailableAt) {
        super(message);
        this.nextAvailableAt = nextAvailableAt;
    }
}
