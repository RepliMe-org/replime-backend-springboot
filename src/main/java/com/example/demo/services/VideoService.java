package com.example.demo.services;

import com.example.demo.dtos.VideoResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.internal.VideoIndexRequestDTO;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.example.demo.repos.TrainingSourceRepository;
import com.example.demo.repos.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.SyncStatusMessageDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoService {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private FastApiService fastApiService;
    @Autowired
    private YoutubeClientService youtubeClientService;
    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;
    @Autowired
    private TrainingSourceRepository trainingSourceRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Video> fetchAndSaveVideosForTrainingSource(
            TrainingSourceRequestDTO sourceRequest,
            TrainingSource trainingSource,
            Chatbot chatbot)
    {
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
            if (video.getYoutubeVideoId() != null &&
                    !videoRepository.existsByYoutubeVideoId(video.getYoutubeVideoId())) {
                video.setTrainingSource(trainingSource);
                video = videoRepository.save(video);
                successfullySavedVideos.add(video);
            }
        }
        return successfullySavedVideos;
    }

    public void indexSavedVideos(List<Video> successfullySavedVideos, Chatbot chatbot) {
        for (Video video : successfullySavedVideos) {
            VideoIndexRequestDTO videoIndexRequestDTO = VideoIndexRequestDTO.builder()
                    .videoTitle(video.getTitle())
                    .chatbotId(chatbot.getId().toString())
                    .youtubeVideoId(video.getYoutubeVideoId())
                    .build();
            fastApiService.indexVideo(videoIndexRequestDTO, video.getId().toString());
        }
    }

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

    public List<VideoResponseDTO> mapToVideoResponseDTO(List<Video> videos) {
        return videos.stream().map(video -> VideoResponseDTO.builder()
                .videoId(video.getId())
                .sourceId(video.getTrainingSource().getId())
                .youtubeVideoId(video.getYoutubeVideoId())
                .title(video.getTitle())
                .thumbnail(video.getThumbnailUrl())
                .syncStatus(video.getSyncStatus())
                .build()).toList();
    }

    public void updateVideoStatus(String videoId, String status) {
        Video updateVideo = videoRepository.findById(Long.parseLong(videoId))
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));
        try {
            SyncStatus syncStatus = SyncStatus.valueOf(status);
            updateVideo.setSyncStatus(syncStatus);
            videoRepository.save(updateVideo);

            TrainingSource trainingSource = updateVideo.getTrainingSource();

            // Send websocket notification for single video update
            SyncStatusMessageDTO videoUpdateMsg = SyncStatusMessageDTO.builder()
                    .type("VIDEO_UPDATE")
                    .sourceId(trainingSource.getId())
                    .videoId(updateVideo.getId())
                    .status(syncStatus.name())
                    .build();
            messagingTemplate.convertAndSend("/topic/chatbot/" + trainingSource.getChatbot().getId() + "/sync-status", videoUpdateMsg);

            boolean allFinished = true;
            for (Video v : trainingSource.getVideos()) {
                // If it's this video, use the new status
                SyncStatus currentStatus = v.getId().equals(updateVideo.getId()) ? syncStatus : v.getSyncStatus();
                if (currentStatus == SyncStatus.PROCESSING) {
                    allFinished = false;
                    break;
                }
            }

            if (allFinished && trainingSource.getSyncStatus() == SyncStatus.PROCESSING) {
                trainingSource.setSyncStatus(SyncStatus.COMPLETED);
                trainingSourceRepository.save(trainingSource);
                // System out for notification (or real notification logic)
                System.out.println("Notification: Ingestion finished for training source ID " + trainingSource.getId() + " of user " + trainingSource.getChatbot().getInfluencer().getUsername());

                // Send websocket notification for entire source
                SyncStatusMessageDTO sourceUpdateMsg = SyncStatusMessageDTO.builder()
                        .type("SOURCE_COMPLETE")
                        .sourceId(trainingSource.getId())
                        .status(SyncStatus.COMPLETED.name())
                        .build();
                messagingTemplate.convertAndSend("/topic/chatbot/" + trainingSource.getChatbot().getId() + "/sync-status", sourceUpdateMsg);
            }

        } catch (IllegalArgumentException e) {
            throw new TrainingSourceException("INVALID_STATUS",
                    "Invalid sync status value: " + status, HttpStatus.BAD_REQUEST);
        }
    }
}
