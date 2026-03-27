package com.homehealthcare.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession> findByIdAndRevokedAtIsNull(UUID id);

    Optional<AuthSession> findByAccessTokenHashAndRevokedAtIsNull(String accessTokenHash);

    Optional<AuthSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    List<AuthSession> findAllByUser_IdAndRevokedAtIsNull(UUID userId);

    List<AuthSession> findAllByUser_IdAndRevokedAtIsNullAndIdNot(UUID userId, UUID id);

    List<AuthSession> findTop10ByUser_IdOrderByLastActivityAtDesc(UUID userId);

    Optional<AuthSession> findByIdAndUser_Id(UUID id, UUID userId);
}
