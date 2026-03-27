package com.homehealthcare.auth.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthMfaLoginChallengeRepository extends JpaRepository<AuthMfaLoginChallenge, UUID> {

    Optional<AuthMfaLoginChallenge> findByToken(String token);

    Optional<AuthMfaLoginChallenge> findFirstByUser_IdAndStatusOrderByCreatedAtDesc(UUID userId, AuthMfaLoginChallengeStatus status);
}
