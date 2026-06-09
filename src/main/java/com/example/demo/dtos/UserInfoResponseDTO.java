package com.example.demo.dtos;

import com.example.demo.entities.utils.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserInfoResponseDTO {
    private String username;
    private String email;
    private Role role;
    private String chatbotName;
    private long conversationsCount;
}
