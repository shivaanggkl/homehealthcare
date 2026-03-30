package com.homehealthcare.messaging.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.messaging.foundation.MessagingDeliveryState;
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
@Table(name = "message_read_receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageReadReceipt extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private CommunicationMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_membership_id", nullable = false)
    private AgencyMembership recipientMembership;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_state", nullable = false, length = 32)
    private MessagingDeliveryState deliveryState;

    @Builder
    private MessageReadReceipt(
            UUID id,
            CommunicationMessage message,
            AgencyMembership recipientMembership,
            OffsetDateTime readAt,
            MessagingDeliveryState deliveryState) {
        this.id = id;
        assignMessage(message);
        assignRecipientMembership(recipientMembership);
        this.readAt = readAt;
        this.deliveryState = Objects.requireNonNull(deliveryState, "deliveryState must not be null");
        validateState();
    }

    public static MessageReadReceipt createPending(
            CommunicationMessage message,
            AgencyMembership recipientMembership) {
        return MessageReadReceipt.builder()
                .id(UUID.randomUUID())
                .message(message)
                .recipientMembership(recipientMembership)
                .deliveryState(MessagingDeliveryState.PENDING)
                .build();
    }

    public void markDelivered() {
        if (deliveryState == MessagingDeliveryState.PENDING) {
            deliveryState = MessagingDeliveryState.DELIVERED;
        }
    }

    public void markRead(OffsetDateTime readAt) {
        this.readAt = Objects.requireNonNull(readAt, "readAt must not be null");
        this.deliveryState = MessagingDeliveryState.READ;
        validateState();
    }

    public UUID getMessageId() {
        return message == null ? null : message.getId();
    }

    private void assignMessage(CommunicationMessage message) {
        this.message = Objects.requireNonNull(message, "message must not be null");
        assignAgency(message.getAgency());
    }

    private void assignRecipientMembership(AgencyMembership recipientMembership) {
        this.recipientMembership = Objects.requireNonNull(recipientMembership, "recipientMembership must not be null");
        if (!Objects.equals(recipientMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("recipientMembership must belong to the same agency as the read receipt");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        deliveryState = Objects.requireNonNull(deliveryState, "deliveryState must not be null");
        assignRecipientMembership(recipientMembership);
        validateState();
    }

    private void validateState() {
        if (deliveryState == MessagingDeliveryState.READ && readAt == null) {
            throw new IllegalArgumentException("readAt must be present when deliveryState is READ");
        }
    }
}
