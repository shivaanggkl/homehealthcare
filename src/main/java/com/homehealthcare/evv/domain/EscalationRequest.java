package com.homehealthcare.evv.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
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
@Table(name = "escalation_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EscalationRequest extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missed_visit_record_id")
    private MissedVisitRecord missedVisitRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_exception_record_id")
    private VisitExceptionRecord visitExceptionRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_membership_id", nullable = false)
    private AgencyMembership createdByMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "target_role_key", nullable = false, length = 64)
    private String targetRoleKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private VisitExceptionSeverity severity;

    @Column(name = "rationale", nullable = false, length = 1000)
    private String rationale;

    @Column(name = "sla_due_at")
    private OffsetDateTime slaDueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EscalationStatus status;

    @Builder
    private EscalationRequest(
            UUID id,
            MissedVisitRecord missedVisitRecord,
            VisitExceptionRecord visitExceptionRecord,
            AgencyMembership createdByMembership,
            Branch branch,
            String targetRoleKey,
            VisitExceptionSeverity severity,
            String rationale,
            OffsetDateTime slaDueAt,
            EscalationStatus status) {
        this.id = id;
        assignSource(missedVisitRecord, visitExceptionRecord);
        assignCreatedByMembership(createdByMembership);
        assignBranch(branch);
        this.targetRoleKey = targetRoleKey;
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.rationale = rationale;
        this.slaDueAt = slaDueAt;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static EscalationRequest createForMissedVisit(
            MissedVisitRecord missedVisitRecord,
            AgencyMembership createdByMembership,
            Branch branch,
            String targetRoleKey,
            VisitExceptionSeverity severity,
            String rationale,
            OffsetDateTime slaDueAt) {
        return EscalationRequest.builder()
                .id(UUID.randomUUID())
                .missedVisitRecord(missedVisitRecord)
                .createdByMembership(createdByMembership)
                .branch(branch)
                .targetRoleKey(targetRoleKey)
                .severity(severity)
                .rationale(rationale)
                .slaDueAt(slaDueAt)
                .status(EscalationStatus.OPEN)
                .build();
    }

    public static EscalationRequest createForException(
            VisitExceptionRecord visitExceptionRecord,
            AgencyMembership createdByMembership,
            Branch branch,
            String targetRoleKey,
            VisitExceptionSeverity severity,
            String rationale,
            OffsetDateTime slaDueAt) {
        return EscalationRequest.builder()
                .id(UUID.randomUUID())
                .visitExceptionRecord(visitExceptionRecord)
                .createdByMembership(createdByMembership)
                .branch(branch)
                .targetRoleKey(targetRoleKey)
                .severity(severity)
                .rationale(rationale)
                .slaDueAt(slaDueAt)
                .status(EscalationStatus.OPEN)
                .build();
    }

    public void acknowledge() {
        this.status = EscalationStatus.ACKNOWLEDGED;
    }

    private void assignSource(MissedVisitRecord missedVisitRecord, VisitExceptionRecord visitExceptionRecord) {
        if ((missedVisitRecord == null) == (visitExceptionRecord == null)) {
            throw new IllegalArgumentException("Exactly one source record must be present for an escalation request");
        }
        if (missedVisitRecord != null) {
            assignAgency(missedVisitRecord.getAgency());
        } else {
            assignAgency(Objects.requireNonNull(visitExceptionRecord, "visitExceptionRecord must not be null").getAgency());
        }
        this.missedVisitRecord = missedVisitRecord;
        this.visitExceptionRecord = visitExceptionRecord;
    }

    private void assignCreatedByMembership(AgencyMembership createdByMembership) {
        this.createdByMembership = Objects.requireNonNull(createdByMembership, "createdByMembership must not be null");
        if (!Objects.equals(createdByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("createdByMembership must belong to the same agency as the escalation request");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the escalation request");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        targetRoleKey = Objects.requireNonNull(targetRoleKey, "targetRoleKey must not be null").trim();
        rationale = Objects.requireNonNull(rationale, "rationale must not be null").trim();
        if (targetRoleKey.isBlank() || rationale.isBlank()) {
            throw new IllegalArgumentException("targetRoleKey and rationale must not be blank");
        }
    }
}
