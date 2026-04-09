package com.example.demo.repos;

import com.example.demo.entities.Chatbot;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.MessageClassType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageClassRepo extends JpaRepository<MessageClass, Long> {
    List<MessageClass> findByCategoryIdAndType(Long categoryId, MessageClassType type);
    List<MessageClass> findByChatbotsContaining(Chatbot chatbot);
    List<MessageClass> findByChatbotsContainingAndType(Chatbot chatbot, MessageClassType type);
}
