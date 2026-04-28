package com.example.demo.dtos;

import com.example.demo.entities.utils.SyncStatus;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVideoStatusRequestDTO {
    @NotNull
    private SyncStatus syncStatus;
    @Nullable
    private String errorMessage;
}
