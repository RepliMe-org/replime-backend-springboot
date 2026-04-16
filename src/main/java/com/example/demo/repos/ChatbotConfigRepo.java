package com.example.demo.repos;

import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotConfigRepo extends JpaRepository<ChatbotConfig,Long> {
}
