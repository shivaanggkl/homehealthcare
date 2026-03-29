package com.homehealthcare.caregivercredential.domain;

import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.certification.domain.CaregiverCertification;
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
@Table(name = "caregiver_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverCredential extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id")
    private CaregiverCertification certification;

    @Column(name = "credential_type", nullable = false, length = 120)
    private String credentialType;

    @Column(name = "license_number", length = 120)
    private String licenseNumber;

    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;

    @Column(name = "issued_on")
    private LocalDate issuedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CaregiverCredentialStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 32)
    private CaregiverCredentialVerificationStatus verificationStatus;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverCredential(
            UUID id,
            CaregiverProfile caregiverProfile,
            CaregiverCertification certification,
            String credentialType,
            String licenseNumber,
            String issuingAuthority,
            LocalDate issuedOn,
            LocalDate expiresOn,
            CaregiverCredentialStatus status,
            CaregiverCredentialVerificationStatus verificationStatus,
            String notes) {
        this.id = id;
        assignProfile(caregiverProfile);
        assignCertification(certification);
        this.credentialType = credentialType;
        this.licenseNumber = licenseNumber;
        this.issuingAuthority = issuingAuthority;
        this.issuedOn = issuedOn;
        this.expiresOn = expiresOn;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.verificationStatus = verificationStatus;
        this.notes = notes;
        validateDateWindow();
    }

    public static CaregiverCredential create(
            CaregiverProfile caregiverProfile,
            CaregiverCertification certification,
            String credentialType,
            String licenseNumber,
            String issuingAuthority,
            LocalDate issuedOn,
            LocalDate expiresOn,
            CaregiverCredentialStatus status,
            CaregiverCredentialVerificationStatus verificationStatus,
            String notes) {
        return CaregiverCredential.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .certification(certification)
                .credentialType(credentialType)
                .licenseNumber(licenseNumber)
                .issuingAuthority(issuingAuthority)
                .issuedOn(issuedOn)
                .expiresOn(expiresOn)
                .status(status)
                .verificationStatus(verificationStatus)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            CaregiverCertification certification,
            String credentialType,
            String licenseNumber,
            String issuingAuthority,
            LocalDate issuedOn,
            LocalDate expiresOn,
            CaregiverCredentialStatus status,
            CaregiverCredentialVerificationStatus verificationStatus,
            String notes) {
        assignCertification(certification);
        this.credentialType = credentialType;
        this.licenseNumber = licenseNumber;
        this.issuingAuthority = issuingAuthority;
        this.issuedOn = issuedOn;
        this.expiresOn = expiresOn;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.verificationStatus = verificationStatus;
        this.notes = notes;
        validateDateWindow();
    }

    public void deactivate() {
        this.status = CaregiverCredentialStatus.ARCHIVED;
    }

    private void assignProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        assignAgency(caregiverProfile.getAgency());
    }

    private void assignCertification(CaregiverCertification certification) {
        if (certification != null && !Objects.equals(certification.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("certification must belong to the same agency as the caregiver profile");
        }
        this.certification = certification;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (caregiverProfile == null) {
            throw new IllegalArgumentException("caregiverProfile must not be null");
        }
        credentialType = Objects.requireNonNull(credentialType, "credentialType must not be null").trim();
        licenseNumber = normalizeOptional(licenseNumber);
        issuingAuthority = normalizeOptional(issuingAuthority);
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        assignCertification(certification);
        validateDateWindow();
    }

    private void validateDateWindow() {
        if (issuedOn != null && expiresOn != null && expiresOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("expiresOn must be on or after issuedOn");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
