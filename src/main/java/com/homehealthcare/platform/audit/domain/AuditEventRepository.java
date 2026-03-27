package com.homehealthcare.platform.audit.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findAllByActorIdOrderByOccurredAtAsc(UUID actorId);

    List<AuditEvent> findAllByAgencyIdOrderByOccurredAtAsc(UUID agencyId);

    List<AuditEvent> findAllByOutcomeOrderByOccurredAtAsc(AuditEventOutcome outcome);
}
