package com.homehealthcare.patient.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "patients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "preferred_name", length = 100)
    private String preferredName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "sex_marker", length = 50)
    private String sexMarker;

    @Column(name = "primary_phone", length = 30)
    private String primaryPhone;

    @Column(name = "secondary_phone", length = 30)
    private String secondaryPhone;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "language", length = 35)
    private String language;

    @Column(name = "notes_summary", length = 1000)
    private String notesSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientLifecycleStatus status;

    @Builder
    private Patient(
            UUID id,
            Agency agency,
            String externalReference,
            String firstName,
            String middleName,
            String lastName,
            String preferredName,
            LocalDate dateOfBirth,
            String sexMarker,
            String primaryPhone,
            String secondaryPhone,
            String email,
            String language,
            String notesSummary,
            PatientLifecycleStatus status) {
        this.id = id;
        assignAgency(agency);
        this.externalReference = externalReference;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.preferredName = preferredName;
        this.dateOfBirth = dateOfBirth;
        this.sexMarker = sexMarker;
        this.primaryPhone = primaryPhone;
        this.secondaryPhone = secondaryPhone;
        this.email = email;
        this.language = language;
        this.notesSummary = notesSummary;
        this.status = status;
    }

    public static Patient create(
            Agency agency,
            String externalReference,
            String firstName,
            String middleName,
            String lastName,
            String preferredName,
            LocalDate dateOfBirth,
            String sexMarker,
            String primaryPhone,
            String secondaryPhone,
            String email,
            String language,
            String notesSummary) {
        return Patient.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .externalReference(externalReference)
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .preferredName(preferredName)
                .dateOfBirth(dateOfBirth)
                .sexMarker(sexMarker)
                .primaryPhone(primaryPhone)
                .secondaryPhone(secondaryPhone)
                .email(email)
                .language(language)
                .notesSummary(notesSummary)
                .status(PatientLifecycleStatus.ACTIVE)
                .build();
    }

    public void updateDetails(
            String externalReference,
            String firstName,
            String middleName,
            String lastName,
            String preferredName,
            LocalDate dateOfBirth,
            String sexMarker,
            String primaryPhone,
            String secondaryPhone,
            String email,
            String language,
            String notesSummary) {
        this.externalReference = externalReference;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.preferredName = preferredName;
        this.dateOfBirth = dateOfBirth;
        this.sexMarker = sexMarker;
        this.primaryPhone = primaryPhone;
        this.secondaryPhone = secondaryPhone;
        this.email = email;
        this.language = language;
        this.notesSummary = notesSummary;
    }

    public void deactivate() {
        this.status = PatientLifecycleStatus.INACTIVE;
    }

    public void archive() {
        this.status = PatientLifecycleStatus.ARCHIVED;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        externalReference = normalizeOptional(externalReference);
        firstName = normalizeRequired(firstName);
        middleName = normalizeOptional(middleName);
        lastName = normalizeRequired(lastName);
        preferredName = normalizeOptional(preferredName);
        dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth must not be null");
        sexMarker = normalizeOptional(sexMarker);
        primaryPhone = normalizeOptional(primaryPhone);
        secondaryPhone = normalizeOptional(secondaryPhone);
        email = normalizeEmail(email);
        language = normalizeLanguageTag(language);
        notesSummary = normalizeOptional(notesSummary);
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

    private static String normalizeLanguageTag(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("(?i)^[a-z]{2,3}(-[a-z]{2})?$")) {
            throw new IllegalArgumentException("Invalid patient language tag");
        }
        return Locale.forLanguageTag(normalized).toLanguageTag();
    }
}
