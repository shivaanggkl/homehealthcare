package com.homehealthcare.schedulingvisit.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitOccurrenceRepository extends JpaRepository<VisitOccurrence, UUID> {

    Optional<VisitOccurrence> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<VisitOccurrence> findAllByAgency_IdOrderByPlannedStartAtAsc(UUID agencyId);

    List<VisitOccurrence> findAllByRecurringVisitRule_IdOrderByPlannedStartAtAsc(UUID recurringVisitRuleId);

    boolean existsByRecurringVisitRule_IdAndPlannedStartAt(UUID recurringVisitRuleId, OffsetDateTime plannedStartAt);
}
