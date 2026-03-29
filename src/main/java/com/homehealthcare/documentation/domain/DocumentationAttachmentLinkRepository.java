package com.homehealthcare.documentation.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationAttachmentLinkRepository extends JpaRepository<DocumentationAttachmentLink, UUID> {

    List<DocumentationAttachmentLink> findAllByDocumentationRecord_IdOrderByLinkedAtAsc(UUID documentationRecordId);
}
