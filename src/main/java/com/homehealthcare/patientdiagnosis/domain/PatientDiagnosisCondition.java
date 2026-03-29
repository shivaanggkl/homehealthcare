package com.homehealthcare.patientdiagnosis.domain;

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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_diagnosis_conditions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientDiagnosisCondition extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "diagnosis_code", length = 50)
    private String diagnosisCode;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "diagnosis_type", length = 100)
    private String diagnosisType;

    @Column(name = "primary_condition", nullable = false)
    private boolean primaryCondition;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientDiagnosisStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private PatientDiagnosisCondition(
            UUID id,
            Patient patient,
            String diagnosisCode,
            String description,
            String diagnosisType,
            boolean primaryCondition,
            LocalDate onsetDate,
            LocalDate resolvedDate,
            PatientDiagnosisStatus status,
            String notes) {
        this.id = id;
        assignPatient(patient);
        this.diagnosisCode = diagnosisCode;
        this.description = description;
        this.diagnosisType = diagnosisType;
        this.primaryCondition = primaryCondition;
        this.onsetDate = onsetDate;
        this.resolvedDate = resolvedDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateClinicalDates();
    }

    public static PatientDiagnosisCondition create(
            Patient patient,
            String diagnosisCode,
            String description,
            String diagnosisType,
            boolean primaryCondition,
            LocalDate onsetDate,
            LocalDate resolvedDate,
            PatientDiagnosisStatus status,
            String notes) {
        return PatientDiagnosisCondition.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .diagnosisCode(diagnosisCode)
                .description(description)
                .diagnosisType(diagnosisType)
                .primaryCondition(primaryCondition)
                .onsetDate(onsetDate)
                .resolvedDate(resolvedDate)
                .status(status)
                .notes(notes)
                .build();
    }

    public void update(
            String diagnosisCode,
            String description,
            String diagnosisType,
            boolean primaryCondition,
            LocalDate onsetDate,
            LocalDate resolvedDate,
            PatientDiagnosisStatus status,
            String notes) {
        this.diagnosisCode = diagnosisCode;
        this.description = description;
        this.diagnosisType = diagnosisType;
        this.primaryCondition = primaryCondition;
        this.onsetDate = onsetDate;
        this.resolvedDate = resolvedDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateClinicalDates();
    }

    public void deactivate() {
        this.status = PatientDiagnosisStatus.INACTIVE;
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (patient == null) {
            throw new IllegalArgumentException("patient must not be null");
        }
        diagnosisCode = normalizeOptional(diagnosisCode);
        description = normalizeRequired(description);
        diagnosisType = normalizeOptional(diagnosisType);
        status = Objects.requireNonNull(status, "status must not be null");
        validateClinicalDates();
        notes = normalizeOptional(notes);
    }

    private void validateClinicalDates() {
        if (onsetDate != null && resolvedDate != null && resolvedDate.isBefore(onsetDate)) {
            throw new IllegalArgumentException("resolvedDate must be on or after onsetDate");
        }
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
