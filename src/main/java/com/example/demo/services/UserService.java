package com.example.demo.services;

import com.example.demo.dtos.UserInfoResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.Role;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ChatbotService chatbotService;
    @Autowired
    private ChatSessionRepo chatSessionRepo;

    public List<UserInfoResponseDTO> getAllUsers() {
        List<User> users = userRepo.findAll();
        List<UserInfoResponseDTO> userDTOs = new ArrayList<>();
        for (User user : users) {
            Chatbot chatbot = null;
            if (user.getRole() == Role.INFLUENCER) {
                chatbot = chatbotService.getChatbotByUser(user);
            }
            long sessionCount = chatSessionRepo.countByUserIdAndStatusNot(
                    user.getId(), ChatSessionStatus.DELETED);
            UserInfoResponseDTO userDTO = UserInfoResponseDTO.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .chatbotName(chatbot != null && chatbot.getConfig() != null ? chatbot.getConfig().getName() : null)
                    .conversationsCount(sessionCount)
                    .build();
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }

    public void promoteToAdmin(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        user.setRole(Role.ADMIN);
        userRepo.save(user);
    }
}
