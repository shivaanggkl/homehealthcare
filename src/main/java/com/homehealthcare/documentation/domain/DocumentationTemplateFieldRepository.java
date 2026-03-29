package com.homehealthcare.documentation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationTemplateFieldRepository extends JpaRepository<DocumentationTemplateField, UUID> {

    List<DocumentationTemplateField> findAllByDocumentationTemplate_IdOrderBySortOrderAscLabelAsc(UUID documentationTemplateId);

    Optional<DocumentationTemplateField> findByIdAndAgency_Id(UUID id, UUID agencyId);

    void deleteAllByDocumentationTemplate_Id(UUID documentationTemplateId);
}
