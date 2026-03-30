package com.homehealthcare.messaging.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.messaging.foundation.CommunicationMessageType;
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
@Table(name = "communication_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunicationMessage extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private CommunicationThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_membership_id", nullable = false)
    private AgencyMembership senderMembership;

    @Column(name = "message_body", nullable = false, length = 4000)
    private String messageBody;

    @Column(name = "created_at_at_source", nullable = false)
    private OffsetDateTime createdAtAtSource;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    private CommunicationMessageType messageType;

    @Column(name = "attachment_reference", length = 255)
    private String attachmentReference;

    @Builder
    private CommunicationMessage(
            UUID id,
            CommunicationThread thread,
            AgencyMembership senderMembership,
            String messageBody,
            OffsetDateTime createdAtAtSource,
            OffsetDateTime editedAt,
            CommunicationMessageType messageType,
            String attachmentReference) {
        this.id = id;
        assignThread(thread);
        assignSenderMembership(senderMembership);
        this.messageBody = messageBody;
        this.createdAtAtSource = Objects.requireNonNull(createdAtAtSource, "createdAtAtSource must not be null");
        this.editedAt = editedAt;
        this.messageType = Objects.requireNonNull(messageType, "messageType must not be null");
        this.attachmentReference = attachmentReference;
        validateState();
    }

    public static CommunicationMessage create(
            CommunicationThread thread,
            AgencyMembership senderMembership,
            String messageBody,
            OffsetDateTime createdAtAtSource,
            CommunicationMessageType messageType,
            String attachmentReference) {
        return CommunicationMessage.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .senderMembership(senderMembership)
                .messageBody(messageBody)
                .createdAtAtSource(createdAtAtSource)
                .messageType(messageType)
                .attachmentReference(attachmentReference)
                .build();
    }

    public void edit(String messageBody, OffsetDateTime editedAt) {
        this.messageBody = messageBody;
        this.editedAt = Objects.requireNonNull(editedAt, "editedAt must not be null");
        validateState();
    }

    public UUID getThreadId() {
        return thread == null ? null : thread.getId();
    }

    private void assignThread(CommunicationThread thread) {
        this.thread = Objects.requireNonNull(thread, "thread must not be null");
        assignAgency(thread.getAgency());
    }

    private void assignSenderMembership(AgencyMembership senderMembership) {
        this.senderMembership = Objects.requireNonNull(senderMembership, "senderMembership must not be null");
        if (!Objects.equals(senderMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("senderMembership must belong to the same agency as the communication message");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        messageBody = Objects.requireNonNull(messageBody, "messageBody must not be null").trim();
        attachmentReference = normalizeOptional(attachmentReference);
        messageType = Objects.requireNonNull(messageType, "messageType must not be null");
        assignSenderMembership(senderMembership);
        validateState();
    }

    private void validateState() {
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("messageBody must not be blank");
        }
        if (editedAt != null && editedAt.isBefore(createdAtAtSource)) {
            throw new IllegalArgumentException("editedAt must not be before createdAtAtSource");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
