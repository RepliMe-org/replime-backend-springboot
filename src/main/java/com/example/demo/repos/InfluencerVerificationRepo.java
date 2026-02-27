package com.example.demo.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.User;
import com.example.demo.entities.VerificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfluencerVerificationRepo extends JpaRepository<InfluencerVerification, UUID> {
    Optional<InfluencerVerification> findByUserAndStatusIn(User user, List<VerificationStatus> statuses);

    InfluencerVerification findByUser(User user);

    InfluencerVerification findByChannelUrl(String channelUrl);
}
