package com.example.demo.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChatbotCategoryRequest {
    @NotNull
    private String name;
}
