package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientevent.foundation.IncidentRecordStatus;
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
@Table(name = "incident_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_occurrence_id")
    private VisitOccurrence visitOccurrence;

    @Column(name = "incident_type", nullable = false, length = 100)
    private String incidentType;

    @Column(name = "severity_label", length = 60)
    private String severityLabel;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Column(name = "summary", nullable = false, length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentRecordStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_membership_id")
    private AgencyMembership reportedByMembership;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private IncidentRecord(
            UUID id,
            Patient patient,
            Branch branch,
            VisitOccurrence visitOccurrence,
            String incidentType,
            String severityLabel,
            OffsetDateTime occurredAt,
            OffsetDateTime reportedAt,
            String summary,
            IncidentRecordStatus status,
            AgencyMembership reportedByMembership,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignVisitOccurrence(visitOccurrence);
        assignReportedByMembership(reportedByMembership);
        this.incidentType = incidentType;
        this.severityLabel = severityLabel;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt must not be null");
        this.summary = summary;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.resolvedAt = resolvedAt;
        validateState();
    }

    public static IncidentRecord create(
            Patient patient,
            Branch branch,
            VisitOccurrence visitOccurrence,
            String incidentType,
            String severityLabel,
            OffsetDateTime occurredAt,
            OffsetDateTime reportedAt,
            String summary,
            AgencyMembership reportedByMembership) {
        return IncidentRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .visitOccurrence(visitOccurrence)
                .incidentType(incidentType)
                .severityLabel(severityLabel)
                .occurredAt(occurredAt)
                .reportedAt(reportedAt)
                .summary(summary)
                .status(IncidentRecordStatus.OPEN)
                .reportedByMembership(reportedByMembership)
                .build();
    }

    public void updateDetails(
            Branch branch,
            VisitOccurrence visitOccurrence,
            String incidentType,
            String severityLabel,
            OffsetDateTime occurredAt,
            OffsetDateTime reportedAt,
            String summary,
            IncidentRecordStatus status,
            AgencyMembership reportedByMembership) {
        assignBranch(branch);
        assignVisitOccurrence(visitOccurrence);
        assignReportedByMembership(reportedByMembership);
        this.incidentType = incidentType;
        this.severityLabel = severityLabel;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt must not be null");
        this.summary = summary;
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (status == IncidentRecordStatus.OPEN || status == IncidentRecordStatus.IN_REVIEW) {
            this.resolvedAt = null;
        }
        validateState();
    }

    public void resolve(OffsetDateTime resolvedAt) {
        this.status = IncidentRecordStatus.RESOLVED;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the incident");
        }
        this.branch = branch;
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        if (visitOccurrence != null) {
            if (!Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the incident");
            }
            if (!Objects.equals(visitOccurrence.getPatient().getId(), patient.getId())) {
                throw new IllegalArgumentException("visitOccurrence must belong to the same patient as the incident");
            }
        }
        this.visitOccurrence = visitOccurrence;
    }

    private void assignReportedByMembership(AgencyMembership reportedByMembership) {
        if (reportedByMembership != null && !Objects.equals(reportedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("reportedByMembership must belong to the same agency as the incident");
        }
        this.reportedByMembership = reportedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        incidentType = required(incidentType).toUpperCase(Locale.ROOT);
        severityLabel = optionalUpper(severityLabel);
        summary = required(summary);
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignVisitOccurrence(visitOccurrence);
        assignReportedByMembership(reportedByMembership);
        validateState();
    }

    private void validateState() {
        if (reportedAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException("reportedAt must not be before occurredAt");
        }
        if ((status == IncidentRecordStatus.RESOLVED || status == IncidentRecordStatus.CLOSED) && resolvedAt == null) {
            throw new IllegalArgumentException("resolved incidents must record resolvedAt");
        }
        if ((status == IncidentRecordStatus.OPEN || status == IncidentRecordStatus.IN_REVIEW) && resolvedAt != null) {
            throw new IllegalArgumentException("open incidents must not have resolvedAt");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optionalUpper(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
