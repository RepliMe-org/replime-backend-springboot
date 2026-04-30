package com.example.demo.dtos.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoIndexRequestDTO {

    @JsonProperty("chatbot_id")
    private String chatbotId;

    @JsonProperty("videos")
    private List<VideoItem> videos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoItem {
        @JsonProperty("youtube_video_id")
        private String youtubeVideoId;

        @JsonProperty("video_title")
        private String videoTitle;
    }
}

