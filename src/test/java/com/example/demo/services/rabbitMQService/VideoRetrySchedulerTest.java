package com.example.demo.services.rabbitMQService;

import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.repos.VideoRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Coverage criteria: retry scheduler unit coverage for empty and eligible retry batches,
// verifying repository lookup, PROCESSING status transition, save-before-publish ordering, and per-video exception isolation.
class VideoRetrySchedulerTest {

    @Test
    void retryFailedVideosDoesNothingWhenNoVideosAreEligible() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoIndexPublisher publisher = mock(VideoIndexPublisher.class);
        VideoRetryScheduler scheduler = new VideoRetryScheduler(videoRepository, publisher);
        when(videoRepository.findRetryEligibleVideos()).thenReturn(List.of());

        scheduler.retryFailedVideos();

        verify(videoRepository).findRetryEligibleVideos();
        verifyNoInteractions(publisher);
    }

    @Test
    void retryFailedVideosMarksEachVideoProcessingSavesThenPublishes() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoIndexPublisher publisher = mock(VideoIndexPublisher.class);
        VideoRetryScheduler scheduler = new VideoRetryScheduler(videoRepository, publisher);
        Video first = failedVideo(1L);
        Video second = failedVideo(2L);
        when(videoRepository.findRetryEligibleVideos()).thenReturn(List.of(first, second));

        scheduler.retryFailedVideos();

        assertEquals(SyncStatus.PROCESSING, first.getSyncStatus());
        assertEquals(SyncStatus.PROCESSING, second.getSyncStatus());
        var inOrder = inOrder(videoRepository, publisher);
        inOrder.verify(videoRepository).findRetryEligibleVideos();
        inOrder.verify(videoRepository).save(first);
        inOrder.verify(publisher).publishForRetry(first);
        inOrder.verify(videoRepository).save(second);
        inOrder.verify(publisher).publishForRetry(second);
    }

    @Test
    void retryFailedVideosContinuesWhenOnePublishFails() {
        VideoRepository videoRepository = mock(VideoRepository.class);
        VideoIndexPublisher publisher = mock(VideoIndexPublisher.class);
        VideoRetryScheduler scheduler = new VideoRetryScheduler(videoRepository, publisher);
        Video first = failedVideo(1L);
        Video second = failedVideo(2L);
        when(videoRepository.findRetryEligibleVideos()).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("rabbit down")).when(publisher).publishForRetry(first);

        scheduler.retryFailedVideos();

        assertEquals(SyncStatus.PROCESSING, first.getSyncStatus());
        assertEquals(SyncStatus.PROCESSING, second.getSyncStatus());
        verify(videoRepository).save(first);
        verify(videoRepository).save(second);
        verify(publisher).publishForRetry(first);
        verify(publisher).publishForRetry(second);
    }

    private static Video failedVideo(Long id) {
        Video video = new Video();
        video.setId(id);
        video.setSyncStatus(SyncStatus.FAILED);
        return video;
    }
}
