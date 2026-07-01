package com.example.demo.services;

import com.example.demo.dtos.DeleteVideoRequestDTO;
import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.VideoResponseDTO;
import com.example.demo.dtos.internal.UpdateVideoStatusRequestDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.User;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.FailedStage;
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
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        when(videoRepository.existsByYoutubeVideoIdAndSyncStatusNot("abc", SyncStatus.DELETED)).thenReturn(true);

        TrainingSourceException exception = assertThrows(
                TrainingSourceException.class,
                () -> service.fetchAndSaveVideosForTrainingSource(request, TrainingSource.builder().build(), Chatbot.builder().build()));

        assertEquals("This video is already in your knowledge base", exception.getMessage());
    }

    @Test
    void fetchAndSaveVideosForTrainingSourceSavesPlaylistVideosThatAreNotDuplicates() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        YoutubeClientService youtubeClientService = mock(YoutubeClientService.class);
        VideoService service = service(videoRepository, youtubeClientService);
        TrainingSource trainingSource = TrainingSource.builder().id(4L).build();
        TrainingSourceRequestDTO request = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.PLAYLIST)
                .sourceValue("https://youtube.com/playlist?list=pl")
                .build();
        Video video = Video.builder().youtubeVideoId("abc").title("Video").build();
        when(youtubeClientService.extractPlaylistId(request.getSourceValue())).thenReturn("pl");
        when(youtubeClientService.getVideosFromPlaylist("pl")).thenReturn(List.of(video));
        when(videoRepository.existsByYoutubeVideoIdAndSyncStatusNot("abc", SyncStatus.DELETED)).thenReturn(false);
        when(videoRepository.save(video)).thenReturn(video);

        List<Video> saved = service.fetchAndSaveVideosForTrainingSource(
                request, trainingSource, Chatbot.builder().build());

        assertEquals(List.of(video), saved);
        assertEquals(trainingSource, video.getTrainingSource());
        verify(videoRepository).save(video);
    }

    @Test
    void indexSavedVideosPublishesSavedVideos() {
        VideoIndexPublisher publisher = mock(VideoIndexPublisher.class);
        VideoService service = service(mock(VideoRepository.class), mock(YoutubeClientService.class));
        ReflectionTestUtils.setField(service, "videoIndexPublisher", publisher);
        List<Video> videos = List.of(Video.builder().youtubeVideoId("abc").build());

        service.indexSavedVideos(videos, Chatbot.builder().id(UUID.randomUUID()).build());

        verify(publisher).publishForIndexing(videos);
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
        when(videoRepository.findByIdAndSyncStatusNot(99L, SyncStatus.DELETED)).thenReturn(Optional.empty());

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
        when(videoRepository.findByIdAndSyncStatusNot(7L, SyncStatus.DELETED)).thenReturn(Optional.of(video));

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
        when(videoRepository.findByIdAndSyncStatusNot(7L, SyncStatus.DELETED)).thenReturn(Optional.of(video));
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
        when(videoRepository.findByYoutubeVideoIdAndSyncStatusNot("abc", SyncStatus.DELETED))
                .thenReturn(Optional.of(Video.builder().thumbnailUrl("thumb.jpg").build()));

        assertEquals("thumb.jpg", service.getThumbnailByYoutubeVideoId("abc"));
    }

    @Test
    void deleteVideoFromChatbotSendsOwningChatbotIdToFastApi() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        FastApiService fastApiService = mock(FastApiService.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        ReflectionTestUtils.setField(service, "fastApiService", fastApiService);
        UUID chatbotId = UUID.randomUUID();
        Chatbot chatbot = Chatbot.builder().id(chatbotId).build();
        Video video = video(7L, chatbot, SyncStatus.COMPLETED);
        video.setYoutubeVideoId("yt-123");
        when(videoRepository.findByYoutubeVideoIdAndSyncStatusNot("yt-123", SyncStatus.DELETED)).thenReturn(Optional.of(video));
        when(fastApiService.deleteVideoChunks(any(DeleteVideoRequestDTO.class)))
                .thenReturn(java.util.Map.of("deleted_chunks", 4));

        service.deleteVideoFromChatbot("yt-123", Chatbot.builder().id(chatbotId).build());

        ArgumentCaptor<DeleteVideoRequestDTO> captor = ArgumentCaptor.forClass(DeleteVideoRequestDTO.class);
        verify(fastApiService).deleteVideoChunks(captor.capture());
        assertEquals("yt-123", captor.getValue().getYoutubeVideoId());
        assertEquals(chatbotId.toString(), captor.getValue().getChatbotId());
        assertEquals(SyncStatus.DELETED, video.getSyncStatus());
        verify(videoRepository).save(video);
    }

    @Test
    void deleteVideoFromChatbotThrowsWhenVideoBelongsToAnotherChatbot() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        Chatbot owner = Chatbot.builder().id(UUID.randomUUID()).build();
        Chatbot requester = Chatbot.builder().id(UUID.randomUUID()).build();
        Video video = video(7L, owner, SyncStatus.COMPLETED);
        when(videoRepository.findByYoutubeVideoIdAndSyncStatusNot("yt-123", SyncStatus.DELETED))
                .thenReturn(Optional.of(video));

        TrainingSourceException exception = assertThrows(
                TrainingSourceException.class,
                () -> service.deleteVideoFromChatbot("yt-123", requester));

        assertEquals("FORBIDDEN", exception.getErrorCode());
        assertEquals("This video does not belong to your chatbot", exception.getMessage());
    }

    @Test
    void deleteIndexedChunksForChatbotSkipsVideosWithoutYoutubeId() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        FastApiService fastApiService = mock(FastApiService.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        ReflectionTestUtils.setField(service, "fastApiService", fastApiService);
        UUID chatbotId = UUID.randomUUID();
        Chatbot chatbot = Chatbot.builder().id(chatbotId).build();
        TrainingSource source = TrainingSource.builder().id(4L).chatbot(chatbot).build();
        chatbot.setTrainingSources(List.of(source));
        Video skipped = Video.builder().id(1L).trainingSource(source).syncStatus(SyncStatus.COMPLETED).build();
        Video deleted = Video.builder()
                .id(2L)
                .trainingSource(source)
                .youtubeVideoId("yt-123")
                .syncStatus(SyncStatus.COMPLETED)
                .build();
        when(videoRepository.findByTrainingSourceAndSyncStatusNot(source, SyncStatus.DELETED))
                .thenReturn(List.of(skipped, deleted));
        when(fastApiService.deleteVideoChunks(any(DeleteVideoRequestDTO.class)))
                .thenReturn(java.util.Map.of("deleted_chunks", 3));

        service.deleteIndexedChunksForChatbot(chatbot);

        ArgumentCaptor<DeleteVideoRequestDTO> captor = ArgumentCaptor.forClass(DeleteVideoRequestDTO.class);
        verify(fastApiService).deleteVideoChunks(captor.capture());
        assertEquals("yt-123", captor.getValue().getYoutubeVideoId());
        assertEquals(chatbotId.toString(), captor.getValue().getChatbotId());
    }

    @Test
    void updateVideoStatusCompletedClearsFailureStateAndFinalizesTrainingSource() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        TrainingSourceRepository trainingSourceRepository = mock(TrainingSourceRepository.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        VideoService service = service(videoRepository, mock(YoutubeClientService.class));
        ReflectionTestUtils.setField(service, "trainingSourceRepository", trainingSourceRepository);
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        ReflectionTestUtils.setField(service, "messagingTemplate", messagingTemplate);
        UUID chatbotId = UUID.randomUUID();
        ChatbotConfig config = ChatbotConfig.builder().build();
        Chatbot chatbot = Chatbot.builder()
                .id(chatbotId)
                .status(ChatbotStatus.TRAINING)
                .isPublic(false)
                .config(config)
                .influencer(User.builder().email("creator@example.com").build())
                .build();
        TrainingSource source = TrainingSource.builder()
                .id(4L)
                .chatbot(chatbot)
                .syncStatus(SyncStatus.PROCESSING)
                .build();
        Video video = Video.builder()
                .id(7L)
                .trainingSource(source)
                .youtubeVideoId("yt-123")
                .syncStatus(SyncStatus.PROCESSING)
                .failedStage(FailedStage.CHUNKING)
                .failureReason("old failure")
                .build();
        source.setVideos(List.of(video));
        when(videoRepository.findByYoutubeVideoIdAndSyncStatusNot("yt-123", SyncStatus.DELETED))
                .thenReturn(Optional.of(video));

        UpdateVideoStatusRequestDTO request = new UpdateVideoStatusRequestDTO();
        request.setStatus("COMPLETED");
        request.setDescription("AI description");

        service.updateVideoStatus("yt-123", request);

        assertEquals(SyncStatus.COMPLETED, video.getSyncStatus());
        assertNull(video.getFailureReason());
        assertNull(video.getFailedStage());
        assertEquals("AI description", config.getAiGeneratedDescription());
        assertEquals(SyncStatus.COMPLETED, source.getSyncStatus());
        assertEquals(ChatbotStatus.ACTIVE, chatbot.getStatus());
        assertTrue(chatbot.isPublic());
        verify(videoRepository).save(video);
        verify(trainingSourceRepository).save(source);
        verify(chatbotRepo, times(2)).save(chatbot);
        verify(messagingTemplate, times(2)).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/chatbot/" + chatbotId + "/sync-status"),
                any(Object.class));
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
