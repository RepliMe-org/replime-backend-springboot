package com.example.demo.dtos.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotQueryResponseDTO {

    private String answer;

    private List<SourceDTO> sources;

    @JsonProperty("session_title")
    @Nullable
    private String sessionTitle;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceDTO {

        @JsonProperty("video_id")
        private String videoId;

        @JsonProperty("video_title")
        private String videoTitle;

        @JsonProperty("youtube_url")
        private String youtubeUrl;
    }
}