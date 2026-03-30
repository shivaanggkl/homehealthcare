package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.PatientEventEscalationStatus;
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
@Table(name = "patient_event_escalation_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientEventEscalationRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 48)
    private Epic12PatientEventTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientEventEscalationStatus status;

    @Column(name = "severity_label", length = 60)
    private String severityLabel;

    @Column(name = "reason_tag", length = 255)
    private String reasonTag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escalated_by_membership_id", nullable = false)
    private AgencyMembership escalatedByMembership;

    @Column(name = "escalated_at", nullable = false)
    private OffsetDateTime escalatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleared_by_membership_id")
    private AgencyMembership clearedByMembership;

    @Column(name = "cleared_at")
    private OffsetDateTime clearedAt;

    @Builder
    private PatientEventEscalationRecord(
            UUID id,
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventEscalationStatus status,
            String severityLabel,
            String reasonTag,
            AgencyMembership escalatedByMembership,
            OffsetDateTime escalatedAt,
            AgencyMembership clearedByMembership,
            OffsetDateTime clearedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.severityLabel = severityLabel;
        this.reasonTag = reasonTag;
        this.escalatedByMembership = Objects.requireNonNull(escalatedByMembership, "escalatedByMembership must not be null");
        this.escalatedAt = Objects.requireNonNull(escalatedAt, "escalatedAt must not be null");
        this.clearedByMembership = clearedByMembership;
        this.clearedAt = clearedAt;
        validateState();
    }

    public static PatientEventEscalationRecord create(
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            String severityLabel,
            String reasonTag,
            AgencyMembership escalatedByMembership,
            OffsetDateTime escalatedAt) {
        return PatientEventEscalationRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .targetType(targetType)
                .targetId(targetId)
                .status(PatientEventEscalationStatus.ACTIVE)
                .severityLabel(severityLabel)
                .reasonTag(reasonTag)
                .escalatedByMembership(escalatedByMembership)
                .escalatedAt(escalatedAt)
                .build();
    }

    public void clear(AgencyMembership clearedByMembership, OffsetDateTime clearedAt) {
        this.status = PatientEventEscalationStatus.CLEARED;
        this.clearedByMembership = Objects.requireNonNull(clearedByMembership, "clearedByMembership must not be null");
        this.clearedAt = Objects.requireNonNull(clearedAt, "clearedAt must not be null");
        validateState();
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
            throw new IllegalArgumentException("branch must belong to the same agency as the escalation record");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        severityLabel = optional(severityLabel);
        reasonTag = optional(reasonTag);
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (!Objects.equals(escalatedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("escalatedByMembership must belong to the same agency as the escalation record");
        }
        if (clearedByMembership != null && !Objects.equals(clearedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("clearedByMembership must belong to the same agency as the escalation record");
        }
        if (status == PatientEventEscalationStatus.CLEARED && (clearedByMembership == null || clearedAt == null)) {
            throw new IllegalArgumentException("cleared escalations must record who cleared them and when");
        }
        if (status == PatientEventEscalationStatus.ACTIVE && (clearedByMembership != null || clearedAt != null)) {
            throw new IllegalArgumentException("active escalations may not have cleared details");
        }
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
