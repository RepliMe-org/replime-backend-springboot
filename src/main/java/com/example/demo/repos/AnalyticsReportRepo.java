package com.example.demo.repos;

import com.example.demo.entities.AnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsReportRepo extends JpaRepository<AnalyticsReport, Long> {

    Optional<AnalyticsReport> findFirstByChatbotIdOrderByGeneratedAtDesc(UUID chatbotId);

    Optional<AnalyticsReport> findByChatbotIdAndGeneratedAt(UUID chatbotId, LocalDateTime generatedAt);

    @Query("SELECT r.generatedAt, r.contentGapCount FROM AnalyticsReport r WHERE r.chatbot.id = :chatbotId ORDER BY r.generatedAt DESC")
    List<Object[]> findHistoryByChatbotId(@Param("chatbotId") UUID chatbotId);
}
