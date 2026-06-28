package com.example.demo.services;

import com.example.demo.dtos.TrainingSourceRequestDTO;
import com.example.demo.dtos.VideoResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.repos.TrainingSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for training-source creation, initial channel source creation,
// no-video failure, chatbot status/public side effects, indexing calls, and video total aggregation.
class TrainingSourceServiceTest {

    @Test
    void addTrainingSourceSavesVideosIndexesAndReturnsDtos() {
        TrainingSourceRepository repo = mock(TrainingSourceRepository.class);
        VideoService videoService = mock(VideoService.class);
        TrainingSourceService service = service(repo, videoService);
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).status(ChatbotStatus.ACTIVE).isPublic(true).build();
        TrainingSourceRequestDTO request = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.VIDEO)
                .sourceValue("https://youtube.com/watch?v=abc")
                .build();
        Video video = Video.builder().id(7L).youtubeVideoId("abc").build();
        List<VideoResponseDTO> responseDtos = List.of(VideoResponseDTO.builder().videoId(7L).build());
        when(repo.save(org.mockito.ArgumentMatchers.any(TrainingSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(videoService.fetchAndSaveVideosForTrainingSource(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any(TrainingSource.class),
                org.mockito.ArgumentMatchers.eq(chatbot))).thenReturn(List.of(video));
        when(videoService.mapToVideoResponseDTO(List.of(video))).thenReturn(responseDtos);

        List<VideoResponseDTO> response = service.addTrainingSourceToChatbot(request, chatbot);

        assertSame(responseDtos, response);
        assertEquals(ChatbotStatus.TRAINING, chatbot.getStatus());
        assertFalse(chatbot.isPublic());
        verify(videoService).indexSavedVideos(List.of(video), chatbot);
    }

    @Test
    void addTrainingSourceThrowsWhenNoVideosWereSaved() {
        TrainingSourceRepository repo = mock(TrainingSourceRepository.class);
        VideoService videoService = mock(VideoService.class);
        TrainingSourceService service = service(repo, videoService);
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).build();
        TrainingSourceRequestDTO request = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.VIDEO)
                .sourceValue("https://youtube.com/watch?v=abc")
                .build();
        when(repo.save(org.mockito.ArgumentMatchers.any(TrainingSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(videoService.fetchAndSaveVideosForTrainingSource(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any(TrainingSource.class),
                org.mockito.ArgumentMatchers.eq(chatbot))).thenReturn(List.of());

        TrainingSourceException exception = assertThrows(
                TrainingSourceException.class,
                () -> service.addTrainingSourceToChatbot(request, chatbot));

        assertEquals("No videos were saved for the provided training source.", exception.getMessage());
    }

    @Test
    void addInitialTrainingSourceFetchesChannelVideosAndIndexes() {
        TrainingSourceRepository repo = mock(TrainingSourceRepository.class);
        VideoService videoService = mock(VideoService.class);
        TrainingSourceService service = service(repo, videoService);
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).status(ChatbotStatus.ACTIVE).isPublic(true).build();
        Video video = Video.builder().id(7L).youtubeVideoId("abc").build();
        when(repo.save(org.mockito.ArgumentMatchers.any(TrainingSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(videoService.getChannelVideos(org.mockito.ArgumentMatchers.eq("UC123"), org.mockito.ArgumentMatchers.any(TrainingSource.class)))
                .thenReturn(List.of(video));

        service.addInitialTrainingSource(chatbot, "UC123");

        assertEquals(ChatbotStatus.TRAINING, chatbot.getStatus());
        assertFalse(chatbot.isPublic());
        verify(videoService).indexSavedVideos(List.of(video), chatbot);
    }

    @Test
    void getTotalNumberOfVideosOfChatbotSumsTrainingSourceVideos() {
        TrainingSourceRepository repo = mock(TrainingSourceRepository.class);
        TrainingSourceService service = service(repo, mock(VideoService.class));
        UUID chatbotId = UUID.randomUUID();
        TrainingSource first = TrainingSource.builder()
                .videos(List.of(Video.builder().build(), Video.builder().build()))
                .build();
        TrainingSource second = TrainingSource.builder()
                .videos(List.of(Video.builder().build()))
                .build();
        when(repo.findByChatbotId(chatbotId)).thenReturn(List.of(first, second));

        int total = service.getTotalNumberOfVideosOfChatbot(chatbotId);

        assertEquals(3, total);
    }

    private static TrainingSourceService service(TrainingSourceRepository repo, VideoService videoService) {
        TrainingSourceService service = new TrainingSourceService();
        ReflectionTestUtils.setField(service, "trainingSourceRepository", repo);
        ReflectionTestUtils.setField(service, "videoService", videoService);
        return service;
    }
}
