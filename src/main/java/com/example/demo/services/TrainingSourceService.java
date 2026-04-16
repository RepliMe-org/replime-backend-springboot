package com.example.demo.services;

import com.example.demo.repos.TrainingSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TrainingSourceService {
    @Autowired
    private TrainingSourceRepository trainingSourceRepository;

}

