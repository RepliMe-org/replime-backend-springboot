package com.example.demo.services;


import com.example.demo.dtos.ChatbotCategoryResponseDTO;
import com.example.demo.dtos.ChatbotCategoryRequest;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.exceptions.ResourceConflictException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotCategoryRepo;
import com.example.demo.repos.ChatbotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatbotCategoryService {
    @Autowired
    private ChatbotCategoryRepo chatbotCategoryRepo;
    @Autowired
    private MessageClassService messageClassService;
    @Autowired
    private ChatbotRepo chatbotRepo;

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

    public List<ChatbotCategoryResponseDTO> getAllCategories() {
        List<ChatbotCategory> chatbotCategories = chatbotCategoryRepo.findAll();
        List<ChatbotCategoryResponseDTO> chatbotCategoryDTOs = new ArrayList<>();
        for (ChatbotCategory category : chatbotCategories) {
            ChatbotCategoryResponseDTO categoryDTO = ChatbotCategoryResponseDTO.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .build();
            chatbotCategoryDTOs.add(categoryDTO);
        }
        return chatbotCategoryDTOs;
    }

    public void deleteCategory(Long id) {
        if (chatbotRepo.existsByCategoryId(id)) {
            throw new ResourceConflictException("Cannot delete category as it is currently assigned to a chatbot");
        }
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
