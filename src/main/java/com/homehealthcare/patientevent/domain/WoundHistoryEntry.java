package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "wound_history_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WoundHistoryEntry extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wound_record_id", nullable = false)
    private WoundRecord woundRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "observation_summary", nullable = false, length = 2000)
    private String observationSummary;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "depth_cm", precision = 10, scale = 2)
    private BigDecimal depthCm;

    @Column(name = "progression_marker", length = 120)
    private String progressionMarker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_by_membership_id")
    private AgencyMembership capturedByMembership;

    @Builder
    private WoundHistoryEntry(
            UUID id,
            WoundRecord woundRecord,
            Patient patient,
            Branch branch,
            OffsetDateTime capturedAt,
            String observationSummary,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal depthCm,
            String progressionMarker,
            AgencyMembership capturedByMembership) {
        this.id = id;
        assignWoundRecord(woundRecord);
        assignPatient(patient);
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        this.observationSummary = observationSummary;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.depthCm = depthCm;
        this.progressionMarker = progressionMarker;
        validateState();
    }

    public static WoundHistoryEntry create(
            WoundRecord woundRecord,
            Patient patient,
            Branch branch,
            OffsetDateTime capturedAt,
            String observationSummary,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal depthCm,
            String progressionMarker,
            AgencyMembership capturedByMembership) {
        return WoundHistoryEntry.builder()
                .id(UUID.randomUUID())
                .woundRecord(woundRecord)
                .patient(patient)
                .branch(branch)
                .capturedAt(capturedAt)
                .observationSummary(observationSummary)
                .lengthCm(lengthCm)
                .widthCm(widthCm)
                .depthCm(depthCm)
                .progressionMarker(progressionMarker)
                .capturedByMembership(capturedByMembership)
                .build();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignWoundRecord(WoundRecord woundRecord) {
        this.woundRecord = Objects.requireNonNull(woundRecord, "woundRecord must not be null");
        assignAgency(woundRecord.getAgency());
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the wound history entry");
        }
        if (!Objects.equals(patient.getId(), woundRecord.getPatientId())) {
            throw new IllegalArgumentException("patient must match the wound record patient");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null) {
            if (!Objects.equals(branch.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("branch must belong to the same agency as the wound history entry");
            }
            if (woundRecord.getBranchId() != null && !Objects.equals(branch.getId(), woundRecord.getBranchId())) {
                throw new IllegalArgumentException("branch must match the wound record branch");
            }
        }
        this.branch = branch;
    }

    private void assignCapturedByMembership(AgencyMembership capturedByMembership) {
        if (capturedByMembership != null && !Objects.equals(capturedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("capturedByMembership must belong to the same agency as the wound history entry");
        }
        this.capturedByMembership = capturedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        observationSummary = required(observationSummary);
        progressionMarker = optionalUpper(progressionMarker);
        lengthCm = normalizeMeasurement(lengthCm, "lengthCm");
        widthCm = normalizeMeasurement(widthCm, "widthCm");
        depthCm = normalizeMeasurement(depthCm, "depthCm");
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
        validateState();
    }

    private void validateState() {
        if (capturedAt.isBefore(woundRecord.getIdentifiedAt())) {
            throw new IllegalArgumentException("capturedAt must not be before the wound was identified");
        }
    }

    private static BigDecimal normalizeMeasurement(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optionalUpper(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
