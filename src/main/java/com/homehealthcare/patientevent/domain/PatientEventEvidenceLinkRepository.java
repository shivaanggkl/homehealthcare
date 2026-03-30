package com.homehealthcare.patientevent.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientEventEvidenceLinkRepository extends JpaRepository<PatientEventEvidenceLink, UUID> {

    Optional<PatientEventEvidenceLink> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<PatientEventEvidenceLink> findAllByTargetTypeAndTargetIdOrderByLinkedAtAsc(Enum<?> targetType, UUID targetId);

    List<PatientEventEvidenceLink> findAllByAgency_IdAndPatient_IdOrderByLinkedAtAsc(UUID agencyId, UUID patientId);

    List<PatientEventEvidenceLink> findAllByAgency_IdAndTargetTypeAndTargetIdOrderByLinkedAtAsc(UUID agencyId, Enum<?> targetType, UUID targetId);
}
