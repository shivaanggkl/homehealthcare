package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientevent.foundation.InfectionRecordStatus;
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
@Table(name = "infection_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfectionRecord extends AgencyScopedEntity {

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
    @JoinColumn(name = "related_incident_id")
    private IncidentRecord relatedIncident;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "identified_at", nullable = false)
    private OffsetDateTime identifiedAt;

    @Column(name = "infection_type", nullable = false, length = 120)
    private String infectionType;

    @Column(name = "summary", nullable = false, length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InfectionRecordStatus status;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private InfectionRecord(
            UUID id,
            Patient patient,
            Branch branch,
            IncidentRecord relatedIncident,
            LocalDate onsetDate,
            OffsetDateTime identifiedAt,
            String infectionType,
            String summary,
            InfectionRecordStatus status,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignRelatedIncident(relatedIncident);
        this.onsetDate = onsetDate;
        this.identifiedAt = Objects.requireNonNull(identifiedAt, "identifiedAt must not be null");
        this.infectionType = infectionType;
        this.summary = summary;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.resolvedAt = resolvedAt;
        validateState();
    }

    public static InfectionRecord create(
            Patient patient,
            Branch branch,
            IncidentRecord relatedIncident,
            LocalDate onsetDate,
            OffsetDateTime identifiedAt,
            String infectionType,
            String summary,
            InfectionRecordStatus status) {
        return InfectionRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .relatedIncident(relatedIncident)
                .onsetDate(onsetDate)
                .identifiedAt(identifiedAt)
                .infectionType(infectionType)
                .summary(summary)
                .status(status == null ? InfectionRecordStatus.ACTIVE : status)
                .build();
    }

    public void updateDetails(
            Branch branch,
            IncidentRecord relatedIncident,
            LocalDate onsetDate,
            OffsetDateTime identifiedAt,
            String infectionType,
            String summary,
            InfectionRecordStatus status) {
        assignBranch(branch);
        assignRelatedIncident(relatedIncident);
        this.onsetDate = onsetDate;
        this.identifiedAt = Objects.requireNonNull(identifiedAt, "identifiedAt must not be null");
        this.infectionType = infectionType;
        this.summary = summary;
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (status != InfectionRecordStatus.RESOLVED) {
            this.resolvedAt = null;
        }
        validateState();
    }

    public void resolve(OffsetDateTime resolvedAt) {
        this.status = InfectionRecordStatus.RESOLVED;
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
            throw new IllegalArgumentException("branch must belong to the same agency as the infection record");
        }
        this.branch = branch;
    }

    private void assignRelatedIncident(IncidentRecord relatedIncident) {
        if (relatedIncident != null) {
            if (!Objects.equals(relatedIncident.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("relatedIncident must belong to the same agency as the infection record");
            }
            if (!Objects.equals(relatedIncident.getPatientId(), patient.getId())) {
                throw new IllegalArgumentException("relatedIncident must belong to the same patient as the infection record");
            }
        }
        this.relatedIncident = relatedIncident;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        infectionType = required(infectionType).toUpperCase(Locale.ROOT);
        summary = required(summary);
        assignBranch(branch);
        assignRelatedIncident(relatedIncident);
        validateState();
    }

    private void validateState() {
        if (onsetDate != null && identifiedAt.toLocalDate().isBefore(onsetDate)) {
            throw new IllegalArgumentException("identifiedAt must not be before onsetDate");
        }
        if (status == InfectionRecordStatus.RESOLVED && resolvedAt == null) {
            throw new IllegalArgumentException("resolved infections must record resolvedAt");
        }
        if (status != InfectionRecordStatus.RESOLVED && resolvedAt != null) {
            throw new IllegalArgumentException("active infections must not have resolvedAt");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }
}
