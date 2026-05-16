package com.example.demo.dtos.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSourceDto {
    private String videoId;       // youtubeVideoId
    private String videoTitle;    // title
    private String thumbnailUrl;
    private String youtubeUrl;
}
