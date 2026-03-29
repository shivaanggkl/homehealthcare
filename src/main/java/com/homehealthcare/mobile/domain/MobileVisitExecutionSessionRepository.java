package com.homehealthcare.mobile.domain;

import com.homehealthcare.mobile.foundation.MobileExecutionSessionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileVisitExecutionSessionRepository extends JpaRepository<MobileVisitExecutionSession, UUID> {

    Optional<MobileVisitExecutionSession> findByIdAndAgency_Id(UUID id, UUID agencyId);

    Optional<MobileVisitExecutionSession> findFirstByVisitOccurrence_IdAndCaregiverProfile_IdAndExecutionStatusOrderByStartedAtDesc(
            UUID visitOccurrenceId,
            UUID caregiverProfileId,
            MobileExecutionSessionStatus executionStatus);

    Optional<MobileVisitExecutionSession> findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByStartedAtDesc(
            UUID visitOccurrenceId,
            UUID caregiverProfileId);

    List<MobileVisitExecutionSession> findAllByVisitOccurrence_IdOrderByStartedAtAsc(UUID visitOccurrenceId);
}
