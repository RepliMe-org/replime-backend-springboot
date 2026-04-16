package com.example.demo.repos;

import com.example.demo.entities.IngestedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestedContentRepository extends JpaRepository<IngestedContent, Long> {
}

