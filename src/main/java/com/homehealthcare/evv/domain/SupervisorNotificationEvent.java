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
@Table(name = "supervisor_notification_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupervisorNotificationEvent extends AgencyScopedEntity {

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
    @JoinColumn(name = "recipient_membership_id", nullable = false)
    private AgencyMembership recipientMembership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_membership_id", nullable = false)
    private AgencyMembership createdByMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "rationale", nullable = false, length = 1000)
    private String rationale;

    @Column(name = "created_at_event", nullable = false)
    private OffsetDateTime createdAtEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SupervisorNotificationStatus status;

    @Builder
    private SupervisorNotificationEvent(
            UUID id,
            MissedVisitRecord missedVisitRecord,
            VisitExceptionRecord visitExceptionRecord,
            AgencyMembership recipientMembership,
            AgencyMembership createdByMembership,
            Branch branch,
            String channel,
            String rationale,
            OffsetDateTime createdAtEvent,
            SupervisorNotificationStatus status) {
        this.id = id;
        assignSource(missedVisitRecord, visitExceptionRecord);
        assignRecipientMembership(recipientMembership);
        assignCreatedByMembership(createdByMembership);
        assignBranch(branch);
        this.channel = channel;
        this.rationale = rationale;
        this.createdAtEvent = Objects.requireNonNull(createdAtEvent, "createdAtEvent must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static SupervisorNotificationEvent createForMissedVisit(
            MissedVisitRecord missedVisitRecord,
            AgencyMembership recipientMembership,
            AgencyMembership createdByMembership,
            Branch branch,
            String channel,
            String rationale,
            OffsetDateTime createdAtEvent) {
        return SupervisorNotificationEvent.builder()
                .id(UUID.randomUUID())
                .missedVisitRecord(missedVisitRecord)
                .recipientMembership(recipientMembership)
                .createdByMembership(createdByMembership)
                .branch(branch)
                .channel(channel)
                .rationale(rationale)
                .createdAtEvent(createdAtEvent)
                .status(SupervisorNotificationStatus.QUEUED)
                .build();
    }

    public static SupervisorNotificationEvent createForException(
            VisitExceptionRecord visitExceptionRecord,
            AgencyMembership recipientMembership,
            AgencyMembership createdByMembership,
            Branch branch,
            String channel,
            String rationale,
            OffsetDateTime createdAtEvent) {
        return SupervisorNotificationEvent.builder()
                .id(UUID.randomUUID())
                .visitExceptionRecord(visitExceptionRecord)
                .recipientMembership(recipientMembership)
                .createdByMembership(createdByMembership)
                .branch(branch)
                .channel(channel)
                .rationale(rationale)
                .createdAtEvent(createdAtEvent)
                .status(SupervisorNotificationStatus.QUEUED)
                .build();
    }

    public void markSent() {
        this.status = SupervisorNotificationStatus.SENT;
    }

    private void assignSource(MissedVisitRecord missedVisitRecord, VisitExceptionRecord visitExceptionRecord) {
        if ((missedVisitRecord == null) == (visitExceptionRecord == null)) {
            throw new IllegalArgumentException("Exactly one source record must be present for a supervisor notification event");
        }
        if (missedVisitRecord != null) {
            assignAgency(missedVisitRecord.getAgency());
        } else {
            assignAgency(Objects.requireNonNull(visitExceptionRecord, "visitExceptionRecord must not be null").getAgency());
        }
        this.missedVisitRecord = missedVisitRecord;
        this.visitExceptionRecord = visitExceptionRecord;
    }

    private void assignRecipientMembership(AgencyMembership recipientMembership) {
        this.recipientMembership = Objects.requireNonNull(recipientMembership, "recipientMembership must not be null");
        if (!Objects.equals(recipientMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("recipientMembership must belong to the same agency as the notification event");
        }
    }

    private void assignCreatedByMembership(AgencyMembership createdByMembership) {
        this.createdByMembership = Objects.requireNonNull(createdByMembership, "createdByMembership must not be null");
        if (!Objects.equals(createdByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("createdByMembership must belong to the same agency as the notification event");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the notification event");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        channel = Objects.requireNonNull(channel, "channel must not be null").trim();
        rationale = Objects.requireNonNull(rationale, "rationale must not be null").trim();
        if (channel.isBlank() || rationale.isBlank()) {
            throw new IllegalArgumentException("channel and rationale must not be blank");
        }
    }
}
