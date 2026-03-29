package com.homehealthcare.evv.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvvClockEventRepository extends JpaRepository<EvvClockEvent, UUID> {

    Optional<EvvClockEvent> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByVerificationSession_IdAndEventType(UUID verificationSessionId, EvvClockEventType eventType);

    List<EvvClockEvent> findAllByVerificationSession_IdOrderByCapturedAtAsc(UUID verificationSessionId);
}
