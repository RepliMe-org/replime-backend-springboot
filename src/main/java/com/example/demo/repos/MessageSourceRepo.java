package com.example.demo.repos;

import com.example.demo.entities.MessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageSourceRepo extends JpaRepository<MessageSource, Long> {

    @Query("""
            SELECT ms.video.youtubeVideoId, ms.video.title, COUNT(ms)
            FROM MessageSource ms
            WHERE ms.message.session.chatbot.id = :chatbotId
            GROUP BY ms.video.youtubeVideoId, ms.video.title
            ORDER BY COUNT(ms) DESC
            """)
    List<Object[]> aggregateCitedVideosByChatbot(@Param("chatbotId") UUID chatbotId);
}
