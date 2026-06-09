package com.example.demo.dtos.utils;

import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.utils.MessageStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
public class MessageDto {
    private Long id;
    private String message;
    private MessageSender sender;
    private LocalDateTime sentAt;
    private MessageStatus messageStatus;
    private String messageClass;
    private List<MessageSourceDto> sources;
}
