package com.homehealthcare.platform.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "audit_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_type", nullable = false, length = 64)
    private String actorType;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_email", nullable = false, length = 320)
    private String actorEmail;

    @Column(name = "action_type", nullable = false, length = 128)
    private String actionType;

    @Column(name = "target_type", nullable = false, length = 128)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "agency_id")
    private UUID agencyId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private AuditEventOutcome outcome;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "clob")
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Builder
    private AuditEvent(
            UUID id,
            String actorType,
            UUID actorId,
            String actorEmail,
            String actionType,
            String targetType,
            UUID targetId,
            UUID agencyId,
            UUID branchId,
            AuditEventOutcome outcome,
            String metadataJson,
            Instant occurredAt) {
        this.id = id;
        this.actorType = actorType;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.agencyId = agencyId;
        this.branchId = branchId;
        this.outcome = outcome;
        this.metadataJson = metadataJson;
        this.occurredAt = occurredAt;
    }

    public static AuditEvent create(
            String actorType,
            UUID actorId,
            String actorEmail,
            String actionType,
            String targetType,
            UUID targetId,
            UUID agencyId,
            String metadataJson) {
        return create(
                actorType,
                actorId,
                actorEmail,
                actionType,
                targetType,
                targetId,
                agencyId,
                null,
                AuditEventOutcome.SUCCESS,
                metadataJson);
    }

    public static AuditEvent createSuccess(
            String actorType,
            UUID actorId,
            String actorEmail,
            String actionType,
            String targetType,
            UUID targetId,
            UUID agencyId,
            UUID branchId,
            String metadataJson) {
        return create(
                actorType,
                actorId,
                actorEmail,
                actionType,
                targetType,
                targetId,
                agencyId,
                branchId,
                AuditEventOutcome.SUCCESS,
                metadataJson);
    }

    public static AuditEvent createFailure(
            String actorType,
            UUID actorId,
            String actorEmail,
            String actionType,
            String targetType,
            UUID targetId,
            UUID agencyId,
            UUID branchId,
            String metadataJson) {
        return create(
                actorType,
                actorId,
                actorEmail,
                actionType,
                targetType,
                targetId,
                agencyId,
                branchId,
                AuditEventOutcome.FAILURE,
                metadataJson);
    }

    private static AuditEvent create(
            String actorType,
            UUID actorId,
            String actorEmail,
            String actionType,
            String targetType,
            UUID targetId,
            UUID agencyId,
            UUID branchId,
            AuditEventOutcome outcome,
            String metadataJson) {
        return AuditEvent.builder()
                .id(UUID.randomUUID())
                .actorType(actorType)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .agencyId(agencyId)
                .branchId(branchId)
                .outcome(outcome)
                .metadataJson(metadataJson)
                .occurredAt(Instant.now())
                .build();
    }

    @PrePersist
    void initialize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (outcome == null) {
            outcome = AuditEventOutcome.SUCCESS;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditEvent auditEvent)) {
            return false;
        }
        return id != null && Objects.equals(id, auditEvent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
