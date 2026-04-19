package com.example.demo.services;

import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.repos.TrainingSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TrainingSourceService {
    @Autowired
    private TrainingSourceRepository trainingSourceRepository;

    public void addTrainingSourceToChatbot(TrainingSourceRequestDTO sourceRequest, Chatbot chatbot) {

    }
}

