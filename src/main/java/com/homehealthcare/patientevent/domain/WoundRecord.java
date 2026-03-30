package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientevent.foundation.WoundRecordStatus;
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
@Table(name = "wound_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WoundRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "identified_at", nullable = false)
    private OffsetDateTime identifiedAt;

    @Column(name = "wound_type_or_site", nullable = false, length = 160)
    private String woundTypeOrSite;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 32)
    private WoundRecordStatus currentStatus;

    @Column(name = "baseline_summary", length = 2000)
    private String baselineSummary;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private WoundRecord(
            UUID id,
            Patient patient,
            Branch branch,
            OffsetDateTime identifiedAt,
            String woundTypeOrSite,
            WoundRecordStatus currentStatus,
            String baselineSummary,
            boolean active,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        this.identifiedAt = Objects.requireNonNull(identifiedAt, "identifiedAt must not be null");
        this.woundTypeOrSite = woundTypeOrSite;
        this.currentStatus = Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        this.baselineSummary = baselineSummary;
        this.active = active;
        this.resolvedAt = resolvedAt;
        validateState();
    }

    public static WoundRecord create(
            Patient patient,
            Branch branch,
            OffsetDateTime identifiedAt,
            String woundTypeOrSite,
            WoundRecordStatus currentStatus,
            String baselineSummary) {
        WoundRecordStatus initialStatus = currentStatus == null ? WoundRecordStatus.ACTIVE : currentStatus;
        if (initialStatus == WoundRecordStatus.RESOLVED) {
            throw new IllegalArgumentException("New wound records must start in a non-resolved status");
        }
        return WoundRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .identifiedAt(identifiedAt)
                .woundTypeOrSite(woundTypeOrSite)
                .currentStatus(initialStatus)
                .baselineSummary(baselineSummary)
                .active(true)
                .build();
    }

    public void updateDetails(
            Branch branch,
            String woundTypeOrSite,
            WoundRecordStatus currentStatus,
            String baselineSummary) {
        assignBranch(branch);
        this.woundTypeOrSite = woundTypeOrSite;
        this.currentStatus = Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        this.baselineSummary = baselineSummary;
        if (currentStatus != WoundRecordStatus.RESOLVED) {
            this.active = true;
            this.resolvedAt = null;
        }
        validateState();
    }

    public void resolve(OffsetDateTime resolvedAt) {
        this.currentStatus = WoundRecordStatus.RESOLVED;
        this.active = false;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
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
            throw new IllegalArgumentException("branch must belong to the same agency as the wound record");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        woundTypeOrSite = required(woundTypeOrSite).toUpperCase(Locale.ROOT);
        baselineSummary = optional(baselineSummary);
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (currentStatus == WoundRecordStatus.RESOLVED && active) {
            throw new IllegalArgumentException("resolved wound records must not remain active");
        }
        if (currentStatus != WoundRecordStatus.RESOLVED && !active) {
            throw new IllegalArgumentException("non-resolved wound records must remain active");
        }
        if (currentStatus == WoundRecordStatus.RESOLVED && resolvedAt == null) {
            throw new IllegalArgumentException("resolved wound records must record resolvedAt");
        }
        if (currentStatus != WoundRecordStatus.RESOLVED && resolvedAt != null) {
            throw new IllegalArgumentException("active wound records must not have resolvedAt");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
