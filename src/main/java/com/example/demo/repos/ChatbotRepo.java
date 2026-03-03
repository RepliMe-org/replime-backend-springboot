package com.example.demo.repos;

import com.example.demo.entities.Chatbot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotRepo extends JpaRepository<Chatbot, UUID> {
    List<Chatbot> findAllByIsPublicTrue();
}
