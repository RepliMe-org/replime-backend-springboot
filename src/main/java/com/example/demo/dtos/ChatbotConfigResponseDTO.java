package com.example.demo.dtos;

import com.example.demo.entities.utils.Formality;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import com.example.demo.entities.utils.AvatarSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfigResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String greetingMessage;
    private boolean talkLikeMe;
    private Tone tone;
    private Verbosity verbosity;
    private Formality formality;
    private boolean fetchChannel;
    private boolean fetchYoutubeProfilePicture;
    private Integer avatarNumber;
    private String avatarUrl;
    private AvatarSource avatarSource;
    private LocalDateTime createdAt;
}
