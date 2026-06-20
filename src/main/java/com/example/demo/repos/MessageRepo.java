package com.example.demo.repos;

import com.example.demo.entities.Message;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {
    List<Message> findByMessageClassId(Long messageClassId);

    @Query("""
            SELECT m FROM Message m
            WHERE m.session.chatbot.id = :chatbotId
              AND m.sender = :sender
              AND m.intent = :intent
            """)
    List<Message> findByChatbotAndSenderAndIntent(
            @Param("chatbotId") UUID chatbotId,
            @Param("sender") MessageSender sender,
            @Param("intent") MessageIntent intent);
}
