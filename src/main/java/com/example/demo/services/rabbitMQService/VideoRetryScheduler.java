package com.example.demo.services.rabbitMQService;

import com.example.demo.entities.Video;
import com.example.demo.entities.utils.SyncStatus;
import com.example.demo.repos.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoRetryScheduler {

    private final VideoRepository videoRepository;
    private final VideoIndexPublisher publisher;

    /**
     * Every 60 seconds: find FAILED videos whose backoff time
     * has passed and re-queue them.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void retryFailedVideos() {

        List<Video> eligible = videoRepository
                .findRetryEligibleVideos();

        if (eligible.isEmpty()) return;

        log.info("RetryScheduler: found {} video(s) eligible for retry",
                eligible.size());

        for (Video video : eligible) {
            try {
                video.setSyncStatus(SyncStatus.PROCESSING);
                videoRepository.save(video);

                publisher.publishForRetry(video);

            } catch (Exception e) {
                log.error("Failed to re-queue video {}: {}",
                        video.getId(), e.getMessage());
                // Leave as FAILED — next scheduler tick will retry
            }
        }
    }
}
