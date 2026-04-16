package com.example.demo.repos;

import com.example.demo.entities.TrainingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingSourceRepository extends JpaRepository<TrainingSource, Long> {
}

