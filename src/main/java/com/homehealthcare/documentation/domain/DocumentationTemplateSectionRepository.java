package com.homehealthcare.documentation.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationTemplateSectionRepository extends JpaRepository<DocumentationTemplateSection, UUID> {

    List<DocumentationTemplateSection> findAllByDocumentationTemplate_IdOrderBySortOrderAscTitleAsc(UUID documentationTemplateId);

    void deleteAllByDocumentationTemplate_Id(UUID documentationTemplateId);
}
