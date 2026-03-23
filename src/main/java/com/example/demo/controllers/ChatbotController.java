package com.example.demo.controllers;

import com.example.demo.dtos.AdminChatbotResponseDTO;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.dtos.InfluencerChatbotResponseDTO;
import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.entities.ChatbotStatus;
import com.example.demo.services.ChatbotConfigService;
import com.example.demo.services.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class ChatbotController {
    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @GetMapping("/chatbots")
    public ResponseEntity<List<PublicChatbotResponseDTO>> getAllPublicChatbots() {
        return chatbotService.getPublicChatbots();
    }

    @GetMapping("/chatbots/{id}")
    public ResponseEntity<PublicChatbotResponseDTO> getChatbotById(
            @PathVariable UUID id
    ) {
        return chatbotService.getChatbotById(id);
    }

    @GetMapping("/admin/chatbots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminChatbotResponseDTO>> getAllChatbots() {
        return chatbotService.getAllChatbotsForAdmin();
    }

    @PatchMapping("/admin/chatbots/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateChatbotStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        return chatbotService.updateChatbotStatus(id, status);
    }

    @PatchMapping("/admin/chatbots/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateChatbotVisibility(
            @PathVariable UUID id,
            @RequestParam boolean isPublic
    ) {
        return chatbotService.updateChatbotVisibility(id, isPublic);
    }

    @GetMapping("/influencer/chatbot")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<InfluencerChatbotResponseDTO> getChatbotConfigById(
            @RequestHeader("Authorization") String token
    ) {
        return chatbotService.getInfluencerChatbot(token);
    }

    @PostMapping("/influencer/chatbot/config")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<String> saveChatbotConfig(
            @RequestBody ChatbotConfigRequestDTO requestDTO,
            @RequestHeader("Authorization") String token
    ) {
        return chatbotConfigService.saveChatbotConfig(requestDTO, token);
    }

    @PutMapping("/influencer/chatbot/config")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<String> editChatbotConfig(
            @RequestBody ChatbotConfigUpdateDTO requestDTO,
            @RequestHeader("Authorization") String token
    ) {
        return chatbotConfigService.updateChatbotConfig(requestDTO, token);
    }

    @GetMapping("/influencer/chatbot/status")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<ChatbotStatus> getChatbotStatus(
            @RequestHeader("Authorization") String token
    ) {
        return chatbotService.getChatbotStatus(token);
    }

    @PatchMapping("/influencer/chatbot/category/{categoryId}")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<Void> assignCategory(
            @PathVariable Long categoryId,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.assignCategory(categoryId, token);
        return ResponseEntity.noContent().build();
    }
}

