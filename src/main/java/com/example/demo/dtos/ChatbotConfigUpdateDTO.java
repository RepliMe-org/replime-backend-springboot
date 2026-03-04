package com.example.demo.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotConfigUpdateDTO {

    private String name;

    private String description;

    private String greetingMessage;

    private String systemPrompt;

    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "2.0", message = "Temperature must be at most 2.0")
    private Double temperature;
}

