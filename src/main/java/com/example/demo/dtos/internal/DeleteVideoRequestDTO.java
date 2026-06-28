package com.example.demo.dtos.internal;

import lombok.Builder;

@Builder
public class DeleteVideoRequestDTO {
    private String videoId;
    private String description;
}
