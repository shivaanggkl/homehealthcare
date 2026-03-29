package com.homehealthcare.documentation.domain;

import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
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
@Table(name = "documentation_template_tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationTemplateTask extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documentation_template_id", nullable = false)
    private com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private DocumentationTemplateSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;

    @Column(name = "title_override", length = 200)
    private String titleOverride;

    @Column(name = "description_override", length = 1000)
    private String descriptionOverride;

    @Column(name = "required_override")
    private Boolean requiredOverride;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private DocumentationTemplateTask(
            UUID id,
            com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate,
            DocumentationTemplateSection section,
            TaskTemplate taskTemplate,
            String titleOverride,
            String descriptionOverride,
            Boolean requiredOverride,
            int sortOrder) {
        this.id = id;
        assignDocumentationTemplate(documentationTemplate);
        assignSection(section);
        assignTaskTemplate(taskTemplate);
        this.titleOverride = titleOverride;
        this.descriptionOverride = descriptionOverride;
        this.requiredOverride = requiredOverride;
        this.sortOrder = sortOrder;
    }

    public static DocumentationTemplateTask create(
            com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate,
            DocumentationTemplateSection section,
            TaskTemplate taskTemplate,
            String titleOverride,
            String descriptionOverride,
            Boolean requiredOverride,
            int sortOrder) {
        return DocumentationTemplateTask.builder()
                .id(UUID.randomUUID())
                .documentationTemplate(documentationTemplate)
                .section(section)
                .taskTemplate(taskTemplate)
                .titleOverride(titleOverride)
                .descriptionOverride(descriptionOverride)
                .requiredOverride(requiredOverride)
                .sortOrder(sortOrder)
                .build();
    }

    public String effectiveTitle() {
        return titleOverride == null || titleOverride.isBlank()
                ? (taskTemplate == null ? null : taskTemplate.getName())
                : titleOverride;
    }

    public String effectiveDescription() {
        return descriptionOverride == null || descriptionOverride.isBlank()
                ? (taskTemplate == null ? null : taskTemplate.getDescription())
                : descriptionOverride;
    }

    public boolean effectiveRequired() {
        return requiredOverride != null ? requiredOverride : taskTemplate != null && taskTemplate.isRequiredByDefault();
    }

    public UUID getDocumentationTemplateId() {
        return documentationTemplate == null ? null : documentationTemplate.getId();
    }

    private void assignDocumentationTemplate(com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate) {
        this.documentationTemplate = Objects.requireNonNull(documentationTemplate, "documentationTemplate must not be null");
        assignAgency(documentationTemplate.getAgency());
    }

    private void assignSection(DocumentationTemplateSection section) {
        if (section != null && !Objects.equals(section.getDocumentationTemplateId(), getDocumentationTemplateId())) {
            throw new IllegalArgumentException("section must belong to the same documentation template as the task");
        }
        this.section = section;
    }

    private void assignTaskTemplate(TaskTemplate taskTemplate) {
        if (taskTemplate != null && !Objects.equals(taskTemplate.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("taskTemplate must belong to the same agency as the documentation template task");
        }
        this.taskTemplate = taskTemplate;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        titleOverride = optional(titleOverride);
        descriptionOverride = optional(descriptionOverride);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be greater than or equal to 0");
        }
        if (taskTemplate == null && (titleOverride == null || titleOverride.isBlank())) {
            throw new IllegalArgumentException("titleOverride is required when no taskTemplate is linked");
        }
        assignDocumentationTemplate(documentationTemplate);
        assignSection(section);
        assignTaskTemplate(taskTemplate);
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
