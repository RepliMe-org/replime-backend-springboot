package com.example.demo.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import com.example.demo.entities.utils.Formality;

@Getter
@Setter
public class ChatbotConfigRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Greeting message is required")
    private String greetingMessage;

    @NotNull(message = "talkLikeMe is required")
    private Boolean talkLikeMe;

    private Tone tone;

    private Verbosity verbosity;

    private Formality formality;
}
