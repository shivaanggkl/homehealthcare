package com.homehealthcare.messaging.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.messaging.foundation.BranchBroadcastStatus;
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
@Table(name = "branch_broadcasts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchBroadcast extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "eligible_roles_csv", length = 500)
    private String eligibleRolesCsv;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_membership_id", nullable = false)
    private AgencyMembership createdByMembership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private CommunicationThread thread;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BranchBroadcastStatus status;

    @Builder
    private BranchBroadcast(
            UUID id,
            Branch branch,
            String eligibleRolesCsv,
            String subject,
            String body,
            AgencyMembership createdByMembership,
            CommunicationThread thread,
            OffsetDateTime expiresAt,
            BranchBroadcastStatus status) {
        this.id = id;
        assignBranch(branch);
        this.eligibleRolesCsv = eligibleRolesCsv;
        this.subject = subject;
        this.body = body;
        assignCreatedByMembership(createdByMembership);
        assignThread(thread);
        this.expiresAt = expiresAt;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static BranchBroadcast create(
            Branch branch,
            String eligibleRolesCsv,
            String subject,
            String body,
            AgencyMembership createdByMembership,
            CommunicationThread thread,
            OffsetDateTime expiresAt) {
        return BranchBroadcast.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .eligibleRolesCsv(eligibleRolesCsv)
                .subject(subject)
                .body(body)
                .createdByMembership(createdByMembership)
                .thread(thread)
                .expiresAt(expiresAt)
                .status(BranchBroadcastStatus.SENT)
                .build();
    }

    public void cancel() {
        this.status = BranchBroadcastStatus.CANCELLED;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getThreadId() {
        return thread == null ? null : thread.getId();
    }

    private void assignBranch(Branch branch) {
        this.branch = Objects.requireNonNull(branch, "branch must not be null");
        assignAgency(branch.getAgency());
    }

    private void assignCreatedByMembership(AgencyMembership createdByMembership) {
        this.createdByMembership = Objects.requireNonNull(createdByMembership, "createdByMembership must not be null");
        if (!Objects.equals(createdByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("createdByMembership must belong to the same agency as the branch broadcast");
        }
    }

    private void assignThread(CommunicationThread thread) {
        this.thread = Objects.requireNonNull(thread, "thread must not be null");
        if (!Objects.equals(thread.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("thread must belong to the same agency as the branch broadcast");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        eligibleRolesCsv = normalizeOptional(eligibleRolesCsv);
        subject = Objects.requireNonNull(subject, "subject must not be null").trim();
        body = Objects.requireNonNull(body, "body must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        assignCreatedByMembership(createdByMembership);
        assignThread(thread);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
