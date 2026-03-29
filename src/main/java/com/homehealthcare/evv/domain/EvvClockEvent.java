package com.homehealthcare.evv.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.evv.foundation.EvvVerificationStatus;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "evv_clock_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvvClockEvent extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_session_id", nullable = false)
    private EvvVerificationSession verificationSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EvvClockEventType eventType;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "captured_latitude", precision = 9, scale = 6)
    private BigDecimal capturedLatitude;

    @Column(name = "captured_longitude", precision = 9, scale = 6)
    private BigDecimal capturedLongitude;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "capture_source", nullable = false, length = 32)
    private String captureSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 40)
    private EvvVerificationStatus verificationStatus;

    @Builder
    private EvvClockEvent(
            UUID id,
            EvvVerificationSession verificationSession,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            EvvClockEventType eventType,
            OffsetDateTime capturedAt,
            BigDecimal capturedLatitude,
            BigDecimal capturedLongitude,
            String timezone,
            String captureSource,
            EvvVerificationStatus verificationStatus) {
        this.id = id;
        assignVerificationSession(verificationSession);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        this.capturedLatitude = capturedLatitude;
        this.capturedLongitude = capturedLongitude;
        this.timezone = timezone;
        this.captureSource = captureSource;
        this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    }

    public static EvvClockEvent record(
            EvvVerificationSession verificationSession,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            EvvClockEventType eventType,
            OffsetDateTime capturedAt,
            BigDecimal capturedLatitude,
            BigDecimal capturedLongitude,
            String timezone,
            String captureSource,
            EvvVerificationStatus verificationStatus) {
        return EvvClockEvent.builder()
                .id(UUID.randomUUID())
                .verificationSession(verificationSession)
                .caregiverProfile(caregiverProfile)
                .patient(patient)
                .branch(branch)
                .eventType(eventType)
                .capturedAt(capturedAt)
                .capturedLatitude(capturedLatitude)
                .capturedLongitude(capturedLongitude)
                .timezone(timezone)
                .captureSource(captureSource)
                .verificationStatus(verificationStatus)
                .build();
    }

    public UUID getVerificationSessionId() {
        return verificationSession == null ? null : verificationSession.getId();
    }

    private void assignVerificationSession(EvvVerificationSession verificationSession) {
        this.verificationSession = Objects.requireNonNull(verificationSession, "verificationSession must not be null");
        assignAgency(verificationSession.getAgency());
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the clock event");
        }
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the clock event");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the clock event");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        capturedLatitude = normalizeLatitude(capturedLatitude, "capturedLatitude");
        capturedLongitude = normalizeLongitude(capturedLongitude, "capturedLongitude");
        timezone = Objects.requireNonNull(timezone, "timezone must not be null").trim();
        ZoneId.of(timezone);
        captureSource = Objects.requireNonNull(captureSource, "captureSource must not be null").trim().toUpperCase(Locale.ROOT);
        verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
    }

    private static BigDecimal normalizeLatitude(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.valueOf(-90)) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException(label + " must be between -90 and 90");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeLongitude(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.valueOf(-180)) < 0 || value.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException(label + " must be between -180 and 180");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
