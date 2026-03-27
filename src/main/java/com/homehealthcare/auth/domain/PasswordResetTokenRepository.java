package com.homehealthcare.auth.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
            UUID userId,
            PasswordResetTokenStatus status);
}
