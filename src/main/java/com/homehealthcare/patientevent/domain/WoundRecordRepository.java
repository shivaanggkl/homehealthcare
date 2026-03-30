package com.homehealthcare.patientevent.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WoundRecordRepository extends JpaRepository<WoundRecord, UUID> {

    Optional<WoundRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<WoundRecord> findAllByPatient_IdOrderByIdentifiedAtDesc(UUID patientId);

    List<WoundRecord> findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(UUID agencyId, UUID patientId);
}
