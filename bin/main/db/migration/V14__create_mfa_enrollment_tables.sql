alter table users
    add column mfa_secret varchar(64) null;

alter table users
    add column mfa_enrolled_at timestamp with time zone null;

create table mfa_enrollment_challenges (
    id uuid primary key,
    user_id uuid not null,
    auth_session_id uuid not null,
    token varchar(128) not null,
    totp_secret varchar(64) not null,
    recovery_code_hashes clob not null,
    status varchar(32) not null,
    reauthenticated_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    completed_at timestamp with time zone null,
    cancelled_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mfa_enrollment_challenges_user
        foreign key (user_id) references users (id),
    constraint fk_mfa_enrollment_challenges_auth_session
        foreign key (auth_session_id) references auth_sessions (id),
    constraint uk_mfa_enrollment_challenges_token unique (token),
    constraint chk_mfa_enrollment_challenges_status
        check (status in ('PENDING', 'COMPLETED', 'CANCELLED', 'EXPIRED'))
);

create index idx_mfa_enrollment_challenges_user_status
    on mfa_enrollment_challenges (user_id, status);

create table user_mfa_recovery_codes (
    id uuid primary key,
    user_id uuid not null,
    code_hash varchar(128) not null,
    ordinal int not null,
    consumed_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_user_mfa_recovery_codes_user
        foreign key (user_id) references users (id),
    constraint uk_user_mfa_recovery_codes_hash unique (code_hash)
);

create index idx_user_mfa_recovery_codes_user
    on user_mfa_recovery_codes (user_id);
