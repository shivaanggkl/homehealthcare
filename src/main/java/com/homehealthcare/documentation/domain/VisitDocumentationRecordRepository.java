package com.homehealthcare.documentation.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDocumentationRecordRepository extends JpaRepository<VisitDocumentationRecord, UUID> {

    Optional<VisitDocumentationRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByVisitOccurrence_IdAndSelectedTemplate_Id(UUID visitOccurrenceId, UUID selectedTemplateId);

    Optional<VisitDocumentationRecord> findByVisitOccurrence_IdAndSelectedTemplate_Id(UUID visitOccurrenceId, UUID selectedTemplateId);
}
