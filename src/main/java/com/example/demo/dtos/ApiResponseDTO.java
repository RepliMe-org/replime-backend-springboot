package com.example.demo.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ApiResponseDTO {
    private boolean success;
    private String message;
}
