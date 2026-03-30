package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRiskReminderRepository extends JpaRepository<PatientRiskReminder, UUID> {

    Optional<PatientRiskReminder> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<PatientRiskReminder> findAllByPatient_IdOrderByEffectiveAtDesc(UUID patientId);
}
