package com.homehealthcare.careprogression.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalInterventionRepository extends JpaRepository<GoalIntervention, UUID> {

    List<GoalIntervention> findAllByPatientGoal_IdOrderByTargetDateAscIdAsc(UUID patientGoalId);
}
