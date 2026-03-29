package com.homehealthcare.patienteligibility.domain;

import com.homehealthcare.patient.domain.Patient;
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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_service_eligibilities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientServiceEligibility extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientServiceEligibilityStatus status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "verification_source", length = 120)
    private String verificationSource;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private PatientServiceEligibility(
            UUID id,
            Patient patient,
            ServiceLine serviceLine,
            PatientServiceEligibilityStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String verificationSource,
            String notes) {
        this.id = id;
        assignPatient(patient);
        assignServiceLine(serviceLine);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        this.effectiveTo = effectiveTo;
        this.verificationSource = verificationSource;
        this.notes = notes;
        validateDateWindow();
    }

    public static PatientServiceEligibility create(
            Patient patient,
            ServiceLine serviceLine,
            PatientServiceEligibilityStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String verificationSource,
            String notes) {
        return PatientServiceEligibility.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .serviceLine(serviceLine)
                .status(status)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .verificationSource(verificationSource)
                .notes(notes)
                .build();
    }

    public void update(
            ServiceLine serviceLine,
            PatientServiceEligibilityStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String verificationSource,
            String notes) {
        assignServiceLine(serviceLine);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        this.effectiveTo = effectiveTo;
        this.verificationSource = verificationSource;
        this.notes = notes;
        validateDateWindow();
    }

    public void deactivate() {
        this.status = PatientServiceEligibilityStatus.EXPIRED;
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && patient != null && !serviceLine.getAgencyId().equals(patient.getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the patient");
        }
        this.serviceLine = serviceLine;
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
        status = Objects.requireNonNull(status, "status must not be null");
        effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        validateDateWindow();
        verificationSource = normalizeOptional(verificationSource);
        notes = normalizeOptional(notes);
        assignServiceLine(serviceLine);
    }

    private void validateDateWindow() {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be on or after effectiveFrom");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
