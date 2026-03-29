package com.homehealthcare.mobile.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mobile_field_artifacts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileFieldArtifact extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_session_id", nullable = false)
    private MobileVisitExecutionSession executionSession;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 32)
    private MobileFieldArtifactType artifactType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MobileFieldArtifactStatus status;

    @Builder
    private MobileFieldArtifact(
            UUID id,
            MobileVisitExecutionSession executionSession,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            MobileFieldArtifactType artifactType,
            String fileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String description,
            MobileFieldArtifactStatus status) {
        this.id = id;
        assignExecutionSession(executionSession);
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        this.artifactType = Objects.requireNonNull(artifactType, "artifactType must not be null");
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.description = description;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static MobileFieldArtifact create(
            MobileVisitExecutionSession executionSession,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            MobileFieldArtifactType artifactType,
            String fileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String description) {
        return MobileFieldArtifact.builder()
                .id(UUID.randomUUID())
                .executionSession(executionSession)
                .visitOccurrence(visitOccurrence)
                .caregiverProfile(caregiverProfile)
                .patient(patient)
                .branch(branch)
                .artifactType(artifactType)
                .fileName(fileName)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .storageKey(storageKey)
                .description(description)
                .status(MobileFieldArtifactStatus.ACTIVE)
                .build();
    }

    public UUID getExecutionSessionId() {
        return executionSession == null ? null : executionSession.getId();
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    private void assignExecutionSession(MobileVisitExecutionSession executionSession) {
        this.executionSession = Objects.requireNonNull(executionSession, "executionSession must not be null");
        assignAgency(executionSession.getAgency());
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        if (!Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the artifact");
        }
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the artifact");
        }
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the artifact");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the artifact");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        fileName = required(fileName);
        contentType = required(contentType).toLowerCase(Locale.ROOT);
        storageKey = required(storageKey);
        description = optional(description);
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be greater than 0");
        }
        artifactType = Objects.requireNonNull(artifactType, "artifactType must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
