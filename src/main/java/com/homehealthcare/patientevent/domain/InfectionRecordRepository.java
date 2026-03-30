package com.homehealthcare.patientevent.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfectionRecordRepository extends JpaRepository<InfectionRecord, UUID> {

    Optional<InfectionRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<InfectionRecord> findAllByPatient_IdOrderByIdentifiedAtDesc(UUID patientId);

    List<InfectionRecord> findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(UUID agencyId, UUID patientId);
}
