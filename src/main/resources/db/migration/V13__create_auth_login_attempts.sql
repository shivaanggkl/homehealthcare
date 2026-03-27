create table auth_login_attempts (
    id uuid primary key,
    user_id uuid null,
    email varchar(320) not null,
    ip_address varchar(64) null,
    outcome varchar(32) not null,
    failure_reason varchar(64) null,
    attempted_at timestamp with time zone not null,
    constraint fk_auth_login_attempts_user
        foreign key (user_id) references users (id),
    constraint chk_auth_login_attempts_outcome
        check (outcome in ('SUCCESS', 'FAILURE', 'LOCKED_OUT', 'RATE_LIMITED'))
);

create index idx_auth_login_attempts_email_attempted_at
    on auth_login_attempts (email, attempted_at);

create index idx_auth_login_attempts_ip_attempted_at
    on auth_login_attempts (ip_address, attempted_at);

create index idx_auth_login_attempts_user_attempted_at
    on auth_login_attempts (user_id, attempted_at);
