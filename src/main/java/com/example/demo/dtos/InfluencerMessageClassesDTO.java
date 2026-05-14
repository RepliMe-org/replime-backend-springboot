package com.example.demo.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class InfluencerMessageClassesDTO {
    private ChatbotCategoryResponseDTO category;
    private List<MessageClassResponseDTO> pickedClasses;
    private List<MessageClassResponseDTO> customClasses;
    private List<MessageClassResponseDTO> availableClasses;
}

