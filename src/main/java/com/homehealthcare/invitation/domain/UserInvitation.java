package com.homehealthcare.invitation.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.user.domain.User;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInvitation extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_membership_id", nullable = false)
    private AgencyMembership invitedByMembership;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_membership_id", nullable = false)
    private AgencyMembership agencyMembership;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @NotBlank
    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserInvitationStatus status;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Builder
    private UserInvitation(
            UUID id,
            AgencyMembership invitedByMembership,
            AgencyMembership agencyMembership,
            User user,
            String email,
            String token,
            UserInvitationStatus status,
            OffsetDateTime expiresAt,
            OffsetDateTime cancelledAt,
            OffsetDateTime acceptedAt) {
        this.id = id;
        this.invitedByMembership = invitedByMembership;
        this.agencyMembership = agencyMembership;
        this.user = user;
        assignAgency(agencyMembership == null ? null : agencyMembership.getAgency());
        this.email = email;
        this.token = token;
        this.status = status;
        this.expiresAt = expiresAt;
        this.cancelledAt = cancelledAt;
        this.acceptedAt = acceptedAt;
    }

    public static UserInvitation issue(
            AgencyMembership invitedByMembership,
            AgencyMembership agencyMembership,
            User user,
            String email,
            String token,
            OffsetDateTime expiresAt) {
        return UserInvitation.builder()
                .id(UUID.randomUUID())
                .invitedByMembership(invitedByMembership)
                .agencyMembership(agencyMembership)
                .user(user)
                .email(email)
                .token(token)
                .status(UserInvitationStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    public void cancel() {
        this.status = UserInvitationStatus.CANCELLED;
        this.cancelledAt = OffsetDateTime.now();
    }

    public void accept() {
        this.status = UserInvitationStatus.ACCEPTED;
        this.acceptedAt = OffsetDateTime.now();
    }

    public void expire() {
        this.status = UserInvitationStatus.EXPIRED;
    }

    public void rescheduleExpiration(OffsetDateTime expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public boolean isPending() {
        return status == UserInvitationStatus.PENDING;
    }

    public boolean isExpiredAt(OffsetDateTime timestamp) {
        return expiresAt.isBefore(timestamp) || expiresAt.isEqual(timestamp);
    }

    public UUID getAgencyMembershipId() {
        return agencyMembership == null ? null : agencyMembership.getId();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        token = token == null ? null : token.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserInvitation invitation)) {
            return false;
        }
        return id != null && Objects.equals(id, invitation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
