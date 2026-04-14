package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotCategoryRequest;
import com.example.demo.dtos.MessageClassRequestDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.ChatbotCategory;
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
            @RequestBody ChatbotCategoryRequest chatbotCategoryRequest) {
        chatbotCategoryService.addCategory(chatbotCategoryRequest);
        return ResponseEntity.ok("Chatbot category added successfully");
    }

    // TODO: add response DTO
    @GetMapping
    public ResponseEntity<List<ChatbotCategory>> getAllChatbotCategories() {
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

}
