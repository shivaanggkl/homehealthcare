package com.homehealthcare.evv.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.membership.domain.AgencyMembership;
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
@Table(name = "visit_exception_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitExceptionRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_session_id")
    private EvvVerificationSession verificationSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_profile_id")
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_membership_id", nullable = false)
    private AgencyMembership createdByMembership;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 40)
    private VisitExceptionType exceptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private VisitExceptionSeverity severity;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "narrative", nullable = false, length = 2000)
    private String narrative;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private VisitExceptionStatus status;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private VisitExceptionRecord(
            UUID id,
            EvvVerificationSession verificationSession,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            AgencyMembership createdByMembership,
            VisitExceptionType exceptionType,
            VisitExceptionSeverity severity,
            String reasonCode,
            String narrative,
            VisitExceptionStatus status,
            OffsetDateTime acknowledgedAt,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignVerificationSession(verificationSession);
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        assignCreatedByMembership(createdByMembership);
        this.exceptionType = Objects.requireNonNull(exceptionType, "exceptionType must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.reasonCode = reasonCode;
        this.narrative = narrative;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
    }

    public static VisitExceptionRecord log(
            EvvVerificationSession verificationSession,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            AgencyMembership createdByMembership,
            VisitExceptionType exceptionType,
            VisitExceptionSeverity severity,
            String reasonCode,
            String narrative) {
        return VisitExceptionRecord.builder()
                .id(UUID.randomUUID())
                .verificationSession(verificationSession)
                .visitOccurrence(visitOccurrence)
                .caregiverProfile(caregiverProfile)
                .patient(patient)
                .branch(branch)
                .createdByMembership(createdByMembership)
                .exceptionType(exceptionType)
                .severity(severity)
                .reasonCode(reasonCode)
                .narrative(narrative)
                .status(VisitExceptionStatus.OPEN)
                .build();
    }

    public void acknowledge(OffsetDateTime acknowledgedAt) {
        this.status = VisitExceptionStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Objects.requireNonNull(acknowledgedAt, "acknowledgedAt must not be null");
    }

    public void markEscalated() {
        this.status = VisitExceptionStatus.ESCALATED;
    }

    public void resolve(OffsetDateTime resolvedAt) {
        this.status = VisitExceptionStatus.RESOLVED;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    private void assignVerificationSession(EvvVerificationSession verificationSession) {
        if (verificationSession != null) {
            assignAgency(verificationSession.getAgency());
        }
        this.verificationSession = verificationSession;
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        if (getAgencyId() == null) {
            assignAgency(visitOccurrence.getAgency());
        }
        if (!Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the visit exception");
        }
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        if (caregiverProfile != null && !Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the visit exception");
        }
        this.caregiverProfile = caregiverProfile;
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the visit exception");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the visit exception");
        }
        this.branch = branch;
    }

    private void assignCreatedByMembership(AgencyMembership createdByMembership) {
        this.createdByMembership = Objects.requireNonNull(createdByMembership, "createdByMembership must not be null");
        if (!Objects.equals(createdByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("createdByMembership must belong to the same agency as the visit exception");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null").trim();
        narrative = Objects.requireNonNull(narrative, "narrative must not be null").trim();
        if (reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode must not be blank");
        }
        if (narrative.isBlank()) {
            throw new IllegalArgumentException("narrative must not be blank");
        }
        if (resolvedAt != null && status != VisitExceptionStatus.RESOLVED) {
            throw new IllegalArgumentException("resolvedAt requires RESOLVED status");
        }
    }
}
