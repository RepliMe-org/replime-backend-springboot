package com.example.demo.repos;

import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    boolean existsByYoutubeVideoId(String youtubeVideoId);

    List<Video> findByTrainingSource(TrainingSource trainingSource);
}
