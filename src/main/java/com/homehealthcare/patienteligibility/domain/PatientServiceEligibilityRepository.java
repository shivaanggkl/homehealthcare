package com.homehealthcare.patienteligibility.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientServiceEligibilityRepository extends JpaRepository<PatientServiceEligibility, UUID> {

    List<PatientServiceEligibility> findAllByPatient_IdOrderByEffectiveFromDesc(UUID patientId);
}
