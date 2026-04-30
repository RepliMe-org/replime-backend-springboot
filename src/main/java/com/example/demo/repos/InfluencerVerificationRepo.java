package com.example.demo.repos;

import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.VerificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfluencerVerificationRepo extends JpaRepository<InfluencerVerification, UUID> {
    Optional<InfluencerVerification> findByUserAndStatusIn(User user, List<VerificationStatus> statuses);

    InfluencerVerification findByChannelId(String channelId);

    InfluencerVerification findByUser(User influencer);
}
