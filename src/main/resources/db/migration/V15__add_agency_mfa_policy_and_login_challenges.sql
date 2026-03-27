alter table agencies
    add column mfa_policy_mode varchar(32) not null default 'OFF';

alter table agencies
    add column mfa_required_roles clob null;

alter table agencies
    add constraint chk_agencies_mfa_policy_mode
        check (mfa_policy_mode in ('OFF', 'ALL_USERS', 'SELECTED_ROLES'));

create table auth_mfa_login_challenges (
    id uuid primary key,
    user_id uuid not null,
    email varchar(320) not null,
    token varchar(128) not null,
    expires_at timestamp with time zone not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    completed_at timestamp with time zone null,
    cancelled_at timestamp with time zone null,
    constraint fk_auth_mfa_login_challenges_user
        foreign key (user_id) references users (id),
    constraint uk_auth_mfa_login_challenges_token unique (token),
    constraint chk_auth_mfa_login_challenges_status
        check (status in ('PENDING', 'COMPLETED', 'CANCELLED', 'EXPIRED'))
);

create index idx_auth_mfa_login_challenges_user_status
    on auth_mfa_login_challenges (user_id, status);
