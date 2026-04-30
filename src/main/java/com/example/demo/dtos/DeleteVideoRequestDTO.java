package com.example.demo.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class DeleteVideoRequestDTO {
    private String youtube_video_id;
    private String chatbot_id;
}
