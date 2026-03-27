create table user_invitations (
    id uuid primary key,
    agency_id uuid not null,
    invited_by_membership_id uuid not null,
    agency_membership_id uuid not null,
    user_id uuid not null,
    email varchar(320) not null,
    token varchar(128) not null,
    status varchar(32) not null,
    expires_at timestamp with time zone not null,
    cancelled_at timestamp with time zone null,
    accepted_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_user_invitations_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_user_invitations_invited_by_membership
        foreign key (invited_by_membership_id) references agency_memberships (id),
    constraint fk_user_invitations_agency_membership
        foreign key (agency_membership_id) references agency_memberships (id),
    constraint fk_user_invitations_user
        foreign key (user_id) references users (id),
    constraint uk_user_invitations_token unique (token),
    constraint chk_user_invitations_status
        check (status in ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED'))
);

create index idx_user_invitations_agency_id
    on user_invitations (agency_id);

create index idx_user_invitations_agency_email
    on user_invitations (agency_id, email);
