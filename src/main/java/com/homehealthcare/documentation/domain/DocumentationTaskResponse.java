package com.homehealthcare.documentation.domain;

import com.homehealthcare.documentation.foundation.DocumentationResponseState;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "documentation_task_responses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationTaskResponse extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documentation_record_id", nullable = false)
    private VisitDocumentationRecord documentationRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_task_id", nullable = false)
    private DocumentationTemplateTask templateTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;

    @Column(name = "task_title", nullable = false, length = 200)
    private String taskTitle;

    @Column(name = "task_description", length = 1000)
    private String taskDescription;

    @Column(name = "completion_required", nullable = false)
    private boolean completionRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_state", nullable = false, length = 32)
    private DocumentationResponseState completionState;

    @Column(name = "completion_notes", length = 1000)
    private String completionNotes;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private DocumentationTaskResponse(
            UUID id,
            VisitDocumentationRecord documentationRecord,
            DocumentationTemplateTask templateTask,
            TaskTemplate taskTemplate,
            String taskTitle,
            String taskDescription,
            boolean completionRequired,
            DocumentationResponseState completionState,
            String completionNotes,
            OffsetDateTime completedAt,
            int sortOrder) {
        this.id = id;
        assignDocumentationRecord(documentationRecord);
        assignTemplateTask(templateTask);
        assignTaskTemplate(taskTemplate);
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.completionRequired = completionRequired;
        this.completionState = Objects.requireNonNull(completionState, "completionState must not be null");
        this.completionNotes = completionNotes;
        this.completedAt = completedAt;
        this.sortOrder = sortOrder;
    }

    public static DocumentationTaskResponse create(
            VisitDocumentationRecord documentationRecord,
            DocumentationTemplateTask templateTask) {
        return DocumentationTaskResponse.builder()
                .id(UUID.randomUUID())
                .documentationRecord(documentationRecord)
                .templateTask(templateTask)
                .taskTemplate(templateTask.getTaskTemplate())
                .taskTitle(templateTask.effectiveTitle())
                .taskDescription(templateTask.effectiveDescription())
                .completionRequired(templateTask.effectiveRequired())
                .completionState(DocumentationResponseState.PENDING)
                .sortOrder(templateTask.getSortOrder())
                .build();
    }

    public void update(DocumentationResponseState completionState, String completionNotes, OffsetDateTime completedAt) {
        this.completionState = Objects.requireNonNull(completionState, "completionState must not be null");
        this.completionNotes = completionNotes;
        this.completedAt = completedAt;
    }

    public UUID getDocumentationRecordId() {
        return documentationRecord == null ? null : documentationRecord.getId();
    }

    public UUID getTemplateTaskId() {
        return templateTask == null ? null : templateTask.getId();
    }

    private void assignDocumentationRecord(VisitDocumentationRecord documentationRecord) {
        this.documentationRecord = Objects.requireNonNull(documentationRecord, "documentationRecord must not be null");
        assignAgency(documentationRecord.getAgency());
    }

    private void assignTemplateTask(DocumentationTemplateTask templateTask) {
        this.templateTask = Objects.requireNonNull(templateTask, "templateTask must not be null");
        if (!Objects.equals(templateTask.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("templateTask must belong to the same agency as the task response");
        }
        if (!Objects.equals(templateTask.getDocumentationTemplateId(), documentationRecord.getSelectedTemplateId())) {
            throw new IllegalArgumentException("templateTask must belong to the selected template");
        }
    }

    private void assignTaskTemplate(TaskTemplate taskTemplate) {
        if (taskTemplate != null && !Objects.equals(taskTemplate.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("taskTemplate must belong to the same agency as the task response");
        }
        this.taskTemplate = taskTemplate;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        taskTitle = Objects.requireNonNull(taskTitle, "taskTitle must not be null").trim();
        taskDescription = optional(taskDescription);
        completionNotes = optional(completionNotes);
        completionState = Objects.requireNonNull(completionState, "completionState must not be null");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be greater than or equal to 0");
        }
        if (completedAt != null && completionState == DocumentationResponseState.PENDING) {
            throw new IllegalArgumentException("Pending task responses must not have completedAt");
        }
        assignTemplateTask(templateTask);
        assignTaskTemplate(taskTemplate);
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
