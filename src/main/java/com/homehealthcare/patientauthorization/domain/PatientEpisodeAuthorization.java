package com.homehealthcare.patientauthorization.domain;

import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
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
@Table(name = "patient_episode_authorizations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientEpisodeAuthorization extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_payer_link_id")
    private PatientPayerLink patientPayerLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Column(name = "authorization_number", length = 120)
    private String authorizationNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "authorized_units")
    private Integer authorizedUnits;

    @Column(name = "used_units")
    private Integer usedUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientEpisodeAuthorizationStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private PatientEpisodeAuthorization(
            UUID id,
            Patient patient,
            PatientPayerLink patientPayerLink,
            ServiceLine serviceLine,
            String authorizationNumber,
            LocalDate startDate,
            LocalDate endDate,
            Integer authorizedUnits,
            Integer usedUnits,
            PatientEpisodeAuthorizationStatus status,
            String notes) {
        this.id = id;
        assignPatient(patient);
        assignPayerLink(patientPayerLink);
        assignServiceLine(serviceLine);
        this.authorizationNumber = authorizationNumber;
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.authorizedUnits = authorizedUnits;
        this.usedUnits = usedUnits;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateState();
    }

    public static PatientEpisodeAuthorization create(
            Patient patient,
            PatientPayerLink patientPayerLink,
            ServiceLine serviceLine,
            String authorizationNumber,
            LocalDate startDate,
            LocalDate endDate,
            Integer authorizedUnits,
            Integer usedUnits,
            PatientEpisodeAuthorizationStatus status,
            String notes) {
        return PatientEpisodeAuthorization.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .patientPayerLink(patientPayerLink)
                .serviceLine(serviceLine)
                .authorizationNumber(authorizationNumber)
                .startDate(startDate)
                .endDate(endDate)
                .authorizedUnits(authorizedUnits)
                .usedUnits(usedUnits)
                .status(status)
                .notes(notes)
                .build();
    }

    public void update(
            PatientPayerLink patientPayerLink,
            ServiceLine serviceLine,
            String authorizationNumber,
            LocalDate startDate,
            LocalDate endDate,
            Integer authorizedUnits,
            Integer usedUnits,
            PatientEpisodeAuthorizationStatus status,
            String notes) {
        assignPayerLink(patientPayerLink);
        assignServiceLine(serviceLine);
        this.authorizationNumber = authorizationNumber;
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        this.authorizedUnits = authorizedUnits;
        this.usedUnits = usedUnits;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateState();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignPayerLink(PatientPayerLink patientPayerLink) {
        if (patientPayerLink != null && patient != null && !patientPayerLink.getPatient().getId().equals(patient.getId())) {
            throw new IllegalArgumentException("patientPayerLink must belong to the same patient");
        }
        this.patientPayerLink = patientPayerLink;
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
        authorizationNumber = normalizeOptional(authorizationNumber);
        notes = normalizeOptional(notes);
        validateState();
        assignPayerLink(patientPayerLink);
        assignServiceLine(serviceLine);
    }

    private void validateState() {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        if (authorizedUnits != null && authorizedUnits < 0) {
            throw new IllegalArgumentException("authorizedUnits must be zero or greater");
        }
        if (usedUnits != null && usedUnits < 0) {
            throw new IllegalArgumentException("usedUnits must be zero or greater");
        }
        if (authorizedUnits != null && usedUnits != null && usedUnits > authorizedUnits) {
            throw new IllegalArgumentException("usedUnits must be less than or equal to authorizedUnits");
        }
        if (status == PatientEpisodeAuthorizationStatus.EXHAUSTED && authorizedUnits == null) {
            throw new IllegalArgumentException("authorizedUnits must be provided when status is EXHAUSTED");
        }
        if (status == PatientEpisodeAuthorizationStatus.EXHAUSTED
                && authorizedUnits != null
                && (usedUnits == null || usedUnits < authorizedUnits)) {
            throw new IllegalArgumentException("usedUnits must meet or exceed authorizedUnits when status is EXHAUSTED");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
