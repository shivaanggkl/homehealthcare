package com.homehealthcare.compliance.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.compliance.foundation.ConsentAcknowledgmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
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
@Table(name = "consent_acknowledgment_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentAcknowledgmentRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "acknowledgment_type", nullable = false, length = 100)
    private String acknowledgmentType;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_by_membership_id")
    private AgencyMembership capturedByMembership;

    @Column(name = "capture_method", length = 80)
    private String captureMethod;

    @Column(name = "supporting_artifact_type", length = 80)
    private String supportingArtifactType;

    @Column(name = "supporting_artifact_id")
    private UUID supportingArtifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConsentAcknowledgmentStatus status;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Builder
    private ConsentAcknowledgmentRecord(
            UUID id,
            Patient patient,
            Branch branch,
            String acknowledgmentType,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            AgencyMembership capturedByMembership,
            String captureMethod,
            String supportingArtifactType,
            UUID supportingArtifactId,
            ConsentAcknowledgmentStatus status,
            OffsetDateTime revokedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
        this.acknowledgmentType = acknowledgmentType;
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        this.expiresAt = expiresAt;
        this.captureMethod = captureMethod;
        this.supportingArtifactType = supportingArtifactType;
        this.supportingArtifactId = supportingArtifactId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.revokedAt = revokedAt;
        validateState();
    }

    public static ConsentAcknowledgmentRecord record(
            Patient patient,
            Branch branch,
            String acknowledgmentType,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            AgencyMembership capturedByMembership,
            String captureMethod,
            String supportingArtifactType,
            UUID supportingArtifactId) {
        return ConsentAcknowledgmentRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .acknowledgmentType(acknowledgmentType)
                .effectiveAt(effectiveAt)
                .expiresAt(expiresAt)
                .capturedByMembership(capturedByMembership)
                .captureMethod(captureMethod)
                .supportingArtifactType(supportingArtifactType)
                .supportingArtifactId(supportingArtifactId)
                .status(ConsentAcknowledgmentStatus.ACTIVE)
                .build();
    }

    public void updateDetails(
            Branch branch,
            String acknowledgmentType,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            AgencyMembership capturedByMembership,
            String captureMethod,
            String supportingArtifactType,
            UUID supportingArtifactId) {
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
        this.acknowledgmentType = acknowledgmentType;
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        this.expiresAt = expiresAt;
        this.captureMethod = captureMethod;
        this.supportingArtifactType = supportingArtifactType;
        this.supportingArtifactId = supportingArtifactId;
        this.status = ConsentAcknowledgmentStatus.ACTIVE;
        this.revokedAt = null;
        validateState();
    }

    public void revoke(OffsetDateTime revokedAt) {
        this.status = ConsentAcknowledgmentStatus.REVOKED;
        this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
    }

    public ConsentAcknowledgmentStatus statusAt(OffsetDateTime when) {
        if (status == ConsentAcknowledgmentStatus.REVOKED && (when == null || revokedAt == null || !revokedAt.isAfter(when))) {
            return ConsentAcknowledgmentStatus.REVOKED;
        }
        if (expiresAt != null && when != null && expiresAt.isBefore(when)) {
            return ConsentAcknowledgmentStatus.EXPIRED;
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
            throw new IllegalArgumentException("branch must belong to the same agency as the acknowledgment record");
        }
        this.branch = branch;
    }

    private void assignCapturedByMembership(AgencyMembership capturedByMembership) {
        if (capturedByMembership != null && !Objects.equals(capturedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("capturedByMembership must belong to the same agency as the acknowledgment record");
        }
        this.capturedByMembership = capturedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        acknowledgmentType = required(acknowledgmentType).toUpperCase(Locale.ROOT);
        captureMethod = optionalUpper(captureMethod);
        supportingArtifactType = optionalUpper(supportingArtifactType);
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
        validateState();
    }

    private void validateState() {
        if (expiresAt != null && expiresAt.isBefore(effectiveAt)) {
            throw new IllegalArgumentException("expiresAt must not be before effectiveAt");
        }
        if (status == ConsentAcknowledgmentStatus.REVOKED && revokedAt == null) {
            throw new IllegalArgumentException("revoked acknowledgments must record revokedAt");
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
