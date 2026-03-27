alter table auth_sessions
    add column last_activity_at timestamp with time zone null;

alter table auth_sessions
    add column absolute_expires_at timestamp with time zone null;

update auth_sessions
set last_activity_at = created_at,
    absolute_expires_at = refresh_token_expires_at
where last_activity_at is null
   or absolute_expires_at is null;

alter table auth_sessions
    alter column last_activity_at set not null;

alter table auth_sessions
    alter column absolute_expires_at set not null;

create index idx_auth_sessions_last_activity_at
    on auth_sessions (last_activity_at);

create index idx_auth_sessions_absolute_expires_at
    on auth_sessions (absolute_expires_at);
