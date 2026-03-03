package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotBrowseResponseDTO;
import com.example.demo.dtos.ChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("chatbot")
public class ChatbotController {
    @Autowired
    private ChatbotService chatbotService;

    //for normal users no need to be logged in to see public chatbots
    @GetMapping("/all/public")
    public ResponseEntity<List<ChatbotBrowseResponseDTO>> getAllPublicChatbots() {
        return chatbotService.getPublicChatbots();
    }

    //for admin only
    @GetMapping("/all")
    public ResponseEntity<List<ChatbotResponseDTO>> getAllChatbots(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return chatbotService.getAllChatbots();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatbotResponseDTO> getChatbotById(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return chatbotService.getChatbotById(id);
    }
}
