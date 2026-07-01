package com.example.demo.repos;

import com.example.demo.entities.Chatbot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotRepo extends JpaRepository<Chatbot, UUID> {
    List<Chatbot> findAllByIsPublicTrue();

    // Eager-fetches config/category/influencer in one query to avoid N+1 selects
    // when mapping a list of chatbots (e.g. the public chatbot listing).
    @Query(
        "SELECT DISTINCT c FROM Chatbot c " +
        "LEFT JOIN FETCH c.config " +
        "LEFT JOIN FETCH c.category " +
        "LEFT JOIN FETCH c.influencer " +
        "WHERE c.isPublic = true"
    )
    List<Chatbot> findAllByIsPublicTrueWithDetails();

    Chatbot findByInfluencerId(Long id);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    // Single grouped query instead of one countByCategoryId(...) call per category.
    @Query(
        "SELECT c.category.id, COUNT(c) FROM Chatbot c " +
        "WHERE c.category IS NOT NULL GROUP BY c.category.id"
    )
    List<Object[]> countChatbotsGroupedByCategory();
}
