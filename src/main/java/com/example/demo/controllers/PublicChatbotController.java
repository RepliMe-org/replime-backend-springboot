package com.example.demo.controllers;

import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chatbots")
@Tag(name = "Public Chatbot", description = "Public chatbot endpoints accessible without authentication")
public class PublicChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping
    @Operation(description = "Retrieve all public chatbots")
    public ResponseEntity<List<PublicChatbotResponseDTO>> getAllPublicChatbots() {
        return chatbotService.getPublicChatbots();
    }

    @GetMapping("/{id}")
    @Operation(description = "Retrieve a specific chatbot by ID")
    public ResponseEntity<PublicChatbotResponseDTO> getChatbotById(
            @PathVariable UUID id
    ) {
        return chatbotService.getChatbotById(id);
    }
}
