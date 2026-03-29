package com.homehealthcare.evv.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationRequestRepository extends JpaRepository<EscalationRequest, UUID> {
}
