package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.evv.foundation.EvvVerificationStatus;
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
@Table(name = "signed_visit_validation_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignedVisitValidationResult extends AgencyScopedEntity {

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
    @JoinColumn(name = "verification_session_id")
    private EvvVerificationSession verificationSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private RevenueValidationOutcome outcome;

    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;

    @Column(name = "summary", nullable = false, length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "caregiver_signature_status", nullable = false, length = 32)
    private SignatureVerificationStatus caregiverSignatureStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_signature_status", nullable = false, length = 32)
    private SignatureVerificationStatus patientSignatureStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "evv_verification_status", length = 32)
    private EvvVerificationStatus evvVerificationStatus;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private SignedVisitValidationResult(
            UUID id,
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            EvvVerificationSession verificationSession,
            RevenueValidationOutcome outcome,
            String reasonCode,
            String summary,
            SignatureVerificationStatus caregiverSignatureStatus,
            SignatureVerificationStatus patientSignatureStatus,
            EvvVerificationStatus evvVerificationStatus,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignPatient(patient);
        assignBranch(branch);
        assignVerificationSession(verificationSession);
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.caregiverSignatureStatus = Objects.requireNonNull(caregiverSignatureStatus, "caregiverSignatureStatus must not be null");
        this.patientSignatureStatus = Objects.requireNonNull(patientSignatureStatus, "patientSignatureStatus must not be null");
        this.evvVerificationStatus = evvVerificationStatus;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public static SignedVisitValidationResult create(
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            EvvVerificationSession verificationSession,
            RevenueValidationOutcome outcome,
            String reasonCode,
            String summary,
            SignatureVerificationStatus caregiverSignatureStatus,
            SignatureVerificationStatus patientSignatureStatus,
            EvvVerificationStatus evvVerificationStatus,
            OffsetDateTime evaluatedAt) {
        return SignedVisitValidationResult.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .patient(patient)
                .branch(branch)
                .verificationSession(verificationSession)
                .outcome(outcome)
                .reasonCode(reasonCode)
                .summary(summary)
                .caregiverSignatureStatus(caregiverSignatureStatus)
                .patientSignatureStatus(patientSignatureStatus)
                .evvVerificationStatus(evvVerificationStatus)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            EvvVerificationSession verificationSession,
            RevenueValidationOutcome outcome,
            String reasonCode,
            String summary,
            SignatureVerificationStatus caregiverSignatureStatus,
            SignatureVerificationStatus patientSignatureStatus,
            EvvVerificationStatus evvVerificationStatus,
            OffsetDateTime evaluatedAt) {
        assignVerificationSession(verificationSession);
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.caregiverSignatureStatus = Objects.requireNonNull(caregiverSignatureStatus, "caregiverSignatureStatus must not be null");
        this.patientSignatureStatus = Objects.requireNonNull(patientSignatureStatus, "patientSignatureStatus must not be null");
        this.evvVerificationStatus = evvVerificationStatus;
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
            throw new IllegalArgumentException("patient must belong to the same agency as the signed-visit validation result");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the signed-visit validation result");
        }
        this.branch = branch;
    }

    private void assignVerificationSession(EvvVerificationSession verificationSession) {
        if (verificationSession != null && !Objects.equals(verificationSession.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("verificationSession must belong to the same agency as the signed-visit validation result");
        }
        this.verificationSession = verificationSession;
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
        assignVerificationSession(verificationSession);
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }
}
