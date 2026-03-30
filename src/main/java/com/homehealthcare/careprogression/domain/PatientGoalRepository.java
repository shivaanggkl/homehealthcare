package com.homehealthcare.careprogression.domain;

import com.homehealthcare.careprogression.foundation.PatientGoalLifecycleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientGoalRepository extends JpaRepository<PatientGoal, UUID> {

    List<PatientGoal> findAllByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    List<PatientGoal> findAllByAgency_IdAndPatient_IdOrderByCreatedAtDesc(UUID agencyId, UUID patientId);

    List<PatientGoal> findAllByAgency_IdAndStatusOrderByCreatedAtDesc(UUID agencyId, PatientGoalLifecycleStatus status);
}
