package com.homehealthcare.documentation.domain;

import com.homehealthcare.documentation.foundation.DocumentationResponseState;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
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
@Table(name = "documentation_field_responses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationFieldResponse extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documentation_record_id", nullable = false)
    private VisitDocumentationRecord documentationRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_field_id", nullable = false)
    private DocumentationTemplateField templateField;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "normalized_value", columnDefinition = "clob")
    private String normalizedValue;

    @Column(name = "display_value", length = 1000)
    private String displayValue;

    @Column(name = "response_notes", length = 1000)
    private String responseNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_state", nullable = false, length = 32)
    private DocumentationResponseState completionState;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Builder
    private DocumentationFieldResponse(
            UUID id,
            VisitDocumentationRecord documentationRecord,
            DocumentationTemplateField templateField,
            String fieldKey,
            String normalizedValue,
            String displayValue,
            String responseNotes,
            DocumentationResponseState completionState,
            OffsetDateTime completedAt) {
        this.id = id;
        assignDocumentationRecord(documentationRecord);
        assignTemplateField(templateField);
        this.fieldKey = fieldKey;
        this.normalizedValue = normalizedValue;
        this.displayValue = displayValue;
        this.responseNotes = responseNotes;
        this.completionState = Objects.requireNonNull(completionState, "completionState must not be null");
        this.completedAt = completedAt;
    }

    public static DocumentationFieldResponse create(
            VisitDocumentationRecord documentationRecord,
            DocumentationTemplateField templateField,
            String normalizedValue,
            String displayValue,
            String responseNotes,
            DocumentationResponseState completionState,
            OffsetDateTime completedAt) {
        return DocumentationFieldResponse.builder()
                .id(UUID.randomUUID())
                .documentationRecord(documentationRecord)
                .templateField(templateField)
                .fieldKey(templateField.getFieldKey())
                .normalizedValue(normalizedValue)
                .displayValue(displayValue)
                .responseNotes(responseNotes)
                .completionState(completionState)
                .completedAt(completedAt)
                .build();
    }

    public void update(String normalizedValue, String displayValue, String responseNotes, DocumentationResponseState completionState, OffsetDateTime completedAt) {
        this.normalizedValue = normalizedValue;
        this.displayValue = displayValue;
        this.responseNotes = responseNotes;
        this.completionState = Objects.requireNonNull(completionState, "completionState must not be null");
        this.completedAt = completedAt;
    }

    public UUID getDocumentationRecordId() {
        return documentationRecord == null ? null : documentationRecord.getId();
    }

    public UUID getTemplateFieldId() {
        return templateField == null ? null : templateField.getId();
    }

    private void assignDocumentationRecord(VisitDocumentationRecord documentationRecord) {
        this.documentationRecord = Objects.requireNonNull(documentationRecord, "documentationRecord must not be null");
        assignAgency(documentationRecord.getAgency());
    }

    private void assignTemplateField(DocumentationTemplateField templateField) {
        this.templateField = Objects.requireNonNull(templateField, "templateField must not be null");
        if (!Objects.equals(templateField.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("templateField must belong to the same agency as the field response");
        }
        if (!Objects.equals(templateField.getDocumentationTemplateId(), documentationRecord.getSelectedTemplateId())) {
            throw new IllegalArgumentException("templateField must belong to the selected template");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        fieldKey = Objects.requireNonNull(fieldKey, "fieldKey must not be null").trim();
        normalizedValue = optional(normalizedValue);
        displayValue = optional(displayValue);
        responseNotes = optional(responseNotes);
        completionState = Objects.requireNonNull(completionState, "completionState must not be null");
        if (completedAt != null && completionState == DocumentationResponseState.PENDING) {
            throw new IllegalArgumentException("Pending field responses must not have completedAt");
        }
        assignTemplateField(templateField);
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
