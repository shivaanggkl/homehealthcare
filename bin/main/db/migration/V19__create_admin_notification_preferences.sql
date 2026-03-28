create table admin_notification_preferences (
    id uuid primary key,
    agency_membership_id uuid not null,
    email_enabled boolean not null,
    failed_login_alerts_enabled boolean not null,
    locked_account_alerts_enabled boolean not null,
    new_admin_alerts_enabled boolean not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_admin_notification_preferences_membership
        foreign key (agency_membership_id) references agency_memberships (id),
    constraint uk_admin_notification_preferences_membership unique (agency_membership_id)
);
