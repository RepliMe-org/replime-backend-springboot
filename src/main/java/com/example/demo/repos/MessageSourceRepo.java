package com.example.demo.repos;

import com.example.demo.entities.MessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageSourceRepo extends JpaRepository<MessageSource, Long> {
}
