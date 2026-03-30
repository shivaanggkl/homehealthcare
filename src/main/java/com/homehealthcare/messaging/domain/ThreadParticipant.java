package com.homehealthcare.messaging.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "thread_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThreadParticipant extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private CommunicationThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private AgencyMembership membership;

    @Column(name = "participant_role", length = 100)
    private String participantRole;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "muted", nullable = false)
    private boolean muted;

    @Builder
    private ThreadParticipant(
            UUID id,
            CommunicationThread thread,
            AgencyMembership membership,
            String participantRole,
            OffsetDateTime addedAt,
            OffsetDateTime removedAt,
            boolean muted) {
        this.id = id;
        assignThread(thread);
        assignMembership(membership);
        this.participantRole = participantRole;
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt must not be null");
        this.removedAt = removedAt;
        this.muted = muted;
        validateState();
    }

    public static ThreadParticipant add(
            CommunicationThread thread,
            AgencyMembership membership,
            String participantRole,
            OffsetDateTime addedAt) {
        return ThreadParticipant.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .membership(membership)
                .participantRole(participantRole)
                .addedAt(addedAt)
                .muted(false)
                .build();
    }

    public void remove(OffsetDateTime removedAt) {
        this.removedAt = Objects.requireNonNull(removedAt, "removedAt must not be null");
        validateState();
    }

    public void mute() {
        this.muted = true;
    }

    public void unmute() {
        this.muted = false;
    }

    public boolean isActive() {
        return removedAt == null;
    }

    public UUID getThreadId() {
        return thread == null ? null : thread.getId();
    }

    public UUID getMembershipId() {
        return membership == null ? null : membership.getId();
    }

    private void assignThread(CommunicationThread thread) {
        this.thread = Objects.requireNonNull(thread, "thread must not be null");
        assignAgency(thread.getAgency());
    }

    private void assignMembership(AgencyMembership membership) {
        this.membership = Objects.requireNonNull(membership, "membership must not be null");
        if (!Objects.equals(membership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("membership must belong to the same agency as the thread participant");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        participantRole = normalizeOptional(participantRole);
        assignMembership(membership);
        validateState();
    }

    private void validateState() {
        if (removedAt != null && removedAt.isBefore(addedAt)) {
            throw new IllegalArgumentException("removedAt must not be before addedAt");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
