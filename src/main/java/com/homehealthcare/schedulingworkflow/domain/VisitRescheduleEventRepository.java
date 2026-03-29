package com.homehealthcare.schedulingworkflow.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRescheduleEventRepository extends JpaRepository<VisitRescheduleEvent, UUID> {

    List<VisitRescheduleEvent> findAllByVisitOccurrence_IdOrderByRescheduledAtAsc(UUID visitOccurrenceId);
}
