package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.revenuereadiness.foundation.RevenueExportLifecycleStatus;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.serviceline.domain.ServiceLine;
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
@Table(name = "revenue_readiness_projections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevenueReadinessProjection extends AgencyScopedEntity {

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
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completion_validation_id")
    private VisitCompletionValidationResult completionValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signature_validation_id")
    private SignedVisitValidationResult signatureValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_service_summary_id")
    private PayerServiceSummaryProjection payerServiceSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_usage_snapshot_id")
    private AuthorizationUsageSnapshot authorizationUsageSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 32)
    private RevenueReadinessStatus readinessStatus;

    @Column(name = "exception_count", nullable = false)
    private int exceptionCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_lifecycle_status", nullable = false, length = 32)
    private RevenueExportLifecycleStatus exportLifecycleStatus;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private RevenueReadinessProjection(
            UUID id,
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            VisitCompletionValidationResult completionValidation,
            SignedVisitValidationResult signatureValidation,
            PayerServiceSummaryProjection payerServiceSummary,
            AuthorizationUsageSnapshot authorizationUsageSnapshot,
            RevenueReadinessStatus readinessStatus,
            int exceptionCount,
            int warningCount,
            RevenueExportLifecycleStatus exportLifecycleStatus,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignCompletionValidation(completionValidation);
        assignSignatureValidation(signatureValidation);
        assignPayerServiceSummary(payerServiceSummary);
        assignAuthorizationUsageSnapshot(authorizationUsageSnapshot);
        this.readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus must not be null");
        this.exceptionCount = exceptionCount;
        this.warningCount = warningCount;
        this.exportLifecycleStatus = Objects.requireNonNull(exportLifecycleStatus, "exportLifecycleStatus must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static RevenueReadinessProjection create(
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            VisitCompletionValidationResult completionValidation,
            SignedVisitValidationResult signatureValidation,
            PayerServiceSummaryProjection payerServiceSummary,
            AuthorizationUsageSnapshot authorizationUsageSnapshot,
            RevenueReadinessStatus readinessStatus,
            int exceptionCount,
            int warningCount,
            RevenueExportLifecycleStatus exportLifecycleStatus,
            OffsetDateTime evaluatedAt) {
        return RevenueReadinessProjection.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .completionValidation(completionValidation)
                .signatureValidation(signatureValidation)
                .payerServiceSummary(payerServiceSummary)
                .authorizationUsageSnapshot(authorizationUsageSnapshot)
                .readinessStatus(readinessStatus)
                .exceptionCount(exceptionCount)
                .warningCount(warningCount)
                .exportLifecycleStatus(exportLifecycleStatus)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            Branch branch,
            ServiceLine serviceLine,
            VisitCompletionValidationResult completionValidation,
            SignedVisitValidationResult signatureValidation,
            PayerServiceSummaryProjection payerServiceSummary,
            AuthorizationUsageSnapshot authorizationUsageSnapshot,
            RevenueReadinessStatus readinessStatus,
            int exceptionCount,
            int warningCount,
            RevenueExportLifecycleStatus exportLifecycleStatus,
            OffsetDateTime evaluatedAt) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignCompletionValidation(completionValidation);
        assignSignatureValidation(signatureValidation);
        assignPayerServiceSummary(payerServiceSummary);
        assignAuthorizationUsageSnapshot(authorizationUsageSnapshot);
        this.readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus must not be null");
        this.exceptionCount = exceptionCount;
        this.warningCount = warningCount;
        this.exportLifecycleStatus = Objects.requireNonNull(exportLifecycleStatus, "exportLifecycleStatus must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public void markExportLifecycleStatus(RevenueExportLifecycleStatus exportLifecycleStatus, OffsetDateTime evaluatedAt) {
        this.exportLifecycleStatus = Objects.requireNonNull(exportLifecycleStatus, "exportLifecycleStatus must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
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
            throw new IllegalArgumentException("patient must belong to the same agency as the readiness projection");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the readiness projection");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the readiness projection");
        }
        this.serviceLine = serviceLine;
    }

    private void assignCompletionValidation(VisitCompletionValidationResult completionValidation) {
        if (completionValidation != null && !Objects.equals(completionValidation.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("completionValidation must belong to the same agency as the readiness projection");
        }
        this.completionValidation = completionValidation;
    }

    private void assignSignatureValidation(SignedVisitValidationResult signatureValidation) {
        if (signatureValidation != null && !Objects.equals(signatureValidation.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("signatureValidation must belong to the same agency as the readiness projection");
        }
        this.signatureValidation = signatureValidation;
    }

    private void assignPayerServiceSummary(PayerServiceSummaryProjection payerServiceSummary) {
        if (payerServiceSummary != null && !Objects.equals(payerServiceSummary.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("payerServiceSummary must belong to the same agency as the readiness projection");
        }
        this.payerServiceSummary = payerServiceSummary;
    }

    private void assignAuthorizationUsageSnapshot(AuthorizationUsageSnapshot authorizationUsageSnapshot) {
        if (authorizationUsageSnapshot != null && !Objects.equals(authorizationUsageSnapshot.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("authorizationUsageSnapshot must belong to the same agency as the readiness projection");
        }
        this.authorizationUsageSnapshot = authorizationUsageSnapshot;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignCompletionValidation(completionValidation);
        assignSignatureValidation(signatureValidation);
        assignPayerServiceSummary(payerServiceSummary);
        assignAuthorizationUsageSnapshot(authorizationUsageSnapshot);
        validateState();
    }

    private void validateState() {
        if (exceptionCount < 0 || warningCount < 0) {
            throw new IllegalArgumentException("Projection counters must be zero or greater");
        }
    }
}
