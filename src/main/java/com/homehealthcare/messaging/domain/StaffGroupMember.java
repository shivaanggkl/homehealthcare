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
@Table(name = "staff_group_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffGroupMember extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_group_id", nullable = false)
    private StaffGroup staffGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private AgencyMembership membership;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Builder
    private StaffGroupMember(
            UUID id,
            StaffGroup staffGroup,
            AgencyMembership membership,
            OffsetDateTime addedAt,
            OffsetDateTime removedAt) {
        this.id = id;
        assignStaffGroup(staffGroup);
        assignMembership(membership);
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt must not be null");
        this.removedAt = removedAt;
        validateState();
    }

    public static StaffGroupMember add(StaffGroup staffGroup, AgencyMembership membership, OffsetDateTime addedAt) {
        return StaffGroupMember.builder()
                .id(UUID.randomUUID())
                .staffGroup(staffGroup)
                .membership(membership)
                .addedAt(addedAt)
                .build();
    }

    public void remove(OffsetDateTime removedAt) {
        this.removedAt = Objects.requireNonNull(removedAt, "removedAt must not be null");
        validateState();
    }

    public boolean isActive() {
        return removedAt == null;
    }

    public UUID getMembershipId() {
        return membership == null ? null : membership.getId();
    }

    private void assignStaffGroup(StaffGroup staffGroup) {
        this.staffGroup = Objects.requireNonNull(staffGroup, "staffGroup must not be null");
        assignAgency(staffGroup.getAgency());
    }

    private void assignMembership(AgencyMembership membership) {
        this.membership = Objects.requireNonNull(membership, "membership must not be null");
        if (!Objects.equals(membership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("membership must belong to the same agency as the staff group member");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignMembership(membership);
        validateState();
    }

    private void validateState() {
        if (removedAt != null && removedAt.isBefore(addedAt)) {
            throw new IllegalArgumentException("removedAt must not be before addedAt");
        }
    }
}
