package com.homehealthcare.documentationtemplate.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "documentation_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationTemplate extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 64)
    private DocumentationTemplateType templateType;

    @Column(name = "structured_definition_json", nullable = false, columnDefinition = "clob")
    private String structuredDefinitionJson;

    @Column(name = "version", nullable = false)
    private int version;

    @Builder
    private DocumentationTemplate(
            UUID id,
            Agency agency,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int version) {
        this.id = id;
        assignAgency(agency);
        this.name = name;
        this.code = code;
        this.templateType = templateType;
        this.structuredDefinitionJson = structuredDefinitionJson;
        this.version = version;
    }

    public static DocumentationTemplate createDraft(
            Agency agency,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int displayOrder) {
        DocumentationTemplate template = DocumentationTemplate.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name(name)
                .code(code)
                .templateType(templateType)
                .structuredDefinitionJson(structuredDefinitionJson)
                .version(1)
                .build();
        template.updateDisplayOrder(displayOrder);
        template.markDraft();
        return template;
    }

    public void updateDraft(
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int displayOrder) {
        this.name = name;
        this.code = code;
        this.templateType = templateType;
        this.structuredDefinitionJson = structuredDefinitionJson;
        this.version = this.version + 1;
        updateDisplayOrder(displayOrder);
        markDraft();
    }

    public void publish() {
        activate();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = required(name);
        code = required(code).toUpperCase(Locale.ROOT);
        structuredDefinitionJson = required(structuredDefinitionJson);
        Objects.requireNonNull(templateType, "templateType must not be null");
        if (version <= 0) {
            throw new IllegalArgumentException("version must be greater than 0");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }
}
