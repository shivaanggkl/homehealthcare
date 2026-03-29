package com.homehealthcare.evv.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvvVerificationSessionRepository extends JpaRepository<EvvVerificationSession, UUID> {

    Optional<EvvVerificationSession> findByIdAndAgency_Id(UUID id, UUID agencyId);

    Optional<EvvVerificationSession> findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(UUID visitOccurrenceId);

    Optional<EvvVerificationSession> findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByOpenedAtDesc(UUID visitOccurrenceId, UUID caregiverProfileId);

    List<EvvVerificationSession> findAllByAgency_IdOrderByOpenedAtDesc(UUID agencyId);
}
