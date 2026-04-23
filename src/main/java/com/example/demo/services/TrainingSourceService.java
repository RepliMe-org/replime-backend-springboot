package com.example.demo.services;

import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.internal.VideoIndexRequestDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.User;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.example.demo.repos.TrainingSourceRepository;
import com.example.demo.repos.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingSourceService {
    @Autowired
    private TrainingSourceRepository trainingSourceRepository;

    @Autowired
    private YoutubeClientService youtubeClientService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;

    @Autowired
    private FastApiService fastApiService;

    @Transactional
    public void addTrainingSourceToChatbot(TrainingSourceRequestDTO sourceRequest, Chatbot chatbot) {
        TrainingSource trainingSource = TrainingSource.builder()
                .chatbot(chatbot)
                .sourceType(sourceRequest.getSourceType())
                .sourceValue(sourceRequest.getSourceValue())
                .last_n(sourceRequest.getLast_n())
                .addedAt(LocalDateTime.now())
                .syncStatus(SyncStatus.PROCESSING)
                .build();

        trainingSource = trainingSourceRepository.save(trainingSource);

        List<Video> videosToSave;

        if (sourceRequest.getSourceType() == SourceType.PLAYLIST) {
            String playlistId = youtubeClientService.extractPlaylistId(sourceRequest.getSourceValue());
            if (playlistId != null) {
                videosToSave = youtubeClientService.getVideosFromPlaylist(playlistId);
            } else {
                throw new TrainingSourceException("INVALID_URL", "Could not extract playlist ID from URL", HttpStatus.BAD_REQUEST);
            }
        } else {
            videosToSave = new ArrayList<>();
            if (sourceRequest.getSourceType() == SourceType.VIDEO) {
                String videoId = youtubeClientService.extractVideoId(sourceRequest.getSourceValue());
                if (videoId == null) {
                    throw new TrainingSourceException("INVALID_URL", "Could not extract video ID from URL", HttpStatus.BAD_REQUEST);
                }
                if (videoRepository.existsByYoutubeVideoId(videoId)) {
                    throw new TrainingSourceException("ALREADY_INGESTED", "This video is already in your knowledge base", HttpStatus.CONFLICT);
                }
                Video video = youtubeClientService.getVideoDetailsForEntity(videoId);
                if (video == null) {
                    throw new TrainingSourceException("VIDEO_NOT_FOUND", "YouTube video does not exist", HttpStatus.BAD_REQUEST);
                }
                videosToSave.add(video);
            } else if (sourceRequest.getSourceType() == SourceType.LAST_N) {
                User user = chatbot.getInfluencer();
                influencerVerificationRepo.findByUserAndStatusIn(user, List.of(VerificationStatus.VERIFIED))
                        .ifPresent(verification -> {
                            String channelId = verification.getChannelId();
                            if (channelId != null && sourceRequest.getLast_n() != null) {
                                List<Video> latestVideos = youtubeClientService.getLatestVideosFromChannel(channelId, sourceRequest.getLast_n());
                                videosToSave.addAll(latestVideos);
                            }
                        });
            }
        }

        List<Video> successfullySavedVideos = new ArrayList<>();
        for (Video video : videosToSave) {
            if (video.getYoutubeVideoId() != null && !videoRepository.existsByYoutubeVideoId(video.getYoutubeVideoId())) {
                video.setTrainingSource(trainingSource);
                video = videoRepository.save(video);
                successfullySavedVideos.add(video);
            }
        }
        if (successfullySavedVideos.isEmpty()) {
            trainingSource.setSyncStatus(SyncStatus.FAILED);
        }

        trainingSource.setVideos(successfullySavedVideos);
        trainingSourceRepository.save(trainingSource);

        for (Video video : successfullySavedVideos) {
            VideoIndexRequestDTO videoIndexRequestDTO = VideoIndexRequestDTO.builder()
                    .videoTitle(video.getTitle())
                    .chatbotId(chatbot.getId().toString())
                    .youtubeVideoId(video.getYoutubeVideoId())
                    .build();
            fastApiService.indexVideo(videoIndexRequestDTO,video.getId().toString());
        }
    }
}
