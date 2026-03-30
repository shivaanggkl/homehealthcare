package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.revenuereadiness.foundation.RevenueUsagePosture;
import com.homehealthcare.serviceline.domain.ServiceLine;
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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "authorization_usage_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorizationUsageSnapshot extends AgencyScopedEntity {

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
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id")
    private PatientEpisodeAuthorization authorization;

    @Column(name = "authorized_units")
    private Integer authorizedUnits;

    @Column(name = "used_units", nullable = false)
    private int usedUnits;

    @Column(name = "remaining_units")
    private Integer remainingUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_posture", nullable = false, length = 32)
    private RevenueUsagePosture usagePosture;

    @Column(name = "counted_visit_count", nullable = false)
    private int countedVisitCount;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private AuthorizationUsageSnapshot(
            UUID id,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            PatientEpisodeAuthorization authorization,
            Integer authorizedUnits,
            int usedUnits,
            Integer remainingUnits,
            RevenueUsagePosture usagePosture,
            int countedVisitCount,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignAuthorization(authorization);
        this.authorizedUnits = authorizedUnits;
        this.usedUnits = usedUnits;
        this.remainingUnits = remainingUnits;
        this.usagePosture = Objects.requireNonNull(usagePosture, "usagePosture must not be null");
        this.countedVisitCount = countedVisitCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static AuthorizationUsageSnapshot create(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            PatientEpisodeAuthorization authorization,
            Integer authorizedUnits,
            int usedUnits,
            Integer remainingUnits,
            RevenueUsagePosture usagePosture,
            int countedVisitCount,
            OffsetDateTime evaluatedAt) {
        return AuthorizationUsageSnapshot.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .authorization(authorization)
                .authorizedUnits(authorizedUnits)
                .usedUnits(usedUnits)
                .remainingUnits(remainingUnits)
                .usagePosture(usagePosture)
                .countedVisitCount(countedVisitCount)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void refresh(
            Branch branch,
            ServiceLine serviceLine,
            PatientEpisodeAuthorization authorization,
            Integer authorizedUnits,
            int usedUnits,
            Integer remainingUnits,
            RevenueUsagePosture usagePosture,
            int countedVisitCount,
            OffsetDateTime evaluatedAt) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignAuthorization(authorization);
        this.authorizedUnits = authorizedUnits;
        this.usedUnits = usedUnits;
        this.remainingUnits = remainingUnits;
        this.usagePosture = Objects.requireNonNull(usagePosture, "usagePosture must not be null");
        this.countedVisitCount = countedVisitCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public UUID getAuthorizationId() {
        return authorization == null ? null : authorization.getId();
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
            throw new IllegalArgumentException("branch must belong to the same agency as the authorization usage snapshot");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the authorization usage snapshot");
        }
        this.serviceLine = serviceLine;
    }

    private void assignAuthorization(PatientEpisodeAuthorization authorization) {
        if (authorization != null && !Objects.equals(authorization.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("authorization must belong to the same agency as the authorization usage snapshot");
        }
        this.authorization = authorization;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignAuthorization(authorization);
        validateState();
    }

    private void validateState() {
        if (usedUnits < 0 || countedVisitCount < 0) {
            throw new IllegalArgumentException("Usage counters must be zero or greater");
        }
    }
}
