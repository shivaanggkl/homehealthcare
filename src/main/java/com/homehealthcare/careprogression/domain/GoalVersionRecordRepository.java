package com.homehealthcare.careprogression.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalVersionRecordRepository extends JpaRepository<GoalVersionRecord, UUID> {

    List<GoalVersionRecord> findAllByPatientGoal_IdOrderByVersionNumberDesc(UUID patientGoalId);

    Optional<GoalVersionRecord> findTopByPatientGoal_IdOrderByVersionNumberDesc(UUID patientGoalId);
}
