package com.example.demo.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class DeleteVideoRequestDTO {
    @JsonProperty("youtube_video_id")
    private String youtubeVideoId;

    @JsonProperty("chatbot_id")
    private String chatbotId;
}
