package com.homehealthcare.careprogression.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalProgressNoteRepository extends JpaRepository<GoalProgressNote, UUID> {

    List<GoalProgressNote> findAllByPatientGoal_IdOrderByCapturedAtDesc(UUID patientGoalId);
}
