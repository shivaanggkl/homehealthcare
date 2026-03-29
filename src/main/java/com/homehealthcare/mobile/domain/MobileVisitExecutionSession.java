package com.homehealthcare.mobile.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.mobile.foundation.MobileExecutionSessionStatus;
import com.homehealthcare.mobile.foundation.MobileSyncDisposition;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mobile_visit_execution_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileVisitExecutionSession extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "started_latitude", precision = 9, scale = 6)
    private BigDecimal startedLatitude;

    @Column(name = "started_longitude", precision = 9, scale = 6)
    private BigDecimal startedLongitude;

    @Column(name = "ended_latitude", precision = 9, scale = 6)
    private BigDecimal endedLatitude;

    @Column(name = "ended_longitude", precision = 9, scale = 6)
    private BigDecimal endedLongitude;

    @Column(name = "start_source", length = 32)
    private String startSource;

    @Column(name = "end_source", length = 32)
    private String endSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 32)
    private MobileExecutionSessionStatus executionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 32)
    private MobileSyncDisposition syncStatus;

    @Builder
    private MobileVisitExecutionSession(
            UUID id,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            BigDecimal startedLatitude,
            BigDecimal startedLongitude,
            BigDecimal endedLatitude,
            BigDecimal endedLongitude,
            String startSource,
            String endSource,
            MobileExecutionSessionStatus executionStatus,
            MobileSyncDisposition syncStatus) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.endedAt = endedAt;
        this.startedLatitude = startedLatitude;
        this.startedLongitude = startedLongitude;
        this.endedLatitude = endedLatitude;
        this.endedLongitude = endedLongitude;
        this.startSource = startSource;
        this.endSource = endSource;
        this.executionStatus = Objects.requireNonNull(executionStatus, "executionStatus must not be null");
        this.syncStatus = syncStatus;
        validateLifecycle();
    }

    public static MobileVisitExecutionSession start(
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            OffsetDateTime startedAt,
            BigDecimal startedLatitude,
            BigDecimal startedLongitude,
            String startSource,
            MobileSyncDisposition syncStatus) {
        return MobileVisitExecutionSession.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .caregiverProfile(caregiverProfile)
                .patient(patient)
                .branch(branch)
                .startedAt(startedAt)
                .startedLatitude(startedLatitude)
                .startedLongitude(startedLongitude)
                .startSource(startSource)
                .executionStatus(MobileExecutionSessionStatus.IN_PROGRESS)
                .syncStatus(syncStatus)
                .build();
    }

    public void complete(
            OffsetDateTime endedAt,
            BigDecimal endedLatitude,
            BigDecimal endedLongitude,
            String endSource,
            MobileSyncDisposition syncStatus) {
        this.endedAt = Objects.requireNonNull(endedAt, "endedAt must not be null");
        this.endedLatitude = endedLatitude;
        this.endedLongitude = endedLongitude;
        this.endSource = endSource;
        this.syncStatus = syncStatus;
        this.executionStatus = MobileExecutionSessionStatus.COMPLETED;
        validateLifecycle();
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile == null ? null : caregiverProfile.getId();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the execution session");
        }
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the execution session");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the execution session");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        startedLatitude = normalizeLatitude(startedLatitude, "startedLatitude");
        startedLongitude = normalizeLongitude(startedLongitude, "startedLongitude");
        endedLatitude = normalizeLatitude(endedLatitude, "endedLatitude");
        endedLongitude = normalizeLongitude(endedLongitude, "endedLongitude");
        startSource = normalizeCode(startSource);
        endSource = normalizeCode(endSource);
        executionStatus = Objects.requireNonNull(executionStatus, "executionStatus must not be null");
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        validateLifecycle();
    }

    private void validateLifecycle() {
        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must not be before startedAt");
        }
        if (executionStatus == MobileExecutionSessionStatus.COMPLETED && endedAt == null) {
            throw new IllegalArgumentException("Completed execution sessions must have endedAt");
        }
        if (executionStatus == MobileExecutionSessionStatus.IN_PROGRESS && endedAt != null) {
            throw new IllegalArgumentException("In-progress execution sessions must not have endedAt");
        }
    }

    private static String normalizeCode(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static BigDecimal normalizeLatitude(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.valueOf(-90)) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between -90 and 90");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeLongitude(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.valueOf(-180)) < 0 || value.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between -180 and 180");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
