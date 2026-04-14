package com.example.demo.services;


import com.example.demo.dtos.ChatbotCategoryRequest;
import com.example.demo.dtos.MessageClassRequestDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.exceptions.ResourceConflictException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotCategoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatbotCategoryService {
    @Autowired
    private ChatbotCategoryRepo chatbotCategoryRepo;
    @Autowired
    private MessageClassService messageClassService;

    public void addCategory(ChatbotCategoryRequest chatbotCategoryRequest) {
        if (chatbotCategoryRepo.existsByName(chatbotCategoryRequest.getName())) {
            throw new ResourceConflictException("Chatbot category name already exists");
        }

        ChatbotCategory chatbotCategory = new ChatbotCategory();
        chatbotCategory.setName(chatbotCategoryRequest.getName());

        try {
            chatbotCategoryRepo.save(chatbotCategory);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Chatbot category name already exists", ex);
        }
    }

    public List<ChatbotCategory> getAllCategories() {
        return chatbotCategoryRepo.findAll();
    }

    public void deleteCategory(Long id) {
        chatbotCategoryRepo.deleteById(id);
    }

    public ChatbotCategory getChabotCategoryById(Long id) {
        return chatbotCategoryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "Chatbot category not found with id: " + id
        ));
    }

    public List<MessageClassResponseDTO> getAllClassesInCategory(Long categoryId) {
        ChatbotCategory category = getChabotCategoryById(categoryId);
        return messageClassService.getAllSystemMessageClassesForCategory(categoryId);
    }

    public List<MessageClassResponseDTO> createMessageClassForCategory(
            Long categoryId, List<String> messageClassesNames
    ) {
        ChatbotCategory category = getChabotCategoryById(categoryId);
        return messageClassService.createMessageClassForCategory(
                category,messageClassesNames);
    }
}
