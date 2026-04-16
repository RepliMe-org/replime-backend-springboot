package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.MessageClassRequestDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.*;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageClassRepo;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageClassService {

    @Autowired
    private MessageClassRepo messageClassRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatbotRepo chatbotRepo;

    public List<MessageClassResponseDTO> getAllSystemMessageClassesForCategory(
        Long id
    ) {
        List<MessageClass> messageClasses =
            messageClassRepo.findByCategoryIdAndType(
                id,
                MessageClassType.SYSTEM
            );
        return MapToMessageClassResponseDTO(messageClasses);
    }

    private List<MessageClassResponseDTO> MapToMessageClassResponseDTO(
        List<MessageClass> messageClasses
    ) {
        List<MessageClassResponseDTO> messageClassResponseDTOS =
            new ArrayList<>();
        for (MessageClass messageClass : messageClasses) {
            MessageClassResponseDTO messageClassResponseDTO =
                MessageClassResponseDTO.builder()
                    .id(messageClass.getId())
                    .name(messageClass.getName())
                    .build();
            messageClassResponseDTOS.add(messageClassResponseDTO);
        }
        return messageClassResponseDTOS;
    }

    public List<MessageClassResponseDTO> getAllMessageClassesByUserChatbot(
        Chatbot chatbot
    ) {
        List<MessageClass> systemCategoryClasses =
            messageClassRepo.findByChatbotsContainingAndType(
                chatbot,
                MessageClassType.SYSTEM
            );
        List<MessageClass> customChatbotClasses =
            messageClassRepo.findByChatbotsContainingAndType(
                chatbot,
                MessageClassType.CUSTOM
            );

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
        ChatbotCategory category,
        List<String> messageClassesNames
    ) {
        for (String messageClassName : messageClassesNames) {
            if (messageClassRepo.existsByCategoryIdAndName(
                category.getId(), messageClassName
            )) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Message class name already exists in this category: " + messageClassName
                );
            }
        }
        for (String messageClassName : messageClassesNames) {
            MessageClass newMessageClass = MessageClass.builder()
                    .name(messageClassName)
                    .category(category)
                    .type(MessageClassType.SYSTEM)
                    .build();
            messageClassRepo.save(newMessageClass);
        }
        return getAllSystemMessageClassesForCategory(category.getId());
    }

    public void assignClassesToChatbot(
        List<Long> messageClassIds,
        Chatbot chatbot
    ) {
        List<MessageClass> messageClasses = messageClassRepo.findByIdIn(
            messageClassIds
        );
        for (MessageClass messageClass : messageClasses) {
             if (!messageClass.getChatbots().contains(chatbot)) {
                messageClass.getChatbots().add(chatbot);
                messageClassRepo.save(messageClass);
            }
        }
        chatbot.getMessageClasses().addAll(messageClasses);

        chatbotRepo.save(chatbot);
    }

    public void createCustomMessageClassesForChatbot(
            Chatbot chatbot, List<String> messageClassesNames
    ){
        for(String messageClassName : messageClassesNames) {

            MessageClass newMessageClass = MessageClass.builder()
                    .name(messageClassName)
                    .category(chatbot.getCategory())
                    .type(MessageClassType.CUSTOM)
                    .chatbots(new HashSet<>(List.of(chatbot)))
                    .build();
            MessageClass savedMessageClass = messageClassRepo.save(newMessageClass);
            chatbot.getMessageClasses().add(savedMessageClass);
        }
        chatbotRepo.save(chatbot);
    }

    public void deleteMessageClassfromChatbot(Long messageClassId, Chatbot chatbot) {
        MessageClass messageClass = messageClassRepo.findById(messageClassId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Message class not found with id: " + messageClassId
            ));
        if (messageClass.getType() == MessageClassType.SYSTEM) {
            chatbot.getMessageClasses().remove(messageClass);
            messageClass.getChatbots().remove(chatbot);
            messageClassRepo.save(messageClass);
        }else { // CUSTOM message class, delete it entirely
            chatbot.getMessageClasses().remove(messageClass);
            messageClassRepo.delete(messageClass);
        }
         chatbotRepo.save(chatbot);
    }
}
