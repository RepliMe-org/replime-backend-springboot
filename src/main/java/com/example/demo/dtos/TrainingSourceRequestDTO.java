package com.example.demo.dtos;

import com.example.demo.entities.utils.SourceType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSourceRequestDTO {
    private String sourceValue;
    private SourceType sourceType;
    private Integer last_n;
}
