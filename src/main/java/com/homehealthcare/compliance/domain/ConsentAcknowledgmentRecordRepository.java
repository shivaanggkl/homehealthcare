package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentAcknowledgmentRecordRepository extends JpaRepository<ConsentAcknowledgmentRecord, UUID> {

    Optional<ConsentAcknowledgmentRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<ConsentAcknowledgmentRecord> findAllByPatient_IdOrderByEffectiveAtDesc(UUID patientId);
}
