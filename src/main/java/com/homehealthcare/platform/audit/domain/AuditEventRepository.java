package com.homehealthcare.platform.audit.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    List<AuditEvent> findAllByActorIdOrderByOccurredAtAsc(UUID actorId);

    List<AuditEvent> findAllByAgencyIdOrderByOccurredAtAsc(UUID agencyId);

    List<AuditEvent> findAllByOutcomeOrderByOccurredAtAsc(AuditEventOutcome outcome);
}
