package com.homehealthcare.documentation.domain;

import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "documentation_template_sections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationTemplateSection extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documentation_template_id", nullable = false)
    private DocumentationTemplate documentationTemplate;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private DocumentationTemplateSection(
            UUID id,
            DocumentationTemplate documentationTemplate,
            String sectionKey,
            String title,
            String helpText,
            int sortOrder) {
        this.id = id;
        assignDocumentationTemplate(documentationTemplate);
        this.sectionKey = sectionKey;
        this.title = title;
        this.helpText = helpText;
        this.sortOrder = sortOrder;
    }

    public static DocumentationTemplateSection create(
            DocumentationTemplate documentationTemplate,
            String sectionKey,
            String title,
            String helpText,
            int sortOrder) {
        return DocumentationTemplateSection.builder()
                .id(UUID.randomUUID())
                .documentationTemplate(documentationTemplate)
                .sectionKey(sectionKey)
                .title(title)
                .helpText(helpText)
                .sortOrder(sortOrder)
                .build();
    }

    public UUID getDocumentationTemplateId() {
        return documentationTemplate == null ? null : documentationTemplate.getId();
    }

    private void assignDocumentationTemplate(DocumentationTemplate documentationTemplate) {
        this.documentationTemplate = Objects.requireNonNull(documentationTemplate, "documentationTemplate must not be null");
        assignAgency(documentationTemplate.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        sectionKey = required(sectionKey);
        title = required(title);
        helpText = optional(helpText);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be greater than or equal to 0");
        }
        assignDocumentationTemplate(documentationTemplate);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
