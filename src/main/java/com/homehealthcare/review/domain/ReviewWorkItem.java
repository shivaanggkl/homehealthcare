package com.homehealthcare.review.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.review.foundation.ReviewLifecycleStatus;
import com.homehealthcare.review.foundation.ReviewPriority;
import com.homehealthcare.review.foundation.ReviewSourceType;
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
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "review_work_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewWorkItem extends AgencyScopedEntity {

    private static final Set<ReviewLifecycleStatus> ACTIVE_STATUSES = EnumSet.of(
            ReviewLifecycleStatus.PENDING_REVIEW,
            ReviewLifecycleStatus.ASSIGNED,
            ReviewLifecycleStatus.IN_REVIEW,
            ReviewLifecycleStatus.RETURNED_FOR_FIX,
            ReviewLifecycleStatus.RESUBMITTED,
            ReviewLifecycleStatus.SIGNOFF_REQUESTED);

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 48)
    private ReviewSourceType sourceType;

    @Column(name = "source_record_id", nullable = false)
    private UUID sourceRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_occurrence_id")
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentation_record_id")
    private VisitDocumentationRecord documentationRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReviewLifecycleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 24)
    private ReviewPriority priority;

    @Column(name = "exception_driven", nullable = false)
    private boolean exceptionDriven;

    @Column(name = "entered_queue_at", nullable = false)
    private OffsetDateTime enteredQueueAt;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private ReviewWorkItem(
            UUID id,
            ReviewSourceType sourceType,
            UUID sourceRecordId,
            Branch branch,
            Patient patient,
            VisitOccurrence visitOccurrence,
            VisitDocumentationRecord documentationRecord,
            ReviewLifecycleStatus status,
            ReviewPriority priority,
            boolean exceptionDriven,
            OffsetDateTime enteredQueueAt,
            OffsetDateTime dueAt,
            OffsetDateTime lastActivityAt,
            OffsetDateTime resolvedAt) {
        this.id = id;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceRecordId = Objects.requireNonNull(sourceRecordId, "sourceRecordId must not be null");
        assignBranch(branch);
        assignPatient(patient);
        assignVisitOccurrence(visitOccurrence);
        assignDocumentationRecord(documentationRecord);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.priority = priority;
        this.exceptionDriven = exceptionDriven;
        this.enteredQueueAt = Objects.requireNonNull(enteredQueueAt, "enteredQueueAt must not be null");
        this.dueAt = dueAt;
        this.lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt must not be null");
        this.resolvedAt = resolvedAt;
        validateState();
    }

    public static ReviewWorkItem queue(
            ReviewSourceType sourceType,
            UUID sourceRecordId,
            Branch branch,
            Patient patient,
            VisitOccurrence visitOccurrence,
            VisitDocumentationRecord documentationRecord,
            ReviewPriority priority,
            boolean exceptionDriven,
            OffsetDateTime enteredQueueAt,
            OffsetDateTime dueAt) {
        return ReviewWorkItem.builder()
                .id(UUID.randomUUID())
                .sourceType(sourceType)
                .sourceRecordId(sourceRecordId)
                .branch(branch)
                .patient(patient)
                .visitOccurrence(visitOccurrence)
                .documentationRecord(documentationRecord)
                .status(ReviewLifecycleStatus.PENDING_REVIEW)
                .priority(priority)
                .exceptionDriven(exceptionDriven)
                .enteredQueueAt(enteredQueueAt)
                .dueAt(dueAt)
                .lastActivityAt(enteredQueueAt)
                .build();
    }

    public void markAssigned(OffsetDateTime when) {
        transitionTo(ReviewLifecycleStatus.ASSIGNED, when);
    }

    public void markInReview(OffsetDateTime when) {
        transitionTo(ReviewLifecycleStatus.IN_REVIEW, when);
    }

    public void markReturnedForFix(OffsetDateTime when) {
        transitionTo(ReviewLifecycleStatus.RETURNED_FOR_FIX, when);
    }

    public void markResubmitted(OffsetDateTime when) {
        transitionTo(ReviewLifecycleStatus.RESUBMITTED, when);
        this.resolvedAt = null;
    }

    public void markApproved(OffsetDateTime when) {
        transitionToResolved(ReviewLifecycleStatus.APPROVED, when);
    }

    public void markRejected(OffsetDateTime when) {
        transitionToResolved(ReviewLifecycleStatus.REJECTED, when);
    }

    public void markSignoffRequested(OffsetDateTime when) {
        transitionTo(ReviewLifecycleStatus.SIGNOFF_REQUESTED, when);
    }

    public void markSignoffCompleted(OffsetDateTime when) {
        transitionToResolved(ReviewLifecycleStatus.SIGNOFF_COMPLETED, when);
    }

    public boolean isActiveQueueEntry() {
        return ACTIVE_STATUSES.contains(status);
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getDocumentationRecordId() {
        return documentationRecord == null ? null : documentationRecord.getId();
    }

    private void transitionTo(ReviewLifecycleStatus newStatus, OffsetDateTime when) {
        this.status = Objects.requireNonNull(newStatus, "newStatus must not be null");
        this.lastActivityAt = Objects.requireNonNull(when, "when must not be null");
    }

    private void transitionToResolved(ReviewLifecycleStatus newStatus, OffsetDateTime when) {
        transitionTo(newStatus, when);
        this.resolvedAt = when;
    }

    private void assignBranch(Branch branch) {
        if (branch != null) {
            assignAgency(branch.getAgency());
        }
        this.branch = branch;
    }

    private void assignPatient(Patient patient) {
        if (patient != null) {
            if (getAgencyId() == null) {
                assignAgency(patient.getAgency());
            }
            if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("patient must belong to the same agency as the review work item");
            }
        }
        this.patient = patient;
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        if (visitOccurrence != null) {
            if (getAgencyId() == null) {
                assignAgency(visitOccurrence.getAgency());
            }
            if (!Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the review work item");
            }
        }
        this.visitOccurrence = visitOccurrence;
    }

    private void assignDocumentationRecord(VisitDocumentationRecord documentationRecord) {
        if (documentationRecord != null) {
            if (getAgencyId() == null) {
                assignAgency(documentationRecord.getAgency());
            }
            if (!Objects.equals(documentationRecord.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("documentationRecord must belong to the same agency as the review work item");
            }
        }
        this.documentationRecord = documentationRecord;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        sourceRecordId = Objects.requireNonNull(sourceRecordId, "sourceRecordId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        enteredQueueAt = Objects.requireNonNull(enteredQueueAt, "enteredQueueAt must not be null");
        lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt must not be null");
        validateState();
    }

    private void validateState() {
        if (dueAt != null && dueAt.isBefore(enteredQueueAt)) {
            throw new IllegalArgumentException("dueAt must not be before enteredQueueAt");
        }
        if (lastActivityAt.isBefore(enteredQueueAt)) {
            throw new IllegalArgumentException("lastActivityAt must not be before enteredQueueAt");
        }
        if (resolvedAt != null && resolvedAt.isBefore(enteredQueueAt)) {
            throw new IllegalArgumentException("resolvedAt must not be before enteredQueueAt");
        }
        if (documentationRecord != null && sourceType != ReviewSourceType.VISIT_DOCUMENTATION_RECORD) {
            throw new IllegalArgumentException("documentationRecord linkage requires VISIT_DOCUMENTATION_RECORD source type");
        }
    }
}
