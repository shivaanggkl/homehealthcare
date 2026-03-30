package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.revenuereadiness.foundation.RevenueValidationOutcome;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
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
@Table(name = "visit_completion_validation_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitCompletionValidationResult extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentation_record_id")
    private VisitDocumentationRecord documentationRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private RevenueValidationOutcome outcome;

    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;

    @Column(name = "summary", nullable = false, length = 2000)
    private String summary;

    @Column(name = "visit_execution_completed", nullable = false)
    private boolean visitExecutionCompleted;

    @Column(name = "documentation_submitted", nullable = false)
    private boolean documentationSubmitted;

    @Column(name = "unresolved_review_return", nullable = false)
    private boolean unresolvedReviewReturn;

    @Column(name = "critical_exception_open", nullable = false)
    private boolean criticalExceptionOpen;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private VisitCompletionValidationResult(
            UUID id,
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            VisitDocumentationRecord documentationRecord,
            RevenueValidationOutcome outcome,
            String reasonCode,
            String summary,
            boolean visitExecutionCompleted,
            boolean documentationSubmitted,
            boolean unresolvedReviewReturn,
            boolean criticalExceptionOpen,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignPatient(patient);
        assignBranch(branch);
        assignDocumentationRecord(documentationRecord);
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.visitExecutionCompleted = visitExecutionCompleted;
        this.documentationSubmitted = documentationSubmitted;
        this.unresolvedReviewReturn = unresolvedReviewReturn;
        this.criticalExceptionOpen = criticalExceptionOpen;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public static VisitCompletionValidationResult create(
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            VisitDocumentationRecord documentationRecord,
            RevenueValidationOutcome outcome,
            String reasonCode,
            String summary,
            boolean visitExecutionCompleted,
            boolean documentationSubmitted,
            boolean unresolvedReviewReturn,
            boolean criticalExceptionOpen,
            OffsetDateTime evaluatedAt) {
        return VisitCompletionValidationResult.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .patient(patient)
                .branch(branch)
                .documentationRecord(documentationRecord)
                .outcome(outcome)
                .reasonCode(reasonCode)
                .summary(summary)
                .visitExecutionCompleted(visitExecutionCompleted)
                .documentationSubmitted(documentationSubmitted)
                .unresolvedReviewReturn(unresolvedReviewReturn)
                .criticalExceptionOpen(criticalExceptionOpen)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            VisitDocumentationRecord documentationRecord,
            RevenueValidationOutcome outcome,
            String reasonCode,
            String summary,
            boolean visitExecutionCompleted,
            boolean documentationSubmitted,
            boolean unresolvedReviewReturn,
            boolean criticalExceptionOpen,
            OffsetDateTime evaluatedAt) {
        assignDocumentationRecord(documentationRecord);
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.visitExecutionCompleted = visitExecutionCompleted;
        this.documentationSubmitted = documentationSubmitted;
        this.unresolvedReviewReturn = unresolvedReviewReturn;
        this.criticalExceptionOpen = criticalExceptionOpen;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the completion validation result");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the completion validation result");
        }
        this.branch = branch;
    }

    private void assignDocumentationRecord(VisitDocumentationRecord documentationRecord) {
        if (documentationRecord != null && !Objects.equals(documentationRecord.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("documentationRecord must belong to the same agency as the completion validation result");
        }
        this.documentationRecord = documentationRecord;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reasonCode = normalizeRequired(reasonCode);
        summary = normalizeRequired(summary);
        assignPatient(patient);
        assignBranch(branch);
        assignDocumentationRecord(documentationRecord);
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }
}
