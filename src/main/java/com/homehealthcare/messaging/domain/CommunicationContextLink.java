package com.homehealthcare.messaging.domain;

import com.homehealthcare.messaging.foundation.CoordinationContextType;
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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "communication_context_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunicationContextLink extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private CommunicationThread thread;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 32)
    private CoordinationContextType contextType;

    @Column(name = "context_id", nullable = false)
    private UUID contextId;

    @Builder
    private CommunicationContextLink(
            UUID id,
            CommunicationThread thread,
            CoordinationContextType contextType,
            UUID contextId) {
        this.id = id;
        assignThread(thread);
        this.contextType = Objects.requireNonNull(contextType, "contextType must not be null");
        this.contextId = Objects.requireNonNull(contextId, "contextId must not be null");
    }

    public static CommunicationContextLink create(
            CommunicationThread thread,
            CoordinationContextType contextType,
            UUID contextId) {
        return CommunicationContextLink.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .contextType(contextType)
                .contextId(contextId)
                .build();
    }

    private void assignThread(CommunicationThread thread) {
        this.thread = Objects.requireNonNull(thread, "thread must not be null");
        assignAgency(thread.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        contextType = Objects.requireNonNull(contextType, "contextType must not be null");
        contextId = Objects.requireNonNull(contextId, "contextId must not be null");
    }
}
