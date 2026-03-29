package com.homehealthcare.patient.domain;

import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    List<Patient> findAllByAgency_IdOrderByLastNameAscFirstNameAsc(UUID agencyId);

    boolean existsByAgency_IdAndFirstNameAndLastNameAndDateOfBirth(
            UUID agencyId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth);

    boolean existsByAgency_IdAndFirstNameAndLastNameAndDateOfBirthAndIdNot(
            UUID agencyId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            UUID id);

    long countByAgency_IdAndStatus(UUID agencyId, PatientLifecycleStatus status);
}
