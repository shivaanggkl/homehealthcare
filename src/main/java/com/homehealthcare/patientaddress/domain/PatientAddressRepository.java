package com.homehealthcare.patientaddress.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientAddressRepository extends JpaRepository<PatientAddress, UUID> {

    Optional<PatientAddress> findByPatient_Id(UUID patientId);
}
