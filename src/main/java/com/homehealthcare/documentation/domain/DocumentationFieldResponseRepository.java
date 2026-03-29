package com.homehealthcare.documentation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationFieldResponseRepository extends JpaRepository<DocumentationFieldResponse, UUID> {

    List<DocumentationFieldResponse> findAllByDocumentationRecord_IdOrderByFieldKeyAsc(UUID documentationRecordId);

    Optional<DocumentationFieldResponse> findByDocumentationRecord_IdAndTemplateField_Id(UUID documentationRecordId, UUID templateFieldId);
}
