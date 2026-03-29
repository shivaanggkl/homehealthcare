package com.homehealthcare.patientcontact.domain;

import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_contacts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientContact extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "relationship_type", length = 100)
    private String relationshipType;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "emergency_contact", nullable = false)
    private boolean emergencyContact;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact;

    @Column(name = "responsible_party", nullable = false)
    private boolean responsibleParty;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientLifecycleStatus status;

    @Builder
    private PatientContact(
            UUID id,
            Patient patient,
            String relationshipType,
            String fullName,
            String phone,
            String email,
            String address,
            boolean emergencyContact,
            boolean primaryContact,
            boolean responsibleParty,
            String notes,
            PatientLifecycleStatus status) {
        this.id = id;
        assignPatient(patient);
        this.relationshipType = relationshipType;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.primaryContact = primaryContact;
        this.responsibleParty = responsibleParty;
        this.notes = notes;
        this.status = status;
    }

    public static PatientContact create(
            Patient patient,
            String relationshipType,
            String fullName,
            String phone,
            String email,
            String address,
            boolean emergencyContact,
            boolean primaryContact,
            boolean responsibleParty,
            String notes) {
        return PatientContact.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .relationshipType(relationshipType)
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .address(address)
                .emergencyContact(emergencyContact)
                .primaryContact(primaryContact)
                .responsibleParty(responsibleParty)
                .notes(notes)
                .status(PatientLifecycleStatus.ACTIVE)
                .build();
    }

    public void updateDetails(
            String relationshipType,
            String fullName,
            String phone,
            String email,
            String address,
            boolean emergencyContact,
            boolean primaryContact,
            boolean responsibleParty,
            String notes) {
        this.relationshipType = relationshipType;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.primaryContact = primaryContact;
        this.responsibleParty = responsibleParty;
        this.notes = notes;
    }

    public void deactivate() {
        this.status = PatientLifecycleStatus.INACTIVE;
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
        relationshipType = normalizeOptional(relationshipType);
        fullName = normalizeRequired(fullName);
        phone = normalizeOptional(phone);
        email = normalizeEmail(email);
        address = normalizeOptional(address);
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
