package com.homehealthcare.schedulingworkflow.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitCancellationEventRepository extends JpaRepository<VisitCancellationEvent, UUID> {

    List<VisitCancellationEvent> findAllByVisitOccurrence_IdOrderByCancelledAtAsc(UUID visitOccurrenceId);
}
