package com.homehealthcare.compliance.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.compliance.foundation.CertificationPeriodRecordState;
import com.homehealthcare.compliance.foundation.CertificationPeriodStatus;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
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
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "certification_period_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CertificationPeriodRecord extends AgencyScopedEntity {

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
    @JoinColumn(name = "patient_payer_link_id")
    private PatientPayerLink patientPayerLink;

    @Column(name = "program_context", length = 120)
    private String programContext;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_state", nullable = false, length = 24)
    private CertificationPeriodRecordState recordState;

    @Column(name = "source", length = 120)
    private String source;

    @Builder
    private CertificationPeriodRecord(
            UUID id,
            Patient patient,
            Branch branch,
            PatientPayerLink patientPayerLink,
            String programContext,
            LocalDate startDate,
            LocalDate endDate,
            CertificationPeriodRecordState recordState,
            String source) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignPatientPayerLink(patientPayerLink);
        this.programContext = programContext;
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.recordState = Objects.requireNonNull(recordState, "recordState must not be null");
        this.source = source;
        validateState();
    }

    public static CertificationPeriodRecord create(
            Patient patient,
            Branch branch,
            PatientPayerLink patientPayerLink,
            String programContext,
            LocalDate startDate,
            LocalDate endDate,
            boolean closed,
            String source) {
        return CertificationPeriodRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .patientPayerLink(patientPayerLink)
                .programContext(programContext)
                .startDate(startDate)
                .endDate(endDate)
                .recordState(closed ? CertificationPeriodRecordState.CLOSED : CertificationPeriodRecordState.ACTIVE)
                .source(source)
                .build();
    }

    public void updateDetails(
            Branch branch,
            PatientPayerLink patientPayerLink,
            String programContext,
            LocalDate startDate,
            LocalDate endDate,
            boolean closed,
            String source) {
        assignBranch(branch);
        assignPatientPayerLink(patientPayerLink);
        this.programContext = programContext;
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.recordState = closed ? CertificationPeriodRecordState.CLOSED : CertificationPeriodRecordState.ACTIVE;
        this.source = source;
        validateState();
    }

    public CertificationPeriodStatus projectStatus(LocalDate asOfDate, int expiryWarningDays) {
        if (asOfDate == null) {
            return CertificationPeriodStatus.MISSING;
        }
        if (endDate.isBefore(asOfDate)) {
            return CertificationPeriodStatus.EXPIRED;
        }
        if (!startDate.isAfter(asOfDate) && !endDate.isAfter(asOfDate.plusDays(Math.max(expiryWarningDays, 0)))) {
            return CertificationPeriodStatus.UPCOMING_EXPIRY;
        }
        if (!startDate.isAfter(asOfDate)) {
            return CertificationPeriodStatus.CURRENT;
        }
        return CertificationPeriodStatus.UPCOMING_EXPIRY;
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
            throw new IllegalArgumentException("branch must belong to the same agency as the certification period");
        }
        this.branch = branch;
    }

    private void assignPatientPayerLink(PatientPayerLink patientPayerLink) {
        if (patientPayerLink != null && !Objects.equals(patientPayerLink.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patientPayerLink must belong to the same agency as the certification period");
        }
        if (patientPayerLink != null && patient != null && !Objects.equals(patientPayerLink.getPatient().getId(), patient.getId())) {
            throw new IllegalArgumentException("patientPayerLink must belong to the same patient as the certification period");
        }
        this.patientPayerLink = patientPayerLink;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        programContext = optional(programContext);
        source = optionalUpper(source);
        assignBranch(branch);
        assignPatientPayerLink(patientPayerLink);
        validateState();
    }

    private void validateState() {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
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
