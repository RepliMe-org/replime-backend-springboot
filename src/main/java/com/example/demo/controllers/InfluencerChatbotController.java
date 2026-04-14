package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.dtos.InfluencerChatbotResponseDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.services.ChatbotConfigService;
import com.example.demo.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/influencer/chatbot")
@PreAuthorize("hasRole('INFLUENCER')")
@Tag(name = "Influencer Chatbot", description = "Endpoints for influencer chatbot management")
public class InfluencerChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @GetMapping
    @Operation(description = "Retrieve the influencer's chatbot configuration and details")
    public ResponseEntity<InfluencerChatbotResponseDTO> getInfluencerChatbot(
            @RequestHeader("Authorization") String token
    ) {
        return chatbotService.getInfluencerChatbot(token);
    }

    @PostMapping("/config")
    @Operation(description = "Create and save initial chatbot configuration")
    public ResponseEntity<String> saveChatbotConfig(
            @RequestBody ChatbotConfigRequestDTO requestDTO,
            @RequestHeader("Authorization") String token
    ) {
        return chatbotConfigService.saveChatbotConfig(requestDTO, token);
    }

    @PutMapping("/config")
    @Operation(description = "Update existing chatbot configuration")
    public ResponseEntity<String> updateChatbotConfig(
            @RequestBody ChatbotConfigUpdateDTO requestDTO,
            @RequestHeader("Authorization") String token
    ) {
        return chatbotConfigService.updateChatbotConfig(requestDTO, token);
    }

    @PatchMapping("/category/{categoryId}")
    @Operation(description = "Assign a category to the influencer's chatbot")
    public ResponseEntity<Void> assignCategory(
            @PathVariable Long categoryId,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.assignCategory(categoryId, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/message-classes")
    @Operation(description = "Get all message classes available for the influencer's chatbot category")
    public ResponseEntity<List<MessageClassResponseDTO>> getAllMessageClasses(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(
                chatbotService.getAllMessageClassesAssignedToChatbot(token)
        );
    }

    @PutMapping("/message-classes")
    @Operation(description = "Select and assign message classes from available system classes to the chatbot")
    public ResponseEntity<String> chooseMessageClasses(
            @RequestBody List<Long> messageClassIds,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.chooseMessageClassesForChatbot(messageClassIds, token);
        return ResponseEntity.ok("Message classes assigned to chatbot successfully");
    }

    @PostMapping("/message-classes")
    @Operation(description = "Create custom message classes for the influencer's chatbot")
    public ResponseEntity<String> createCustomMessageClasses(
            @RequestBody List<String> messageClassesNames,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.createMessageClassesForSpecificChatbot(token, messageClassesNames);
        return ResponseEntity.ok("Message classes created and assigned to chatbot successfully");
    }

    @DeleteMapping("/message-classes/{messageClassId}")
    @Operation(description = "Delete a message class from the chatbot. " +
            "If CUSTOM class, delete it permanently. If SYSTEM class, only remove it from this chatbot.")
    public ResponseEntity<String> removeMessageClass(
            @PathVariable Long messageClassId,
            @RequestHeader("Authorization") String token
    ) {
        chatbotService.deleteMessageClassFromChatbot(messageClassId, token);
        return ResponseEntity.ok("Message class removed from chatbot successfully");
    }
}
