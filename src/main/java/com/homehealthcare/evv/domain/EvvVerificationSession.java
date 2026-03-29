package com.homehealthcare.evv.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.evv.foundation.EvvComplianceOutcome;
import com.homehealthcare.evv.foundation.EvvVerificationStatus;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "evv_verification_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvvVerificationSession extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_session_id")
    private MobileVisitExecutionSession executionSession;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 40)
    private EvvVerificationStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_outcome", nullable = false, length = 32)
    private EvvComplianceOutcome complianceOutcome;

    @Builder
    private EvvVerificationSession(
            UUID id,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            MobileVisitExecutionSession executionSession,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt,
            EvvVerificationStatus verificationStatus,
            EvvComplianceOutcome complianceOutcome) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        assignExecutionSession(executionSession);
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
        this.closedAt = closedAt;
        this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        this.complianceOutcome = Objects.requireNonNull(complianceOutcome, "complianceOutcome must not be null");
    }

    public static EvvVerificationSession open(
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            MobileVisitExecutionSession executionSession,
            OffsetDateTime openedAt) {
        return EvvVerificationSession.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .caregiverProfile(caregiverProfile)
                .patient(patient)
                .branch(branch)
                .executionSession(executionSession)
                .openedAt(openedAt)
                .verificationStatus(EvvVerificationStatus.PENDING_VERIFICATION)
                .complianceOutcome(EvvComplianceOutcome.BLOCKED)
                .build();
    }

    public void refreshOutcome(EvvVerificationStatus verificationStatus, EvvComplianceOutcome complianceOutcome) {
        this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        this.complianceOutcome = Objects.requireNonNull(complianceOutcome, "complianceOutcome must not be null");
        if (verificationStatus == EvvVerificationStatus.RESOLVED || verificationStatus == EvvVerificationStatus.MISSED_VISIT_REPORTED) {
            OffsetDateTime closureTimestamp = OffsetDateTime.now();
            if (closureTimestamp.isBefore(openedAt)) {
                closureTimestamp = openedAt;
            }
            this.closedAt = closureTimestamp;
        }
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile == null ? null : caregiverProfile.getId();
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

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the verification session");
        }
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the verification session");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the verification session");
        }
        this.branch = branch;
    }

    private void assignExecutionSession(MobileVisitExecutionSession executionSession) {
        if (executionSession != null && !Objects.equals(executionSession.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("executionSession must belong to the same agency as the verification session");
        }
        if (executionSession != null && visitOccurrence != null && !Objects.equals(executionSession.getVisitOccurrenceId(), visitOccurrence.getId())) {
            throw new IllegalArgumentException("executionSession must belong to the same visit as the verification session");
        }
        this.executionSession = executionSession;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
        if (closedAt != null && closedAt.isBefore(openedAt)) {
            throw new IllegalArgumentException("closedAt must not be before openedAt");
        }
        verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        complianceOutcome = Objects.requireNonNull(complianceOutcome, "complianceOutcome must not be null");
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        assignExecutionSession(executionSession);
    }
}
