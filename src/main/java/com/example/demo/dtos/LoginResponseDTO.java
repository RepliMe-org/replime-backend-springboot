package com.example.demo.dtos;

import com.example.demo.entities.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponseDTO {
    @NotBlank
    String token;

    @NotBlank
    String username;

    @NotBlank
    Role role;
}
