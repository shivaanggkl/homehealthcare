package com.homehealthcare.messaging.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationMarkerRepository extends JpaRepository<EscalationMarker, UUID> {

    Optional<EscalationMarker> findTopByThread_IdOrderByTaggedAtDesc(UUID threadId);
}
