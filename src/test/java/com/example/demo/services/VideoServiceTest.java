package com.example.demo.services;

import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.VideoResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.example.demo.repos.TrainingSourceRepository;
import com.example.demo.repos.VideoRepository;
import com.example.demo.services.rabbitMQService.VideoIndexPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for video ingestion validation, response mapping, retry rules,
// thumbnail lookup, ownership/state failures, status transitions, and indexing publisher side effects.
class VideoServiceTest {

    @Test
    void fetchAndSaveVideosForTrainingSourceThrowsForInvalidVideoUrl() {
        YoutubeClientService youtubeClientService = mock(YoutubeClientService.class);
        VideoService service = service(mock(VideoRepository.class), youtubeClientService);
        TrainingSourceRequestDTO request = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.VIDEO)
                .sourceValue("bad-url")
                .build();
        when(youtubeClientService.extractVideoId("bad-url")).thenReturn(null);

        TrainingSourceException exception = assertThrows(
                TrainingSourceException.class,
                () -> service.fetchAndSaveVideosForTrainingSource(request, TrainingSource.builder().build(), Chatbot.builder().build()));

        assertEquals("Could not extract video ID from URL", exception.getMessage());
    }

    @Test
    void fetchAndSaveVideosForTrainingSourceThrowsWhenVideoAlreadyExists() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        YoutubeClientService youtubeClientService = mock(YoutubeClientService.class);
        VideoService service = service(videoRepository, youtubeClientService);
        TrainingSourceRequestDTO request = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.VIDEO)
                .sourceValue("https://youtube.com/watch?v=abc")
                .build();
        when(youtubeClientService.extractVideoId(request.getSourceValue())).thenReturn("abc");
        when(videoRepository.existsByYoutubeVideoId("abc")).thenReturn(true);

        TrainingSourceException exception = assertThrows(
                TrainingSourceException.class,
                () -> service.fetchAndSaveVideosForTrainingSource(request, TrainingSource.builder().build(), Chatbot.builder().build()));

        assertEquals("This video is already in your knowledge base", exception.getMessage());
    }

    @Test
    void mapToVideoResponseDtoIncludesFailureReasonOnlyForFailedVideos() {
        VideoService service = service(mock(VideoRepository.class), mock(YoutubeClientService.class));
        TrainingSource source = TrainingSource.builder().id(4L).build();
        Video failed = Video.builder()
                .id(1L)
                .trainingSource(source)
                .youtubeVideoId("failed")
                .syncStatus(SyncStatus.FAILED)
                .failureReason("network")
                .build();
        Video completed = Video.builder()
                .id(2L)
                .trainingSource(source)
                .youtubeVideoId("done")
                .syncStatus(SyncStatus.COMPLETED)
                .failureReason("old")
                .build();

        List<VideoResponseDTO> responses = service.mapToVideoResponseDTO(List.of(failed, completed));

        assertEquals("network", responses.get(0).getFailureReason());
        assertNull(responses.get(1).getFailureReason());
    }

    @Test
    void retryVideoThrowsWhenVideoMissing() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.retryVideo(99L, Chatbot.builder().build()));

        assertEquals("Video not found with id: 99", exception.getMessage());
    }

    @Test
    void retryVideoThrowsWhenAlreadyProcessing() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).build();
        Video video = video(7L, chatbot, SyncStatus.PROCESSING);
        when(videoRepository.findById(7L)).thenReturn(Optional.of(video));

        TrainingSourceException exception = assertThrows(
                TrainingSourceException.class,
                () -> service.retryVideo(7L, chatbot));

        assertTrue(exception.getMessage().contains("Video is already PROCESSING"));
        assertTrue(exception.getMessage().contains("cannot retry"));
    }

    @Test
    void retryVideoResetsFailureStateAndPublishesForIndexing() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        TrainingSourceRepository trainingSourceRepository = mock(TrainingSourceRepository.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        VideoIndexPublisher publisher = mock(VideoIndexPublisher.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        ReflectionTestUtils.setField(service, "trainingSourceRepository", trainingSourceRepository);
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        ReflectionTestUtils.setField(service, "videoIndexPublisher", publisher);
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).status(ChatbotStatus.ACTIVE).isPublic(true).build();
        Video video = video(7L, chatbot, SyncStatus.FAILED);
        video.setFailureReason("network");
        video.setRetryCount(2);
        when(videoRepository.findById(7L)).thenReturn(Optional.of(video));
        when(videoRepository.save(video)).thenReturn(video);

        VideoResponseDTO response = service.retryVideo(7L, chatbot);

        assertEquals(7L, response.getVideoId());
        assertEquals(SyncStatus.PROCESSING, video.getSyncStatus());
        assertNull(video.getFailureReason());
        assertEquals(0, video.getRetryCount());
        assertEquals(ChatbotStatus.TRAINING, chatbot.getStatus());
        verify(videoRepository).save(video);
        verify(chatbotRepo).save(chatbot);
        verify(trainingSourceRepository).save(video.getTrainingSource());
        verify(publisher).publishForIndexing(List.of(video));
    }

    @Test
    void getThumbnailByYoutubeVideoIdReturnsThumbnail() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        when(videoRepository.findByYoutubeVideoId("abc"))
                .thenReturn(Optional.of(Video.builder().thumbnailUrl("thumb.jpg").build()));

        assertEquals("thumb.jpg", service.getThumbnailByYoutubeVideoId("abc"));
    }

    private static Video video(Long id, Chatbot chatbot, SyncStatus status) {
        TrainingSource source = TrainingSource.builder()
                .id(4L)
                .chatbot(chatbot)
                .syncStatus(SyncStatus.COMPLETED)
                .build();
        return Video.builder()
                .id(id)
                .trainingSource(source)
                .youtubeVideoId("abc")
                .title("Video")
                .syncStatus(status)
                .build();
    }

    private static VideoService service(VideoRepository videoRepository, YoutubeClientService youtubeClientService) {
        VideoService service = new VideoService();
        ReflectionTestUtils.setField(service, "videoRepository", videoRepository);
        ReflectionTestUtils.setField(service, "youtubeClientService", youtubeClientService);
        ReflectionTestUtils.setField(service, "fastApiService", mock(FastApiService.class));
        ReflectionTestUtils.setField(service, "influencerVerificationRepo", mock(InfluencerVerificationRepo.class));
        ReflectionTestUtils.setField(service, "trainingSourceRepository", mock(TrainingSourceRepository.class));
        ReflectionTestUtils.setField(service, "chatbotRepo", mock(ChatbotRepo.class));
        ReflectionTestUtils.setField(service, "messagingTemplate", mock(SimpMessagingTemplate.class));
        ReflectionTestUtils.setField(service, "videoIndexPublisher", mock(VideoIndexPublisher.class));
        return service;
    }
}
