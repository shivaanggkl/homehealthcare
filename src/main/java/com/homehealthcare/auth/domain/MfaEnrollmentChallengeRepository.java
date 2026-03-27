package com.homehealthcare.auth.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaEnrollmentChallengeRepository extends JpaRepository<MfaEnrollmentChallenge, UUID> {

    Optional<MfaEnrollmentChallenge> findByToken(String token);

    Optional<MfaEnrollmentChallenge> findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
            UUID userId,
            MfaEnrollmentChallengeStatus status);
}
