package com.homehealthcare.documentation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationTaskResponseRepository extends JpaRepository<DocumentationTaskResponse, UUID> {

    List<DocumentationTaskResponse> findAllByDocumentationRecord_IdOrderBySortOrderAscTaskTitleAsc(UUID documentationRecordId);

    Optional<DocumentationTaskResponse> findByDocumentationRecord_IdAndTemplateTask_Id(UUID documentationRecordId, UUID templateTaskId);
}
