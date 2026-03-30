package com.homehealthcare.patientevent.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRecordRepository extends JpaRepository<IncidentRecord, UUID> {

    Optional<IncidentRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<IncidentRecord> findAllByPatient_IdOrderByOccurredAtDesc(UUID patientId);

    List<IncidentRecord> findAllByAgency_IdAndPatient_IdOrderByOccurredAtAsc(UUID agencyId, UUID patientId);
}
