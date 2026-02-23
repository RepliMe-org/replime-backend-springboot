package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "influencerVerifications")
public class InfluencerVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String channelId;

    private String channelUrl;

    private long subscriberCount;

    private String verificationToken;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status;
    // PENDING, VERIFIED, REJECTED

    private LocalDateTime requestedAt;

    private LocalDateTime verifiedAt;
}
