package com.homehealthcare.patientdiagnosis.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientDiagnosisConditionRepository extends JpaRepository<PatientDiagnosisCondition, UUID> {

    List<PatientDiagnosisCondition> findAllByPatient_IdOrderByPrimaryConditionDescDescriptionAsc(UUID patientId);

    long countByPatient_IdAndPrimaryConditionTrueAndIdNot(UUID patientId, UUID id);

    long countByPatient_IdAndPrimaryConditionTrue(UUID patientId);
}
