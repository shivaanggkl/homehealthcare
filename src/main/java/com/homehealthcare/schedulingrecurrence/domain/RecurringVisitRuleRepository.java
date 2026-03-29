package com.homehealthcare.schedulingrecurrence.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringVisitRuleRepository extends JpaRepository<RecurringVisitRule, UUID> {

    Optional<RecurringVisitRule> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<RecurringVisitRule> findAllByPatient_IdOrderByEffectiveStartAsc(UUID patientId);
}
