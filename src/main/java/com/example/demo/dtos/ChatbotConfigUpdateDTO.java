package com.example.demo.dtos;

import lombok.Getter;
import lombok.Setter;
import com.example.demo.entities.Tone;
import com.example.demo.entities.Verbosity;
import com.example.demo.entities.Formality;

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
}
