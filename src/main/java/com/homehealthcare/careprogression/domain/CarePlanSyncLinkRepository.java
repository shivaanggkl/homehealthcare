package com.homehealthcare.careprogression.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarePlanSyncLinkRepository extends JpaRepository<CarePlanSyncLink, UUID> {

    List<CarePlanSyncLink> findAllByPatientGoal_IdOrderByCareplanIdentifierAsc(UUID patientGoalId);
}
