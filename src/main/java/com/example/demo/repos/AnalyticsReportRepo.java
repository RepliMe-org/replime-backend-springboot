package com.example.demo.repos;

import com.example.demo.entities.AnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsReportRepo extends JpaRepository<AnalyticsReport, Long> {
    List<AnalyticsReport> findByChatbotIdOrderByGeneratedAtDesc(UUID chatbotId);

    Optional<AnalyticsReport> findFirstByChatbotIdOrderByGeneratedAtDesc(UUID chatbotId);
}
