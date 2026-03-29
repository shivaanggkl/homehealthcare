package com.homehealthcare.documentation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationTemplateTaskRepository extends JpaRepository<DocumentationTemplateTask, UUID> {

    List<DocumentationTemplateTask> findAllByDocumentationTemplate_IdOrderBySortOrderAscIdAsc(UUID documentationTemplateId);

    Optional<DocumentationTemplateTask> findByIdAndAgency_Id(UUID id, UUID agencyId);

    void deleteAllByDocumentationTemplate_Id(UUID documentationTemplateId);
}
