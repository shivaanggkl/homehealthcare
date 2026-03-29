package com.homehealthcare.evv.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvvVerificationSessionRepository extends JpaRepository<EvvVerificationSession, UUID> {

    Optional<EvvVerificationSession> findByIdAndAgency_Id(UUID id, UUID agencyId);

    Optional<EvvVerificationSession> findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByOpenedAtDesc(UUID visitOccurrenceId, UUID caregiverProfileId);
}
