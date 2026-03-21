package com.example.demo.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignCategoryRequest {
    @NotNull
    private Long categoryId;
    @NotNull
    private String name;
}
