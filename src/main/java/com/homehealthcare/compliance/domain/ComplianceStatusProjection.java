package com.homehealthcare.compliance.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.compliance.foundation.CertificationPeriodStatus;
import com.homehealthcare.compliance.foundation.ComplianceReadinessStatus;
import com.homehealthcare.patient.domain.Patient;
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
@Table(name = "compliance_status_projections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComplianceStatusProjection extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Column(name = "checklist_pass_count", nullable = false)
    private int checklistPassCount;

    @Column(name = "checklist_warning_count", nullable = false)
    private int checklistWarningCount;

    @Column(name = "checklist_fail_count", nullable = false)
    private int checklistFailCount;

    @Column(name = "documentation_satisfied_count", nullable = false)
    private int documentationSatisfiedCount;

    @Column(name = "documentation_warning_count", nullable = false)
    private int documentationWarningCount;

    @Column(name = "documentation_unsatisfied_count", nullable = false)
    private int documentationUnsatisfiedCount;

    @Column(name = "missing_acknowledgment_count", nullable = false)
    private int missingAcknowledgmentCount;

    @Column(name = "expired_acknowledgment_count", nullable = false)
    private int expiredAcknowledgmentCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "certification_period_status", nullable = false, length = 32)
    private CertificationPeriodStatus certificationPeriodStatus;

    @Column(name = "active_risk_reminder_count", nullable = false)
    private int activeRiskReminderCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 32)
    private ComplianceReadinessStatus readinessStatus;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private ComplianceStatusProjection(
            UUID id,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            int checklistPassCount,
            int checklistWarningCount,
            int checklistFailCount,
            int documentationSatisfiedCount,
            int documentationWarningCount,
            int documentationUnsatisfiedCount,
            int missingAcknowledgmentCount,
            int expiredAcknowledgmentCount,
            CertificationPeriodStatus certificationPeriodStatus,
            int activeRiskReminderCount,
            ComplianceReadinessStatus readinessStatus,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.checklistPassCount = checklistPassCount;
        this.checklistWarningCount = checklistWarningCount;
        this.checklistFailCount = checklistFailCount;
        this.documentationSatisfiedCount = documentationSatisfiedCount;
        this.documentationWarningCount = documentationWarningCount;
        this.documentationUnsatisfiedCount = documentationUnsatisfiedCount;
        this.missingAcknowledgmentCount = missingAcknowledgmentCount;
        this.expiredAcknowledgmentCount = expiredAcknowledgmentCount;
        this.certificationPeriodStatus = Objects.requireNonNull(certificationPeriodStatus, "certificationPeriodStatus must not be null");
        this.activeRiskReminderCount = activeRiskReminderCount;
        this.readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static ComplianceStatusProjection create(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            int checklistPassCount,
            int checklistWarningCount,
            int checklistFailCount,
            int documentationSatisfiedCount,
            int documentationWarningCount,
            int documentationUnsatisfiedCount,
            int missingAcknowledgmentCount,
            int expiredAcknowledgmentCount,
            CertificationPeriodStatus certificationPeriodStatus,
            int activeRiskReminderCount,
            ComplianceReadinessStatus readinessStatus,
            OffsetDateTime evaluatedAt) {
        return ComplianceStatusProjection.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .checklistPassCount(checklistPassCount)
                .checklistWarningCount(checklistWarningCount)
                .checklistFailCount(checklistFailCount)
                .documentationSatisfiedCount(documentationSatisfiedCount)
                .documentationWarningCount(documentationWarningCount)
                .documentationUnsatisfiedCount(documentationUnsatisfiedCount)
                .missingAcknowledgmentCount(missingAcknowledgmentCount)
                .expiredAcknowledgmentCount(expiredAcknowledgmentCount)
                .certificationPeriodStatus(certificationPeriodStatus)
                .activeRiskReminderCount(activeRiskReminderCount)
                .readinessStatus(readinessStatus)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            Branch branch,
            ServiceLine serviceLine,
            int checklistPassCount,
            int checklistWarningCount,
            int checklistFailCount,
            int documentationSatisfiedCount,
            int documentationWarningCount,
            int documentationUnsatisfiedCount,
            int missingAcknowledgmentCount,
            int expiredAcknowledgmentCount,
            CertificationPeriodStatus certificationPeriodStatus,
            int activeRiskReminderCount,
            ComplianceReadinessStatus readinessStatus,
            OffsetDateTime evaluatedAt) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.checklistPassCount = checklistPassCount;
        this.checklistWarningCount = checklistWarningCount;
        this.checklistFailCount = checklistFailCount;
        this.documentationSatisfiedCount = documentationSatisfiedCount;
        this.documentationWarningCount = documentationWarningCount;
        this.documentationUnsatisfiedCount = documentationUnsatisfiedCount;
        this.missingAcknowledgmentCount = missingAcknowledgmentCount;
        this.expiredAcknowledgmentCount = expiredAcknowledgmentCount;
        this.certificationPeriodStatus = Objects.requireNonNull(certificationPeriodStatus, "certificationPeriodStatus must not be null");
        this.activeRiskReminderCount = activeRiskReminderCount;
        this.readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getServiceLineId() {
        return serviceLine == null ? null : serviceLine.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the compliance projection");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the compliance projection");
        }
        this.serviceLine = serviceLine;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignBranch(branch);
        assignServiceLine(serviceLine);
        validateState();
    }

    private void validateState() {
        if (checklistPassCount < 0
                || checklistWarningCount < 0
                || checklistFailCount < 0
                || documentationSatisfiedCount < 0
                || documentationWarningCount < 0
                || documentationUnsatisfiedCount < 0
                || missingAcknowledgmentCount < 0
                || expiredAcknowledgmentCount < 0
                || activeRiskReminderCount < 0) {
            throw new IllegalArgumentException("Projection counters must be zero or greater");
        }
    }
}
