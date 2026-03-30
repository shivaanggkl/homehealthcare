package com.homehealthcare.patientevent.domain;

import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.PatientEventFollowUpStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientEventFollowUpAssignmentRepository extends JpaRepository<PatientEventFollowUpAssignment, UUID> {

    Optional<PatientEventFollowUpAssignment> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByAgency_IdAndTargetTypeAndTargetIdAndStatus(
            UUID agencyId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventFollowUpStatus status);

    List<PatientEventFollowUpAssignment> findAllByAgency_IdAndPatient_IdOrderByAssignedAtAsc(UUID agencyId, UUID patientId);

    List<PatientEventFollowUpAssignment> findAllByAgency_IdAndStatusOrderByDueAtAsc(UUID agencyId, PatientEventFollowUpStatus status);

    List<PatientEventFollowUpAssignment> findAllByAgency_IdAndTargetTypeAndTargetIdOrderByAssignedAtAsc(
            UUID agencyId,
            Epic12PatientEventTargetType targetType,
            UUID targetId);
}
