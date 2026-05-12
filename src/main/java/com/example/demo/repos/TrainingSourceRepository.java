package com.example.demo.repos;

import com.example.demo.entities.TrainingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingSourceRepository extends JpaRepository<TrainingSource, Long> {
    List<TrainingSource> findByChatbotId(UUID id);
}

