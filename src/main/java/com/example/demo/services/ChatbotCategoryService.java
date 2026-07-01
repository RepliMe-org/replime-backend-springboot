package com.example.demo.services;


import com.example.demo.dtos.ChatbotCategoryResponseDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.exceptions.ResourceConflictException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotCategoryRepo;
import com.example.demo.repos.ChatbotRepo;
import jakarta.transaction.Transactional;
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
    @Transactional
    public void addCategories(List<String> chatbotCategoryNames) {
        for (String categoryName : chatbotCategoryNames) {
            addCategory(categoryName);
        }
    }
    public void addCategory(String categoryName) {
        ChatbotCategory chatbotCategory = new ChatbotCategory();
        chatbotCategory.setName(categoryName);

        try {
            chatbotCategoryRepo.save(chatbotCategory);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Chatbot category name already exists with name: " + categoryName, ex);
        }
    }

    public List<ChatbotCategoryResponseDTO> getAllCategories() {
        List<ChatbotCategory> chatbotCategories = chatbotCategoryRepo.findByIsDeletedFalse();

        // Single grouped query instead of one countByCategoryId(...) call per category (N+1 fix).
        java.util.Map<Long, Long> countsByCategoryId = new java.util.HashMap<>();
        for (Object[] row : chatbotRepo.countChatbotsGroupedByCategory()) {
            countsByCategoryId.put((Long) row[0], (Long) row[1]);
        }

        List<ChatbotCategoryResponseDTO> chatbotCategoryDTOs = new ArrayList<>();
        for (ChatbotCategory category : chatbotCategories) {
            ChatbotCategoryResponseDTO categoryDTO = ChatbotCategoryResponseDTO.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .chatbotCount(Math.toIntExact(countsByCategoryId.getOrDefault(category.getId(), 0L)))
                    .build();
            chatbotCategoryDTOs.add(categoryDTO);
        }
        return chatbotCategoryDTOs;
    }

    public void deleteCategory(Long id) {
        if (chatbotRepo.existsByCategoryId(id)) {
            ChatbotCategory category = chatbotCategoryRepo.findById(id).get();
            category.setDeleted(true);
            chatbotCategoryRepo.save(category);
            messageClassService.deleteAllMessageClassesByCategory(id);
        } else {
            messageClassService.deleteAllMessageClassesByCategory(id);
            chatbotCategoryRepo.deleteById(id);
        }
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

    public void deleteMessageClassForCategory(Long categoryId, Long classId) {
        ChatbotCategory category = getChabotCategoryById(categoryId);
        category.getMessageClasses().removeIf(messageClass -> messageClass.getId().equals(classId));
        chatbotCategoryRepo.save(category);
        messageClassService.deleteMessageClassFromCategory(classId);
    }
}
