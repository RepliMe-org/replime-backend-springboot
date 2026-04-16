package com.example.demo.services;

import com.example.demo.repos.IngestedContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class IngestedContentService {
    @Autowired
    private IngestedContentRepository ingestedContentRepository;

}

