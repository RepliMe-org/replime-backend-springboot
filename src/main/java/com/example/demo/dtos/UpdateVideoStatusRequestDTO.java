package com.example.demo.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVideoStatusRequestDTO {
    @NotNull
    private String status;
    private String error;
}
