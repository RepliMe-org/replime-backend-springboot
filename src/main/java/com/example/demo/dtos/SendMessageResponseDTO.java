package com.example.demo.dtos;


import com.example.demo.dtos.utils.MessageDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
public class SendMessageResponseDTO {
    private Long sessionId;

    private String sessionTitle;

    private MessageDto userMessage;

    private MessageDto aiResponse;

    private LocalDateTime updatedAt;

}
