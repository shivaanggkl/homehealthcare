package com.homehealthcare.documentation.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDocumentationRecordRepository extends JpaRepository<VisitDocumentationRecord, UUID> {

    Optional<VisitDocumentationRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByVisitOccurrence_IdAndSelectedTemplate_Id(UUID visitOccurrenceId, UUID selectedTemplateId);

    Optional<VisitDocumentationRecord> findByVisitOccurrence_IdAndSelectedTemplate_Id(UUID visitOccurrenceId, UUID selectedTemplateId);

    List<VisitDocumentationRecord> findAllByAgency_IdOrderByLastSavedAtDesc(UUID agencyId);

    List<VisitDocumentationRecord> findAllByAgency_IdAndPatient_IdOrderByLastSavedAtDesc(UUID agencyId, UUID patientId);

    List<VisitDocumentationRecord> findAllByVisitOccurrence_IdOrderByLastSavedAtDesc(UUID visitOccurrenceId);

    List<VisitDocumentationRecord> findAllByAgency_IdAndLastSavedAtBetweenOrderByLastSavedAtDesc(
            UUID agencyId,
            OffsetDateTime from,
            OffsetDateTime to);
}
