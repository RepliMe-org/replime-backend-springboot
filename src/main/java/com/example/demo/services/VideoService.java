package com.example.demo.services;

import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Video;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.repos.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoService {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private FastApiService fastApiService;

    @Transactional
    public void deleteVideoFromChatbot(Long videoId, Chatbot chatbot) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));

        if (!video.getTrainingSource().getChatbot().getId().equals(chatbot.getId())) {
            throw new TrainingSourceException("FORBIDDEN", "This video does not belong to your chatbot", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        try {
            Map<String, Object> response = fastApiService.deleteVideoChunks(video.getId().toString(), chatbot.getId().toString());
            System.out.println("FastAPI Deleted chunks: " + response.get("deleted_chunks"));
        } catch (Exception e) {
            throw new TrainingSourceException("AI_SERVICE_ERROR", "Failed to delete video chunks from AI service: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

        videoRepository.delete(video);
    }
}

