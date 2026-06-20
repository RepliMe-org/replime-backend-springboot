package com.example.demo.repos;

import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    boolean existsByYoutubeVideoId(String youtubeVideoId);

    List<Video> findByTrainingSource(TrainingSource trainingSource);

    Optional<Video> findByYoutubeVideoId(String youtubeVideoId);

    @Query(value = """
    SELECT * FROM videos
    WHERE sync_status = 'FAILED'
      AND retry_count < max_retries
      AND (
            last_retry_at IS NULL
            OR last_retry_at + (POWER(retry_count, 2) * INTERVAL '1 minute')
               <= NOW()
          )
    ORDER BY last_retry_at ASC NULLS FIRST
    LIMIT 50
    """, nativeQuery = true)
    List<Video> findRetryEligibleVideos();
}
