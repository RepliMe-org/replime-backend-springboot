package com.example.demo.repos;

import com.example.demo.entities.ChatbotCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotCategoryRepo extends JpaRepository<ChatbotCategory, Long> {
	boolean existsByName(String name);
}
