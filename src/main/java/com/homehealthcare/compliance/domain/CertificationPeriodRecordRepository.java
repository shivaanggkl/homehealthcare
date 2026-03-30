package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationPeriodRecordRepository extends JpaRepository<CertificationPeriodRecord, UUID> {

    Optional<CertificationPeriodRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CertificationPeriodRecord> findAllByPatient_IdOrderByStartDateDesc(UUID patientId);
}
