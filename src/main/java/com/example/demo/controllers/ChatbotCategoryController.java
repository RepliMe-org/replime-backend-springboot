package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotCategoryResponseDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.services.ChatbotCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot/categories")
public class ChatbotCategoryController {
    @Autowired
    private ChatbotCategoryService chatbotCategoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<String> addChatbotCategory(
            @RequestBody List<String> chatbotCategoryNames) {
        chatbotCategoryService.addCategories(chatbotCategoryNames);
        return ResponseEntity.ok("Chatbot category added successfully");
    }

    @GetMapping
    public ResponseEntity<List<ChatbotCategoryResponseDTO>> getAllChatbotCategories() {
        System.out.println("Fetching all chatbot categories...");
        return ResponseEntity.ok(chatbotCategoryService.getAllCategories());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteChatbotCategory(@PathVariable Long id) {
        chatbotCategoryService.deleteCategory(id);
        return ResponseEntity.ok("Chatbot category deleted successfully");
    }

    @GetMapping("{categoryId}/message-classes")
    @PreAuthorize("hasAnyRole('ADMIN','INFLUENCER')")
    @Operation(description = "Get all system message classes for a specific chatbot category.")
    public ResponseEntity<List<MessageClassResponseDTO>> GetAllCategoryMessageClassesForAdmin(
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(
                chatbotCategoryService.getAllClassesInCategory(categoryId)
        );
    }

    @PostMapping("{categoryId}/message-classes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Admin Create new system message classes for a specific category.")
    public ResponseEntity<List<MessageClassResponseDTO>> CreateMessageClassForAdmin(
            @PathVariable Long categoryId,
            @RequestBody List<String> messageClassesNames
    ){
        return ResponseEntity.ok(
                chatbotCategoryService.createMessageClassForCategory(
                        categoryId,messageClassesNames));
    }

    @DeleteMapping("{categoryId}/message-classes/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Admin delete system message classes for a specific category.")
    public ResponseEntity<String> DeleteMessageClassForAdmin(
            @PathVariable Long categoryId,
            @PathVariable Long classId
    ){
        chatbotCategoryService.deleteMessageClassForCategory(categoryId,classId);
        return ResponseEntity.ok("Message class got deleted for a specific category.");
    }

}
