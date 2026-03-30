package com.homehealthcare.patientevent.domain;

import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.PatientEventEscalationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientEventEscalationRecordRepository extends JpaRepository<PatientEventEscalationRecord, UUID> {

    Optional<PatientEventEscalationRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByAgency_IdAndTargetTypeAndTargetIdAndStatus(
            UUID agencyId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventEscalationStatus status);

    List<PatientEventEscalationRecord> findAllByAgency_IdAndPatient_IdOrderByEscalatedAtAsc(UUID agencyId, UUID patientId);

    List<PatientEventEscalationRecord> findAllByAgency_IdAndTargetTypeAndTargetIdOrderByEscalatedAtAsc(
            UUID agencyId,
            Epic12PatientEventTargetType targetType,
            UUID targetId);
}
