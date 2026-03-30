package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
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
@Table(name = "invoice_export_rows")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceExportRow extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_profile_id")
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_payer_link_id")
    private PatientPayerLink patientPayerLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id")
    private PatientEpisodeAuthorization authorization;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "authorization_number", length = 120)
    private String authorizationNumber;

    @Column(name = "scheduled_start_at", nullable = false)
    private OffsetDateTime scheduledStartAt;

    @Column(name = "scheduled_end_at", nullable = false)
    private OffsetDateTime scheduledEndAt;

    @Column(name = "performed_start_at")
    private OffsetDateTime performedStartAt;

    @Column(name = "performed_end_at")
    private OffsetDateTime performedEndAt;

    @Column(name = "billable_units", nullable = false)
    private int billableUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 32)
    private RevenueReadinessStatus readinessStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_lifecycle_status", nullable = false, length = 32)
    private RevenueExportLifecycleStatus exportLifecycleStatus;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Builder
    private InvoiceExportRow(
            UUID id,
            VisitOccurrence visitOccurrence,
            Patient patient,
            CaregiverProfile caregiverProfile,
            Branch branch,
            ServiceLine serviceLine,
            PatientPayerLink patientPayerLink,
            PatientEpisodeAuthorization authorization,
            String payerName,
            String authorizationNumber,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            OffsetDateTime performedStartAt,
            OffsetDateTime performedEndAt,
            int billableUnits,
            RevenueReadinessStatus readinessStatus,
            RevenueExportLifecycleStatus exportLifecycleStatus,
            OffsetDateTime generatedAt) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignPatient(patient);
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignPatientPayerLink(patientPayerLink);
        assignAuthorization(authorization);
        this.payerName = payerName;
        this.authorizationNumber = authorizationNumber;
        this.scheduledStartAt = Objects.requireNonNull(scheduledStartAt, "scheduledStartAt must not be null");
        this.scheduledEndAt = Objects.requireNonNull(scheduledEndAt, "scheduledEndAt must not be null");
        this.performedStartAt = performedStartAt;
        this.performedEndAt = performedEndAt;
        this.billableUnits = billableUnits;
        this.readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus must not be null");
        this.exportLifecycleStatus = Objects.requireNonNull(exportLifecycleStatus, "exportLifecycleStatus must not be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        validateState();
    }

    public static InvoiceExportRow create(
            VisitOccurrence visitOccurrence,
            Patient patient,
            CaregiverProfile caregiverProfile,
            Branch branch,
            ServiceLine serviceLine,
            PatientPayerLink patientPayerLink,
            PatientEpisodeAuthorization authorization,
            String payerName,
            String authorizationNumber,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            OffsetDateTime performedStartAt,
            OffsetDateTime performedEndAt,
            int billableUnits,
            RevenueReadinessStatus readinessStatus,
            RevenueExportLifecycleStatus exportLifecycleStatus,
            OffsetDateTime generatedAt) {
        return InvoiceExportRow.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .patient(patient)
                .caregiverProfile(caregiverProfile)
                .branch(branch)
                .serviceLine(serviceLine)
                .patientPayerLink(patientPayerLink)
                .authorization(authorization)
                .payerName(payerName)
                .authorizationNumber(authorizationNumber)
                .scheduledStartAt(scheduledStartAt)
                .scheduledEndAt(scheduledEndAt)
                .performedStartAt(performedStartAt)
                .performedEndAt(performedEndAt)
                .billableUnits(billableUnits)
                .readinessStatus(readinessStatus)
                .exportLifecycleStatus(exportLifecycleStatus)
                .generatedAt(generatedAt)
                .build();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignPatient(Patient patient) {
        if (patient != null && !Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the invoice export row");
        }
        this.patient = patient;
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        if (caregiverProfile != null && !Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the invoice export row");
        }
        this.caregiverProfile = caregiverProfile;
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the invoice export row");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the invoice export row");
        }
        this.serviceLine = serviceLine;
    }

    private void assignPatientPayerLink(PatientPayerLink patientPayerLink) {
        if (patientPayerLink != null && !Objects.equals(patientPayerLink.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patientPayerLink must belong to the same agency as the invoice export row");
        }
        this.patientPayerLink = patientPayerLink;
    }

    private void assignAuthorization(PatientEpisodeAuthorization authorization) {
        if (authorization != null && !Objects.equals(authorization.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("authorization must belong to the same agency as the invoice export row");
        }
        this.authorization = authorization;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        payerName = normalizeOptional(payerName);
        authorizationNumber = normalizeOptional(authorizationNumber);
        assignPatient(patient);
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignPatientPayerLink(patientPayerLink);
        assignAuthorization(authorization);
        validateState();
    }

    private void validateState() {
        if (billableUnits < 0) {
            throw new IllegalArgumentException("billableUnits must be zero or greater");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
