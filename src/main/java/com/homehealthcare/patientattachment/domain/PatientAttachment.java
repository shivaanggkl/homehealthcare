package com.homehealthcare.patientattachment.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_attachments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientAttachment extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_membership_id", nullable = false)
    private AgencyMembership uploaderMembership;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "uploader_email", nullable = false, length = 320)
    private String uploaderEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientAttachmentStatus status;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder
    private PatientAttachment(
            UUID id,
            Patient patient,
            AgencyMembership uploaderMembership,
            String fileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String category,
            String uploaderEmail,
            PatientAttachmentStatus status,
            String description) {
        this.id = id;
        assignPatient(patient);
        assignUploaderMembership(uploaderMembership);
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.category = category;
        this.uploaderEmail = uploaderEmail;
        this.status = status;
        this.description = description;
    }

    public static PatientAttachment create(
            Patient patient,
            AgencyMembership uploaderMembership,
            String fileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String category,
            String description) {
        return PatientAttachment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .uploaderMembership(uploaderMembership)
                .fileName(fileName)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .storageKey(storageKey)
                .category(category)
                .uploaderEmail(uploaderMembership.getUser().getEmail())
                .status(PatientAttachmentStatus.ACTIVE)
                .description(description)
                .build();
    }

    public void updateMetadata(String category, String description) {
        this.category = category;
        this.description = description;
    }

    public void archive() {
        this.status = PatientAttachmentStatus.ARCHIVED;
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignUploaderMembership(AgencyMembership uploaderMembership) {
        this.uploaderMembership = Objects.requireNonNull(uploaderMembership, "uploaderMembership must not be null");
        if (getAgency() != null && !getAgencyId().equals(uploaderMembership.getAgencyId())) {
            throw new IllegalArgumentException("uploaderMembership must belong to the same agency as the patient");
        }
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
        if (uploaderMembership == null) {
            throw new IllegalArgumentException("uploaderMembership must not be null");
        }
        fileName = normalizeRequired(fileName);
        contentType = normalizeContentType(contentType);
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be zero or greater");
        }
        storageKey = normalizeRequired(storageKey);
        category = normalizeRequired(category);
        uploaderEmail = normalizeEmail(uploaderEmail);
        status = Objects.requireNonNull(status, "status must not be null");
        description = normalizeOptional(description);
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value);
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeContentType(String value) {
        String normalized = normalizeRequired(value);
        return normalized.toLowerCase(Locale.ROOT);
    }
}
