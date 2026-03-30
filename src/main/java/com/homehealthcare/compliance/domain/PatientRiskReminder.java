package com.homehealthcare.compliance.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.compliance.foundation.PatientRiskReminderStatus;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.patient.domain.Patient;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_risk_reminders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientRiskReminder extends AgencyScopedEntity {

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
    @JoinColumn(name = "visit_occurrence_id")
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentation_record_id")
    private VisitDocumentationRecord documentationRecord;

    @Column(name = "risk_type", nullable = false, length = 100)
    private String riskType;

    @Column(name = "severity_label", length = 60)
    private String severityLabel;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "source_context_type", length = 80)
    private String sourceContextType;

    @Column(name = "source_record_id")
    private UUID sourceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientRiskReminderStatus status;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private PatientRiskReminder(
            UUID id,
            Patient patient,
            Branch branch,
            VisitOccurrence visitOccurrence,
            VisitDocumentationRecord documentationRecord,
            String riskType,
            String severityLabel,
            String summary,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            String sourceContextType,
            UUID sourceRecordId,
            PatientRiskReminderStatus status,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignVisitOccurrence(visitOccurrence);
        assignDocumentationRecord(documentationRecord);
        this.riskType = riskType;
        this.severityLabel = severityLabel;
        this.summary = summary;
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        this.expiresAt = expiresAt;
        this.sourceContextType = sourceContextType;
        this.sourceRecordId = sourceRecordId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.resolvedAt = resolvedAt;
        validateState();
    }

    public static PatientRiskReminder create(
            Patient patient,
            Branch branch,
            VisitOccurrence visitOccurrence,
            VisitDocumentationRecord documentationRecord,
            String riskType,
            String severityLabel,
            String summary,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            String sourceContextType,
            UUID sourceRecordId) {
        return PatientRiskReminder.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .visitOccurrence(visitOccurrence)
                .documentationRecord(documentationRecord)
                .riskType(riskType)
                .severityLabel(severityLabel)
                .summary(summary)
                .effectiveAt(effectiveAt)
                .expiresAt(expiresAt)
                .sourceContextType(sourceContextType)
                .sourceRecordId(sourceRecordId)
                .status(PatientRiskReminderStatus.ACTIVE)
                .build();
    }

    public void updateDetails(
            Branch branch,
            VisitOccurrence visitOccurrence,
            VisitDocumentationRecord documentationRecord,
            String severityLabel,
            String summary,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            String sourceContextType,
            UUID sourceRecordId) {
        assignBranch(branch);
        assignVisitOccurrence(visitOccurrence);
        assignDocumentationRecord(documentationRecord);
        this.severityLabel = severityLabel;
        this.summary = summary;
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        this.expiresAt = expiresAt;
        this.sourceContextType = sourceContextType;
        this.sourceRecordId = sourceRecordId;
        if (status == PatientRiskReminderStatus.RESOLVED) {
            this.status = PatientRiskReminderStatus.ACTIVE;
            this.resolvedAt = null;
        }
        validateState();
    }

    public void resolve(OffsetDateTime resolvedAt) {
        this.status = PatientRiskReminderStatus.RESOLVED;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    public PatientRiskReminderStatus statusAt(OffsetDateTime when) {
        if (status == PatientRiskReminderStatus.RESOLVED && (when == null || resolvedAt == null || !resolvedAt.isAfter(when))) {
            return PatientRiskReminderStatus.RESOLVED;
        }
        if (expiresAt != null && when != null && expiresAt.isBefore(when)) {
            return PatientRiskReminderStatus.EXPIRED;
        }
        return status;
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the risk reminder");
        }
        this.branch = branch;
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        if (visitOccurrence != null) {
            if (!Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the risk reminder");
            }
            if (patient != null && !Objects.equals(visitOccurrence.getPatient().getId(), patient.getId())) {
                throw new IllegalArgumentException("visitOccurrence must belong to the same patient as the risk reminder");
            }
        }
        this.visitOccurrence = visitOccurrence;
    }

    private void assignDocumentationRecord(VisitDocumentationRecord documentationRecord) {
        if (documentationRecord != null) {
            if (!Objects.equals(documentationRecord.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("documentationRecord must belong to the same agency as the risk reminder");
            }
            if (patient != null && !Objects.equals(documentationRecord.getPatientId(), patient.getId())) {
                throw new IllegalArgumentException("documentationRecord must belong to the same patient as the risk reminder");
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
        riskType = required(riskType).toUpperCase(Locale.ROOT);
        severityLabel = optional(severityLabel);
        summary = required(summary);
        sourceContextType = optionalUpper(sourceContextType);
        assignBranch(branch);
        assignVisitOccurrence(visitOccurrence);
        assignDocumentationRecord(documentationRecord);
        validateState();
    }

    private void validateState() {
        if (expiresAt != null && expiresAt.isBefore(effectiveAt)) {
            throw new IllegalArgumentException("expiresAt must not be before effectiveAt");
        }
        if (status == PatientRiskReminderStatus.RESOLVED && resolvedAt == null) {
            throw new IllegalArgumentException("resolved reminders must record resolvedAt");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String optionalUpper(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
