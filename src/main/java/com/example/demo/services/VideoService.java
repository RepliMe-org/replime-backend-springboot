package com.example.demo.services;

import com.example.demo.dtos.*;
import com.example.demo.dtos.internal.UpdateVideoStatusRequestDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.*;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.dtos.internal.VideoIndexRequestDTO;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.User;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.example.demo.repos.TrainingSourceRepository;
import com.example.demo.repos.VideoRepository;
import com.example.demo.services.rabbitMQService.VideoIndexPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final boolean USE_DIRECT_INDEXING = false;

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
    private ChatbotRepo chatbotRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private VideoIndexPublisher videoIndexPublisher;

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
        if (successfullySavedVideos.isEmpty()) return;
        if (USE_DIRECT_INDEXING) {
            List<VideoIndexRequestDTO.VideoItem> videoItems = successfullySavedVideos.stream()
                    .map(video -> VideoIndexRequestDTO.VideoItem.builder()
                            .youtubeVideoId(video.getYoutubeVideoId())
                            .videoTitle(video.getTitle())
                            .build())
                    .toList();

            VideoIndexRequestDTO videoIndexRequestDTO = VideoIndexRequestDTO.builder()
                    .chatbotId(chatbot.getId().toString())
                    .videos(videoItems)
                    .build();

            fastApiService.indexVideos(videoIndexRequestDTO);
        }else{
            videoIndexPublisher.publishForIndexing(successfullySavedVideos);
        }
    }

    @Transactional
    public void deleteVideoFromChatbot(String youtubeVideoId, Chatbot chatbot) {
        Video video = videoRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + youtubeVideoId));

        if (!video.getTrainingSource().getChatbot().getId().equals(chatbot.getId())) {
            throw new TrainingSourceException("FORBIDDEN", "This video does not belong to your chatbot",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }

        DeleteVideoRequestDTO deleteVideoRequestDTO = DeleteVideoRequestDTO.builder()
                .youtube_video_id(youtubeVideoId)
                .chatbot_id(chatbot.getId().toString())
                .build();

        try {
            Map<String, Object> response =
                    fastApiService.deleteVideoChunks(video.getId().toString(), deleteVideoRequestDTO);
            System.out.println("FastAPI Deleted chunks: " + response.get("deleted_chunks"));
        } catch (Exception e) {
            throw new TrainingSourceException("AI_SERVICE_ERROR", "Failed to delete video chunks from AI service: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

        videoRepository.delete(video);
    }

    @Transactional
    public VideoResponseDTO retryVideo(Long videoId, Chatbot chatbot) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));

        if (!video.getTrainingSource().getChatbot().getId().equals(chatbot.getId())) {
            throw new TrainingSourceException("FORBIDDEN", "You do not own this video", HttpStatus.FORBIDDEN);
        }

        if (video.getSyncStatus() == SyncStatus.PROCESSING ||
            video.getSyncStatus() == SyncStatus.COMPLETED) {
            throw new TrainingSourceException("INVALID_STATE", "Video is already " + video.getSyncStatus() + " — cannot retry", HttpStatus.BAD_REQUEST);
        }

        video.setRetryCount(0);
        video.setSyncStatus(SyncStatus.PROCESSING);
        video.setFailureReason(null);
        video.setFailedStage(null);
        video.setLastRetryAt(OffsetDateTime.now());
        videoRepository.save(video);

        Chatbot videoChatbot = video.getTrainingSource().getChatbot();
        if (videoChatbot.getStatus() != ChatbotStatus.TRAINING) {
            videoChatbot.setStatus(ChatbotStatus.TRAINING);
            videoChatbot.setPublic(false);
            chatbotRepo.save(videoChatbot);
        }

        TrainingSource trainingSource = video.getTrainingSource();
        if(trainingSource.getSyncStatus() != SyncStatus.PROCESSING){
            trainingSource.setSyncStatus(SyncStatus.PROCESSING);
            trainingSourceRepository.save(trainingSource);
        }

        videoIndexPublisher.publishForIndexing(List.of(video));

        return mapToVideoResponseDTO(List.of(video)).get(0);
    }

    public List<VideoResponseDTO> mapToVideoResponseDTO(List<Video> videos) {
        return videos.stream().map(video -> VideoResponseDTO.builder()
                .videoId(video.getId())
                .sourceId(video.getTrainingSource().getId())
                .youtubeVideoId(video.getYoutubeVideoId())
                .title(video.getTitle())
                .duration(video.getDuration())
                .thumbnail(video.getThumbnailUrl())
                .syncStatus(video.getSyncStatus())
                .failureReason(isFailedVideo(video) ? video.getFailureReason() : null)
                .build()).toList();
    }

    private boolean isFailedVideo(Video video) {
        return video.getSyncStatus() == SyncStatus.FAILED || video.getSyncStatus() == SyncStatus.DEAD;
    }

    public void updateVideoStatus(String youtubeVideoId, UpdateVideoStatusRequestDTO request) {
        Video video = videoRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseThrow(() -> new ResourceNotFoundException("video not found with id: " + youtubeVideoId));
        System.out.println(video);
        try {
            SyncStatus newStatus = SyncStatus.valueOf(request.getStatus());

            // ── NEW: populate failure tracking fields ────────────────────
            if (newStatus == SyncStatus.FAILED || newStatus == SyncStatus.DEAD) {

                // Map the stage string to your enum safely
                if (request.getFailedStage() != null) {
                    try {
                        video.setFailedStage(
                                FailedStage.valueOf(request.getFailedStage()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown failedStage value: {}",
                                request.getFailedStage());
                    }
                }

                if (request.getFailureReason() != null) {
                    video.setFailureReason(request.getFailureReason());
                }

                if (request.getAttemptsMade() != null) {
                    video.setRetryCount(request.getAttemptsMade());
                }

                video.setLastRetryAt(OffsetDateTime.now());

                // If FastAPI says non-retryable → force DEAD regardless of count
                boolean isRetryable = request.getRetryable() == null
                        || Boolean.TRUE.equals(request.getRetryable());

                if (!isRetryable || !video.hasRetriesLeft()) {
                    // Permanently dead — user must manually retry
                    newStatus = SyncStatus.DEAD;
                    log.warn("Video {} permanently failed at stage {}. Reason: {}",
                            youtubeVideoId, video.getFailedStage(),
                            video.getFailureReason());
                } else {
                    // Transient failure — RetryScheduler will pick it up
                    newStatus = SyncStatus.FAILED;
                    log.warn("Video {} failed (attempt {}/{}). Will retry at {}",
                            youtubeVideoId, video.getRetryCount(),
                            video.getMaxRetries(),
                            video.nextRetryEligibleAt());
                }

            } else if (newStatus == SyncStatus.COMPLETED) {
                // Clear stale failure data on success
                video.setFailureReason(null);
                video.setFailedStage(null);
                if (request.getDescription() != null) {
                    Chatbot chatbot = video.getTrainingSource().getChatbot();
                    var config = chatbot.getConfig();
                    if (config != null) {
                        config.setAiGeneratedDescription(request.getDescription());
                        chatbotRepo.save(chatbot);
                    }
                }
            }

            video.setSyncStatus(newStatus);
            videoRepository.save(video);
            sendVideoUpdateWebsocket(video);
            checkAndFinalizeTrainingSource(video);

        } catch (IllegalArgumentException e) {
            throw new TrainingSourceException("INVALID_STATUS",
                    "Invalid sync status value: " + request.getStatus(), HttpStatus.BAD_REQUEST);
        }
    }

    private void sendVideoUpdateWebsocket(Video video) {
        TrainingSource trainingSource = video.getTrainingSource();

        System.out.println("Sending to topic: /topic/chatbot/"
                + trainingSource.getChatbot().getId() + "/sync-status");

        // Send websocket notification for single video update
        SyncStatusMessageDTO videoUpdateMsg = SyncStatusMessageDTO.builder()
                .type("VIDEO_UPDATE")
                .sourceId(trainingSource.getId())
                .videoId(video.getId())
                .status(video.getSyncStatus().name())
                .errorMessage(isFailedVideo(video) ? video.getFailureReason() : null)
                .build();
        messagingTemplate.convertAndSend("/topic/chatbot/" +
                trainingSource.getChatbot().getId() + "/sync-status", videoUpdateMsg);

        System.out.println("Sync status updated successfully");
    }

    private void checkAndFinalizeTrainingSource(Video video) {
        TrainingSource trainingSource = video.getTrainingSource();
        boolean allFinished = true;
        for (Video v : trainingSource.getVideos()) {
            // If it's this video, use the new status
            SyncStatus currentStatus = v.getId().equals(video.getId()) ? video.getSyncStatus() : v.getSyncStatus();
            if (currentStatus == SyncStatus.PROCESSING) {
                allFinished = false;
                break;
            }
        }

        if (allFinished && trainingSource.getSyncStatus() == SyncStatus.PROCESSING) {
            trainingSource.setSyncStatus(SyncStatus.COMPLETED);
            trainingSourceRepository.save(trainingSource);
            trainingSource.getChatbot().setStatus(ChatbotStatus.ACTIVE);
            trainingSource.getChatbot().setPublic(true);
            chatbotRepo.save(trainingSource.getChatbot());
            // System out for notification (or real notification logic)
            System.out.println("Notification: Ingestion finished for training source ID "
                    + trainingSource.getId() + " of user " + trainingSource.getChatbot().getInfluencer().getUsername());

            // Send websocket notification for entire source
            SyncStatusMessageDTO sourceUpdateMsg = SyncStatusMessageDTO.builder()
                    .type("SOURCE_COMPLETE")
                    .sourceId(trainingSource.getId())
                    .status(SyncStatus.COMPLETED.name())
                    .build();
            messagingTemplate.convertAndSend("/topic/chatbot/" +
                    trainingSource.getChatbot().getId() + "/sync-status", sourceUpdateMsg);
        }
    }

    public List<Video> getChannelVideos(String channelId, TrainingSource trainingSource) {


        List<Video> allVideos = youtubeClientService.getAllVideosFromChannel(channelId);
        List<Video> successfullySavedVideos = new ArrayList<>();

        for (Video video : allVideos) {
            if (video.getYoutubeVideoId() != null &&
                    !videoRepository.existsByYoutubeVideoId(video.getYoutubeVideoId())) {
                video.setTrainingSource(trainingSource);
                video = videoRepository.save(video);
                successfullySavedVideos.add(video);
            }
        }
        return successfullySavedVideos;
    }

    public List<VideoResponseDTO> getAllVideosOfChatbot(Chatbot chatbot) {
        List<TrainingSource> chatbotTrainingSources  = chatbot.getTrainingSources();
        List<Video> allVideosOfChatbot = new ArrayList<>();
        for (TrainingSource trainingSource : chatbotTrainingSources) {
            List<Video> videosOfSource = videoRepository.findByTrainingSource(trainingSource);
            allVideosOfChatbot.addAll(videosOfSource);
        }
        return mapToVideoResponseDTO(allVideosOfChatbot);
    }

    public String getThumbnailByYoutubeVideoId(String videoId) {
        Video video = videoRepository.findByYoutubeVideoId(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));
        return video.getThumbnailUrl();
    }
}
