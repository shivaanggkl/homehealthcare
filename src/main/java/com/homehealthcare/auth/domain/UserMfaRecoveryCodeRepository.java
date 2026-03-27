package com.homehealthcare.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserMfaRecoveryCodeRepository extends JpaRepository<UserMfaRecoveryCode, UUID> {

    long countByUser_IdAndConsumedAtIsNull(UUID userId);

    @Transactional
    void deleteAllByUser_Id(UUID userId);

    List<UserMfaRecoveryCode> findAllByUser_IdOrderByOrdinalAsc(UUID userId);

    Optional<UserMfaRecoveryCode> findByUser_IdAndCodeHashAndConsumedAtIsNull(UUID userId, String codeHash);
}
