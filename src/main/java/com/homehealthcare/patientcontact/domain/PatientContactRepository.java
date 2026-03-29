package com.homehealthcare.patientcontact.domain;

import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientContactRepository extends JpaRepository<PatientContact, UUID> {

    List<PatientContact> findAllByPatient_IdOrderByPrimaryContactDescEmergencyContactDescFullNameAsc(UUID patientId);

    long countByPatient_IdAndStatus(UUID patientId, PatientLifecycleStatus status);
}
