package com.homehealthcare.patientevent.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WoundHistoryEntryRepository extends JpaRepository<WoundHistoryEntry, UUID> {

    Optional<WoundHistoryEntry> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<WoundHistoryEntry> findAllByWoundRecord_IdOrderByCapturedAtAsc(UUID woundRecordId);

    List<WoundHistoryEntry> findAllByAgency_IdAndPatient_IdOrderByCapturedAtAsc(UUID agencyId, UUID patientId);
}
