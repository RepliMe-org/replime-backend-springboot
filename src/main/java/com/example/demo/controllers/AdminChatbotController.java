package com.example.demo.controllers;

import com.example.demo.dtos.AdminChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/chatbots")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Chatbot Management", description = "Admin endpoints for managing chatbots")
public class AdminChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping
    @Operation(description = "Retrieve all chatbots including non-public ones")
    public ResponseEntity<List<AdminChatbotResponseDTO>> getAllChatbots() {
        return chatbotService.getAllChatbotsForAdmin();
    }

    @PatchMapping("/{id}/visibility")
    @Operation(description = "Update chatbot visibility (public/private)")
    public ResponseEntity<String> updateChatbotVisibility(
            @PathVariable UUID id,
            @RequestParam boolean isPublic
    ) {
        return chatbotService.updateChatbotVisibility(id, isPublic);
    }
}
