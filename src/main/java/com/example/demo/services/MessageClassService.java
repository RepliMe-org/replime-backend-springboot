package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.MessageClassRequestDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.*;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageClassRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MessageClassService {
    @Autowired
    private MessageClassRepo messageClassRepo;
    @Autowired
    private ChatbotCategoryService chatbotCategoryService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ChatbotService chatbotService;
    @Autowired
    private ChatbotRepo chatbotRepo;

    public List<MessageClassResponseDTO> getAllMainMessageClasses(
            Long id) {
        chatbotCategoryService.getChabotCategoryById(id);
        List<MessageClass> messageClasses = messageClassRepo.findByCategoryIdAndType(id, MessageClassType.SYSTEM);
        return MapToMessageClassResponseDTO(messageClasses);
    }

    private List<MessageClassResponseDTO> MapToMessageClassResponseDTO(
            List<MessageClass> messageClasses) {
        List<MessageClassResponseDTO> messageClassResponseDTOS = new ArrayList<>();
        for (MessageClass messageClass : messageClasses) {
            MessageClassResponseDTO messageClassResponseDTO = MessageClassResponseDTO.builder()
                    .id(messageClass.getId())
                    .name(messageClass.getName())
                    .build();
            messageClassResponseDTOS.add(messageClassResponseDTO);
        }
        return messageClassResponseDTOS;
    }

    public List<MessageClassResponseDTO> getAllMessageClassesByCategoryUserToken(
            String token) {
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = chatbotService.getChatbotByUser(user);
        if (chatbot == null) {
            throw new ResourceNotFoundException("Chatbot not found for influencer");
        }
        if (chatbot.getCategory() == null) {
            throw new ResourceNotFoundException("Chatbot category not yet set for influencer");
        }
        List<MessageClass> systemCategoryClasses = messageClassRepo.findByChatbotsContainingAndType(
                chatbot, MessageClassType.SYSTEM);
        List<MessageClass> customChatbotClasses = messageClassRepo.findByChatbotsContainingAndType(
                chatbot, MessageClassType.CUSTOM);

        Map<Long, MessageClass> merged = new LinkedHashMap<>();
        for (MessageClass messageClass : systemCategoryClasses) {
            merged.put(messageClass.getId(), messageClass);
        }
        for (MessageClass messageClass : customChatbotClasses) {
            merged.put(messageClass.getId(), messageClass);
        }

        List<MessageClass> messageClasses = new ArrayList<>(merged.values());
        return MapToMessageClassResponseDTO(messageClasses);
    }

    public List<MessageClassResponseDTO> createMessageClassForCategory(
            Long categoryId, MessageClassRequestDTO messageClassRequestDTO) {
        ChatbotCategory category = chatbotCategoryService.getChabotCategoryById(categoryId);
        MessageClass newMessageClass = MessageClass.builder()
                .name(messageClassRequestDTO.getName())
                .category(category)
                .type(MessageClassType.SYSTEM)
                .build();
        messageClassRepo.save(newMessageClass);
        return getAllMainMessageClasses(categoryId);
    }

    public MessageClassResponseDTO createMessageClassForSpecificChatbot(
            String token, MessageClassRequestDTO messageClassRequestDTO
    ){
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = chatbotService.getChatbotByUser(user);
        if (chatbot == null) {
            throw new ResourceNotFoundException("Chatbot not found for influencer");
        }
        if (chatbot.getCategory() == null) {
            throw new ResourceNotFoundException("Chatbot category not yet set for influencer");
        }
        MessageClass newMessageClass = MessageClass.builder()
                .name(messageClassRequestDTO.getName())
                .category(chatbot.getCategory())
                .type(MessageClassType.CUSTOM)
                .build();
        MessageClass savedMessageClass = messageClassRepo.save(newMessageClass);
        chatbot.getMessageClasses().add(savedMessageClass);
        chatbotRepo.save(chatbot);
        return MessageClassResponseDTO.builder()
                .id(savedMessageClass.getId())
                .name(savedMessageClass.getName())
                .build();
    }
}
