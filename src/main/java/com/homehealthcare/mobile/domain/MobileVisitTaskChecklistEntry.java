package com.homehealthcare.mobile.domain;

import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
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
@Table(name = "mobile_visit_task_checklist_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileVisitTaskChecklistEntry extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_session_id", nullable = false)
    private MobileVisitExecutionSession executionSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64)
    private TaskTemplateCategory category;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "completion_notes", length = 1000)
    private String completionNotes;

    @Builder
    private MobileVisitTaskChecklistEntry(
            UUID id,
            MobileVisitExecutionSession executionSession,
            TaskTemplate taskTemplate,
            String title,
            String description,
            TaskTemplateCategory category,
            int sortOrder,
            boolean completed,
            OffsetDateTime completedAt,
            String completionNotes) {
        this.id = id;
        assignExecutionSession(executionSession);
        assignTaskTemplate(taskTemplate);
        this.title = title;
        this.description = description;
        this.category = category;
        this.sortOrder = sortOrder;
        this.completed = completed;
        this.completedAt = completedAt;
        this.completionNotes = completionNotes;
        validateState();
    }

    public static MobileVisitTaskChecklistEntry create(
            MobileVisitExecutionSession executionSession,
            TaskTemplate taskTemplate,
            String title,
            String description,
            TaskTemplateCategory category,
            int sortOrder,
            boolean completed,
            OffsetDateTime completedAt,
            String completionNotes) {
        return MobileVisitTaskChecklistEntry.builder()
                .id(UUID.randomUUID())
                .executionSession(executionSession)
                .taskTemplate(taskTemplate)
                .title(title)
                .description(description)
                .category(category)
                .sortOrder(sortOrder)
                .completed(completed)
                .completedAt(completedAt)
                .completionNotes(completionNotes)
                .build();
    }

    public void update(
            TaskTemplate taskTemplate,
            String title,
            String description,
            TaskTemplateCategory category,
            int sortOrder,
            boolean completed,
            OffsetDateTime completedAt,
            String completionNotes) {
        assignTaskTemplate(taskTemplate);
        this.title = title;
        this.description = description;
        this.category = category;
        this.sortOrder = sortOrder;
        this.completed = completed;
        this.completedAt = completedAt;
        this.completionNotes = completionNotes;
        validateState();
    }

    public UUID getExecutionSessionId() {
        return executionSession == null ? null : executionSession.getId();
    }

    private void assignExecutionSession(MobileVisitExecutionSession executionSession) {
        this.executionSession = Objects.requireNonNull(executionSession, "executionSession must not be null");
        assignAgency(executionSession.getAgency());
    }

    private void assignTaskTemplate(TaskTemplate taskTemplate) {
        if (taskTemplate != null && !Objects.equals(taskTemplate.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("taskTemplate must belong to the same agency as the checklist entry");
        }
        this.taskTemplate = taskTemplate;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        title = normalizeRequired(title);
        description = normalizeOptional(description);
        completionNotes = normalizeOptional(completionNotes);
        assignTaskTemplate(taskTemplate);
        validateState();
    }

    private void validateState() {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be zero or greater");
        }
        if (completed && completedAt == null) {
            throw new IllegalArgumentException("completed checklist entries must have completedAt");
        }
        if (!completed && completedAt != null) {
            throw new IllegalArgumentException("incomplete checklist entries must not have completedAt");
        }
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
