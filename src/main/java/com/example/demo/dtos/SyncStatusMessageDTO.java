package com.example.demo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusMessageDTO {
    private String type; // e.g. "VIDEO_UPDATE", "SOURCE_COMPLETE"
    private Long sourceId;
    private Long videoId;
    private String status;
}

