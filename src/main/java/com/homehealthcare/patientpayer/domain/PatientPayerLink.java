package com.homehealthcare.patientpayer.domain;

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
@Table(name = "patient_payer_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientPayerLink extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "payer_name", length = 200)
    private String payerName;

    @Column(name = "payer_external_id", length = 100)
    private String payerExternalId;

    @Column(name = "member_policy_number", length = 120)
    private String memberPolicyNumber;

    @Column(name = "group_number", length = 120)
    private String groupNumber;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "primary_payer", nullable = false)
    private boolean primaryPayer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientPayerLinkStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private PatientPayerLink(
            UUID id,
            Patient patient,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String groupNumber,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean primaryPayer,
            PatientPayerLinkStatus status,
            String notes) {
        this.id = id;
        assignPatient(patient);
        this.payerName = payerName;
        this.payerExternalId = payerExternalId;
        this.memberPolicyNumber = memberPolicyNumber;
        this.groupNumber = groupNumber;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        this.effectiveTo = effectiveTo;
        this.primaryPayer = primaryPayer;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateState();
    }

    public static PatientPayerLink create(
            Patient patient,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String groupNumber,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean primaryPayer,
            PatientPayerLinkStatus status,
            String notes) {
        return PatientPayerLink.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .payerName(payerName)
                .payerExternalId(payerExternalId)
                .memberPolicyNumber(memberPolicyNumber)
                .groupNumber(groupNumber)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .primaryPayer(primaryPayer)
                .status(status)
                .notes(notes)
                .build();
    }

    public void update(
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String groupNumber,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean primaryPayer,
            PatientPayerLinkStatus status,
            String notes) {
        this.payerName = payerName;
        this.payerExternalId = payerExternalId;
        this.memberPolicyNumber = memberPolicyNumber;
        this.groupNumber = groupNumber;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        this.effectiveTo = effectiveTo;
        this.primaryPayer = primaryPayer;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateState();
    }

    public void deactivate() {
        this.status = PatientPayerLinkStatus.INACTIVE;
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
        payerName = normalizeOptional(payerName);
        payerExternalId = normalizeOptional(payerExternalId);
        memberPolicyNumber = normalizeOptional(memberPolicyNumber);
        groupNumber = normalizeOptional(groupNumber);
        notes = normalizeOptional(notes);
        validateState();
    }

    private void validateState() {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be on or after effectiveFrom");
        }
        if (normalizeOptional(payerName) == null && normalizeOptional(payerExternalId) == null) {
            throw new IllegalArgumentException("payerName or payerExternalId must be provided");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
