package com.example.demo.repos;

import com.example.demo.entities.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepo extends JpaRepository<ChatSession, Long> {
}
