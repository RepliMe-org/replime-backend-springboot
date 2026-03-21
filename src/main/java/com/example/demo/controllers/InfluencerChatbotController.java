package com.example.demo.controllers;

import com.example.demo.dtos.AssignCategoryRequest;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.dtos.InfluencerChatbotResponseDTO;
import com.example.demo.entities.ChatbotStatus;
import com.example.demo.services.ChatbotConfigService;
import com.example.demo.services.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/influencer/chatbot")
@PreAuthorize("hasRole('INFLUENCER')")
public class InfluencerChatbotController {
    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping
    ResponseEntity<InfluencerChatbotResponseDTO> getChatbotConfigById(
            @RequestHeader("Authorization") String token) {
        return chatbotService.getInfluencerChatbot(token);
    }

    @PostMapping("config")
    ResponseEntity<String> saveChatbotConfig(
            @RequestBody ChatbotConfigRequestDTO requestDTO,
            @RequestHeader("Authorization") String token) {
        return chatbotConfigService.saveChatbotConfig(requestDTO, token);
    }

    @PutMapping("config")
    ResponseEntity<String> editChatbotConfig(
            @RequestBody ChatbotConfigUpdateDTO requestDTO,
            @RequestHeader("Authorization") String token) {
        return chatbotConfigService.updateChatbotConfig(requestDTO,token);
    }

    @GetMapping("status")
    ResponseEntity<ChatbotStatus> getChatbotStatus(
            @RequestHeader("Authorization") String token
    ){
        return chatbotService.getChatbotStatus(token);
    }

    @PatchMapping("/category/{categoryId}")
    public ResponseEntity<Void> assignCategory(
            @RequestParam Long categoryId,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.assignCategory(categoryId,token);
        return ResponseEntity.noContent().build();
    }
}
