package com.example.demo.controllers;

import com.example.demo.dtos.*;
import com.example.demo.entities.ChatbotStatus;
import com.example.demo.services.ChatbotConfigService;
import com.example.demo.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/chatbots/influencer/message-classes")
    @PreAuthorize("hasRole( 'INFLUENCER')")
    @Operation(description = "Get all message classes assigned to the influencer's chatbot category.")
    public ResponseEntity<List<MessageClassResponseDTO>> GetAllCategoryMessageClassesForInfluencer(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(
                chatbotService.getAllMessageClassesAssignedToChatbot(token));
    }

    //TODO: make influencer choose classes that he wants from the system classes
    @PutMapping("/chatbots/influencer/message-classes")
    @PreAuthorize("hasRole( 'INFLUENCER')")
    @Operation(description = "Influencer choose message classes from the system classes assigned to his chatbot category.")
    public ResponseEntity<String> ChooseMessageClassesForChatbot(
            @RequestBody List<Long> messageClassIds,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.chooseMessageClassesForChatbot(messageClassIds, token);
        return ResponseEntity.ok("Message classes assigned to chatbot successfully");
    }

    @PostMapping("chatbots/influencer/message-classes")
    @PreAuthorize("hasRole('INFLUENCER')")
    @Operation(description = "Influencer creates custom message classes to his chatbot category.")
    public ResponseEntity<String> CreateMessageClassForInfluencer(
            @RequestBody List<MessageClassRequestDTO> messageClassesRequestDTO,
            @RequestHeader("Authorization") String token
    ){

        chatbotService.createMessageClassesForSpecificChatbot(
                token,messageClassesRequestDTO);
        return ResponseEntity.ok("Message classes created and assigned to chatbot successfully");
    }

    @DeleteMapping("chatbots/influencer/message-classes/{messageClassId}")
    @PreAuthorize("hasRole('INFLUENCER')")
    @Operation(description = "Influencer deletes a message class from his chatbot category," +
            "if CUSTOM class delete it forever but if it is SYSTEM class just remove it from this chatbot.")
    public ResponseEntity<String> RemoveMessageClassForInfluencer(
            @PathVariable Long messageClassId,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.deleteMessageClassFromChatbot(messageClassId, token);
        return ResponseEntity.ok("Message class deleted from chatbot successfully");
    }
}

