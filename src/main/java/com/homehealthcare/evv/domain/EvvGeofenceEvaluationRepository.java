package com.homehealthcare.evv.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvvGeofenceEvaluationRepository extends JpaRepository<EvvGeofenceEvaluation, UUID> {

    List<EvvGeofenceEvaluation> findAllByVerificationSession_IdOrderByCreatedAtAsc(UUID verificationSessionId);
}
