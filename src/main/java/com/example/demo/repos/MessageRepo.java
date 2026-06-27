package com.example.demo.repos;

import com.example.demo.entities.Message;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {
    List<Message> findByMessageClassId(Long messageClassId);

    List<Message> findBySessionIdOrderBySentAtAscIdAsc(Long sessionId);

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

    @Query("""
            SELECT m FROM Message m
            WHERE m.session.chatbot.id = :chatbotId
              AND m.sender = :sender
              AND m.intent = :intent
              AND m.sentAt >= :since
            """)
    List<Message> findByChatbotAndSenderAndIntentSince(
            @Param("chatbotId") UUID chatbotId,
            @Param("sender") MessageSender sender,
            @Param("intent") MessageIntent intent,
            @Param("since") LocalDateTime since);

    @Query(value = """
            SELECT m.*
            FROM message m
            JOIN chat_session s ON s.id = m.session_id
            WHERE s.user_id = :userId
              AND s.chatbot_id = :chatbotId
              AND s.status != 'DELETED'
              AND to_tsvector('simple', coalesce(m.content, '')) @@ plainto_tsquery('simple', :query)
            ORDER BY ts_rank_cd(
                    to_tsvector('simple', coalesce(m.content, '')),
                    plainto_tsquery('simple', :query)
                ) DESC,
                m.sent_at DESC,
                m.id DESC
            """, nativeQuery = true)
    List<Message> searchUserChatbotMessages(
            @Param("userId") Long userId,
            @Param("chatbotId") UUID chatbotId,
            @Param("query") String query);
}
