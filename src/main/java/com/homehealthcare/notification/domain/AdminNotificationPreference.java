package com.homehealthcare.notification.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_notification_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminNotificationPreference extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_membership_id", nullable = false, unique = true)
    private AgencyMembership agencyMembership;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "failed_login_alerts_enabled", nullable = false)
    private boolean failedLoginAlertsEnabled;

    @Column(name = "locked_account_alerts_enabled", nullable = false)
    private boolean lockedAccountAlertsEnabled;

    @Column(name = "new_admin_alerts_enabled", nullable = false)
    private boolean newAdminAlertsEnabled;

    @Builder
    private AdminNotificationPreference(
            UUID id,
            AgencyMembership agencyMembership,
            boolean emailEnabled,
            boolean failedLoginAlertsEnabled,
            boolean lockedAccountAlertsEnabled,
            boolean newAdminAlertsEnabled) {
        this.id = id;
        this.agencyMembership = agencyMembership;
        this.emailEnabled = emailEnabled;
        this.failedLoginAlertsEnabled = failedLoginAlertsEnabled;
        this.lockedAccountAlertsEnabled = lockedAccountAlertsEnabled;
        this.newAdminAlertsEnabled = newAdminAlertsEnabled;
    }

    public static AdminNotificationPreference defaultsFor(AgencyMembership agencyMembership) {
        return AdminNotificationPreference.builder()
                .id(UUID.randomUUID())
                .agencyMembership(Objects.requireNonNull(agencyMembership, "agencyMembership must not be null"))
                .emailEnabled(true)
                .failedLoginAlertsEnabled(true)
                .lockedAccountAlertsEnabled(true)
                .newAdminAlertsEnabled(true)
                .build();
    }

    public void update(
            boolean emailEnabled,
            boolean failedLoginAlertsEnabled,
            boolean lockedAccountAlertsEnabled,
            boolean newAdminAlertsEnabled) {
        this.emailEnabled = emailEnabled;
        this.failedLoginAlertsEnabled = failedLoginAlertsEnabled;
        this.lockedAccountAlertsEnabled = lockedAccountAlertsEnabled;
        this.newAdminAlertsEnabled = newAdminAlertsEnabled;
    }

    @PrePersist
    void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
