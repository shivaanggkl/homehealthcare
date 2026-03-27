create table auth_sessions (
    id uuid primary key,
    user_id uuid not null,
    access_token_hash varchar(128) not null,
    access_token_expires_at timestamp with time zone not null,
    refresh_token_hash varchar(128) not null,
    refresh_token_expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone null,
    revocation_reason varchar(64) null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_auth_sessions_user
        foreign key (user_id) references users (id)
);

create unique index uk_auth_sessions_access_token_hash
    on auth_sessions (access_token_hash);

create unique index uk_auth_sessions_refresh_token_hash
    on auth_sessions (refresh_token_hash);

create index idx_auth_sessions_user_id
    on auth_sessions (user_id);
