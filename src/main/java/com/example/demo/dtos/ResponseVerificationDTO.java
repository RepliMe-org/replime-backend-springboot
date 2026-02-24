package com.example.demo.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ResponseVerificationDTO {
    private String message;
    private String verificationToken;
    private LocalDateTime expirationDateAt;
}
