package com.homehealthcare.auth.domain;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLoginAttemptRepository extends JpaRepository<AuthLoginAttempt, UUID> {

    long countByIpAddressAndAttemptedAtAfter(String ipAddress, OffsetDateTime attemptedAt);

    long countByEmailAndAttemptedAtAfter(String email, OffsetDateTime attemptedAt);

    long countByEmailAndOutcomeInAndAttemptedAtAfter(String email, Collection<AuthLoginAttemptOutcome> outcomes, OffsetDateTime attemptedAt);

    Optional<AuthLoginAttempt> findFirstByEmailAndOutcomeOrderByAttemptedAtDesc(String email, AuthLoginAttemptOutcome outcome);

    Optional<AuthLoginAttempt> findFirstByEmailAndOutcomeInOrderByAttemptedAtDesc(
            String email,
            Collection<AuthLoginAttemptOutcome> outcomes);
}
