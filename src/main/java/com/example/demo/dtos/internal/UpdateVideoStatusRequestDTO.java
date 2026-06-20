package com.example.demo.dtos.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVideoStatusRequestDTO {
    @NotNull
    private String status;
    private String failedStage;
    private String failureReason;
    private Boolean retryable;
    private Integer attemptsMade;
    private String description;
}
