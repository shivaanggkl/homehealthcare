package com.homehealthcare.evv.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvvGeofenceEvaluationRepository extends JpaRepository<EvvGeofenceEvaluation, UUID> {

    Optional<EvvGeofenceEvaluation> findFirstByClockEvent_Id(UUID clockEventId);

    List<EvvGeofenceEvaluation> findAllByVerificationSession_IdOrderByCreatedAtAsc(UUID verificationSessionId);
}
