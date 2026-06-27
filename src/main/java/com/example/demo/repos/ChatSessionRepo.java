package com.example.demo.repos;

import com.example.demo.entities.ChatSession;
import com.example.demo.entities.utils.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepo extends JpaRepository<ChatSession, Long> {

    long countByUserIdAndStatusNot(Long userId, ChatSessionStatus status);

    List<ChatSession> findByChatbotId(UUID chatbotId);

    @Query("""
    SELECT s
    FROM ChatSession s
    WHERE s.user.id = :userId
    AND s.chatbot.id = :chatbotId
    AND s.status != 'DELETED'
    ORDER BY s.lastMessageAt DESC, s.id DESC
""")
    List<ChatSession> findFirstPage( // without cursor
         @Param("userId")    Long userId,
         @Param("chatbotId") UUID chatbotId,
         Pageable pageable

    );

    @Query(value = """
        SELECT * FROM chat_session s
        WHERE s.user_id    = :userId
        AND   s.chatbot_id = :chatbotId
        AND   s.status != 'DELETED'
        AND (
            s.last_message_at < :cursorTime
            OR (
                s.last_message_at = :cursorTime
                AND s.id < CAST(:cursorId AS BIGINT)
            )
        )
        ORDER BY s.last_message_at DESC, s.id DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<ChatSession> findSessions(
            @Param("userId")     Long userId,
            @Param("chatbotId")  UUID chatbotId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId")   Long cursorId,
            @Param("limit")      int limit
    );
}
