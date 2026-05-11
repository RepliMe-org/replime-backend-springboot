package com.example.demo.dtos.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Setter
@Getter
public class CursorData {
    private LocalDateTime time;
    private Long id;
}
