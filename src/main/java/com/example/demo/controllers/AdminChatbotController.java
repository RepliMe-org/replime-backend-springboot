package com.example.demo.controllers;

import com.example.demo.dtos.AdminChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/chatbots")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatbotController {
    @Autowired
    private ChatbotService chatbotService;

    @GetMapping
    public ResponseEntity<List<AdminChatbotResponseDTO>> getAllChatbots() {
        return chatbotService.getAllChatbotsForAdmin();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateChatbotStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        return chatbotService.updateChatbotStatus(id, status);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<String> updateChatbotVisibility(
            @PathVariable UUID id,
            @RequestParam boolean isPublic
    ) {
        return chatbotService.updateChatbotVisibility(id, isPublic);
    }


}
