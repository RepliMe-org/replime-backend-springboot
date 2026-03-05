package com.example.demo.controllers;

import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("chatbots")
public class PublicChatbotController {
    @Autowired
    private ChatbotService chatbotService;

    @GetMapping
    public ResponseEntity<List<PublicChatbotResponseDTO>> getAllPublicChatbots() {
        return chatbotService.getPublicChatbots();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicChatbotResponseDTO> getChatbotById(
            @PathVariable UUID id
    ) {
        return chatbotService.getChatbotById(id);
    }

//    @PostMapping("/{id}/chat")
//    public ResponseEntity<String> chatWithChatbot(
//            @PathVariable UUID id,
//            @RequestBody String userMessage,
//            @RequestHeader("Authorization") String token
//    ) {
//        return chatbotService.chatWithChatbot(id, userMessage, token);
//    }

}
