package com.homehealthcare.mobile.domain;

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
@Table(name = "mobile_message_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileMessageEntry extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private MobileMessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_membership_id", nullable = false)
    private AgencyMembership senderMembership;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @Column(name = "message_text", nullable = false, length = 4000)
    private String messageText;

    @Builder
    private MobileMessageEntry(
            UUID id,
            MobileMessageThread thread,
            AgencyMembership senderMembership,
            OffsetDateTime sentAt,
            String messageText) {
        this.id = id;
        assignThread(thread);
        assignSenderMembership(senderMembership);
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
        this.messageText = messageText;
    }

    public static MobileMessageEntry create(
            MobileMessageThread thread,
            AgencyMembership senderMembership,
            OffsetDateTime sentAt,
            String messageText) {
        return MobileMessageEntry.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .senderMembership(senderMembership)
                .sentAt(sentAt)
                .messageText(messageText)
                .build();
    }

    private void assignThread(MobileMessageThread thread) {
        this.thread = Objects.requireNonNull(thread, "thread must not be null");
        assignAgency(thread.getAgency());
    }

    private void assignSenderMembership(AgencyMembership senderMembership) {
        this.senderMembership = Objects.requireNonNull(senderMembership, "senderMembership must not be null");
        if (!Objects.equals(senderMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("senderMembership must belong to the same agency as the message");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        messageText = Objects.requireNonNull(messageText, "messageText must not be null").trim();
        assignSenderMembership(senderMembership);
    }
}
