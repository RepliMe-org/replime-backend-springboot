package com.example.demo.services;

import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.VideoResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.repos.TrainingSourceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingSourceService {
    @Autowired
    private TrainingSourceRepository trainingSourceRepository;

    @Autowired
    private VideoService videoService;

    @Transactional
    public List<VideoResponseDTO> addTrainingSourceToChatbot(TrainingSourceRequestDTO sourceRequest, Chatbot chatbot) {
        TrainingSource trainingSource = TrainingSource.builder()
                .chatbot(chatbot)
                .sourceType(sourceRequest.getSourceType())
                .sourceValue(sourceRequest.getSourceValue())
                .last_n(sourceRequest.getLast_n())
                .addedAt(LocalDateTime.now())
                .syncStatus(SyncStatus.PROCESSING)
                .build();

        trainingSource = trainingSourceRepository.save(trainingSource);

        List<Video> successfullySavedVideos = videoService.fetchAndSaveVideosForTrainingSource(sourceRequest, trainingSource, chatbot);

        if (successfullySavedVideos.isEmpty()) {
            throw new TrainingSourceException("NO_NEW_VIDEO","No videos were saved for the provided training source.", HttpStatus.OK);
        }

        trainingSource.setVideos(successfullySavedVideos);
        trainingSourceRepository.save(trainingSource);

        videoService.indexSavedVideos(successfullySavedVideos, chatbot);

        return videoService.mapToVideoResponseDTO(successfullySavedVideos);
    }

    public void addInitialTrainingSource(Chatbot chatbot, String channelId) {
        TrainingSource trainingSource = TrainingSource.builder()
                .chatbot(chatbot)
                .sourceType(SourceType.CHANNEL)
                .sourceValue(channelId)
                .last_n(null)
                .addedAt(LocalDateTime.now())
                .syncStatus(SyncStatus.PROCESSING)
                .build();
        trainingSource = trainingSourceRepository.save(trainingSource);

        List<Video> channelVideos = videoService.getChannelVideos(channelId,trainingSource);
        trainingSource.setVideos(channelVideos);
        trainingSourceRepository.save(trainingSource);

        videoService.indexSavedVideos(channelVideos, chatbot);
    }

}
