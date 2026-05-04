package com.example.demo.dtos;

import lombok.Getter;
import lombok.Setter;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import com.example.demo.entities.utils.Formality;

@Getter
@Setter
public class ChatbotConfigUpdateDTO {

    private String name;

    private String description;

    private String greetingMessage;

    private Boolean talkLikeMe;

    private Tone tone;

    private Verbosity verbosity;

    private Formality formality;

    private Integer avatarNumber;
}
