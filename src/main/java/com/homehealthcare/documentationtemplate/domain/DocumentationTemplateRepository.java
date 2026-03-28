package com.homehealthcare.documentationtemplate.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentationTemplateRepository extends JpaRepository<DocumentationTemplate, UUID> {

    boolean existsByAgency_IdAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndCode(UUID agencyId, String code);

    boolean existsByAgency_IdAndNameAndIdNot(UUID agencyId, String name, UUID id);

    boolean existsByAgency_IdAndCodeAndIdNot(UUID agencyId, String code, UUID id);
}
