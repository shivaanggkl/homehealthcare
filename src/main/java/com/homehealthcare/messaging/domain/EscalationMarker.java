package com.homehealthcare.messaging.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.messaging.foundation.MessagingEscalationStatus;
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
@Table(name = "escalation_markers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EscalationMarker extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private CommunicationThread thread;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MessagingEscalationStatus status;

    @Column(name = "tag", nullable = false, length = 100)
    private String tag;

    @Column(name = "reason", length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tagged_by_membership_id", nullable = false)
    private AgencyMembership taggedByMembership;

    @Column(name = "tagged_at", nullable = false)
    private OffsetDateTime taggedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleared_by_membership_id")
    private AgencyMembership clearedByMembership;

    @Column(name = "cleared_at")
    private OffsetDateTime clearedAt;

    @Builder
    private EscalationMarker(
            UUID id,
            CommunicationThread thread,
            MessagingEscalationStatus status,
            String tag,
            String reason,
            AgencyMembership taggedByMembership,
            OffsetDateTime taggedAt,
            AgencyMembership clearedByMembership,
            OffsetDateTime clearedAt) {
        this.id = id;
        assignThread(thread);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.tag = tag;
        this.reason = reason;
        assignTaggedByMembership(taggedByMembership);
        this.taggedAt = Objects.requireNonNull(taggedAt, "taggedAt must not be null");
        assignClearedByMembership(clearedByMembership);
        this.clearedAt = clearedAt;
        validateState();
    }

    public static EscalationMarker create(
            CommunicationThread thread,
            MessagingEscalationStatus status,
            String tag,
            String reason,
            AgencyMembership taggedByMembership,
            OffsetDateTime taggedAt) {
        return EscalationMarker.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .status(status)
                .tag(tag)
                .reason(reason)
                .taggedByMembership(taggedByMembership)
                .taggedAt(taggedAt)
                .build();
    }

    public void resolve(AgencyMembership clearedByMembership, OffsetDateTime clearedAt) {
        this.status = MessagingEscalationStatus.RESOLVED;
        assignClearedByMembership(Objects.requireNonNull(clearedByMembership, "clearedByMembership must not be null"));
        this.clearedAt = Objects.requireNonNull(clearedAt, "clearedAt must not be null");
        validateState();
    }

    private void assignThread(CommunicationThread thread) {
        this.thread = Objects.requireNonNull(thread, "thread must not be null");
        assignAgency(thread.getAgency());
    }

    private void assignTaggedByMembership(AgencyMembership taggedByMembership) {
        this.taggedByMembership = Objects.requireNonNull(taggedByMembership, "taggedByMembership must not be null");
        if (!Objects.equals(taggedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("taggedByMembership must belong to the same agency as the escalation marker");
        }
    }

    private void assignClearedByMembership(AgencyMembership clearedByMembership) {
        if (clearedByMembership != null && !Objects.equals(clearedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("clearedByMembership must belong to the same agency as the escalation marker");
        }
        this.clearedByMembership = clearedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        tag = Objects.requireNonNull(tag, "tag must not be null").trim();
        if (tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
        reason = normalizeOptional(reason);
        status = Objects.requireNonNull(status, "status must not be null");
        assignTaggedByMembership(taggedByMembership);
        assignClearedByMembership(clearedByMembership);
        validateState();
    }

    private void validateState() {
        if (clearedAt != null && clearedAt.isBefore(taggedAt)) {
            throw new IllegalArgumentException("clearedAt must not be before taggedAt");
        }
        if (status == MessagingEscalationStatus.RESOLVED && (clearedAt == null || clearedByMembership == null)) {
            throw new IllegalArgumentException("resolved escalation markers must have cleared metadata");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
