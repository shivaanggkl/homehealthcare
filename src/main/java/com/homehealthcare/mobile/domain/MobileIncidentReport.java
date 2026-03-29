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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mobile_incident_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileIncidentReport extends AgencyScopedEntity {

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

    @Column(name = "incident_type", nullable = false, length = 80)
    private String incidentType;

    @Column(name = "severity", length = 40)
    private String severity;

    @Column(name = "narrative", nullable = false, length = 4000)
    private String narrative;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MobileIncidentStatus status;

    @Column(name = "escalation_hook", length = 120)
    private String escalationHook;

    @ManyToMany
    @JoinTable(
            name = "mobile_incident_artifact_refs",
            joinColumns = @JoinColumn(name = "incident_report_id"),
            inverseJoinColumns = @JoinColumn(name = "artifact_id"))
    private Set<MobileFieldArtifact> artifacts = new LinkedHashSet<>();

    @Builder
    private MobileIncidentReport(
            UUID id,
            MobileVisitExecutionSession executionSession,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            String incidentType,
            String severity,
            String narrative,
            OffsetDateTime reportedAt,
            MobileIncidentStatus status,
            String escalationHook,
            Set<MobileFieldArtifact> artifacts) {
        this.id = id;
        assignExecutionSession(executionSession);
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        this.incidentType = incidentType;
        this.severity = severity;
        this.narrative = narrative;
        this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.escalationHook = escalationHook;
        setArtifacts(artifacts);
    }

    public static MobileIncidentReport create(
            MobileVisitExecutionSession executionSession,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Patient patient,
            Branch branch,
            String incidentType,
            String severity,
            String narrative,
            OffsetDateTime reportedAt,
            String escalationHook,
            Set<MobileFieldArtifact> artifacts) {
        return MobileIncidentReport.builder()
                .id(UUID.randomUUID())
                .executionSession(executionSession)
                .visitOccurrence(visitOccurrence)
                .caregiverProfile(caregiverProfile)
                .patient(patient)
                .branch(branch)
                .incidentType(incidentType)
                .severity(severity)
                .narrative(narrative)
                .reportedAt(reportedAt)
                .status(MobileIncidentStatus.OPEN)
                .escalationHook(escalationHook)
                .artifacts(artifacts)
                .build();
    }

    private void assignExecutionSession(MobileVisitExecutionSession executionSession) {
        this.executionSession = Objects.requireNonNull(executionSession, "executionSession must not be null");
        assignAgency(executionSession.getAgency());
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        if (!Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the incident");
        }
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the incident");
        }
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the incident");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the incident");
        }
        this.branch = branch;
    }

    private void setArtifacts(Set<MobileFieldArtifact> artifacts) {
        this.artifacts.clear();
        if (artifacts == null) {
            return;
        }
        artifacts.forEach(artifact -> {
            if (!Objects.equals(artifact.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("artifacts must belong to the same agency as the incident");
            }
            this.artifacts.add(artifact);
        });
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        incidentType = required(incidentType).toUpperCase(Locale.ROOT);
        severity = optional(severity);
        if (severity != null) {
            severity = severity.toUpperCase(Locale.ROOT);
        }
        narrative = required(narrative);
        escalationHook = optional(escalationHook);
        status = Objects.requireNonNull(status, "status must not be null");
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignPatient(patient);
        assignBranch(branch);
        setArtifacts(artifacts);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
