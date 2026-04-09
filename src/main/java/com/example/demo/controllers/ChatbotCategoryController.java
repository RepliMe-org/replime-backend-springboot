package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotCategoryRequest;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.services.ChatbotCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot/category")
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

}
