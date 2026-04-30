package com.example.demo.dtos;

import com.example.demo.entities.utils.SyncStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponseDTO {
    private Long sourceId;
    private Long videoId;
    private String youtubeVideoId;
    private String title;
    private String thumbnail;
    private SyncStatus syncStatus;
    private String duration;
}
