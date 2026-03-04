package com.example.demo.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotConfigRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Greeting message is required")
    private String greetingMessage;

    @NotBlank(message = "System prompt is required")
    private String systemPrompt;

    @NotNull(message = "Temperature is required")
    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "2.0", message = "Temperature must be at most 2.0")
    private Double temperature;
}

