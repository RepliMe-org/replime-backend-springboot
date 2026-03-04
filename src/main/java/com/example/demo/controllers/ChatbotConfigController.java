package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.services.ChatbotConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("chatbot/config")
public class ChatbotConfigController {
    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @PostMapping
    ResponseEntity<String> saveChatbotConfig(
            @RequestBody ChatbotConfigRequestDTO requestDTO,
            @RequestHeader("Authorization") String token) {
        return chatbotConfigService.saveChatbotConfig(requestDTO, token);
    }

    @PutMapping
    ResponseEntity<String> editChatbotConfig(
            @RequestBody ChatbotConfigUpdateDTO requestDTO,
            @RequestHeader("Authorization") String token) {
        return chatbotConfigService.updateChatbotConfig(requestDTO,token);
    }
}
