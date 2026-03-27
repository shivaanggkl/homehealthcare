create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null,
    email varchar(320) not null,
    token varchar(128) not null,
    status varchar(32) not null,
    expires_at timestamp with time zone not null,
    requested_at timestamp with time zone not null,
    cancelled_at timestamp with time zone null,
    consumed_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_password_reset_tokens_user
        foreign key (user_id) references users (id),
    constraint uk_password_reset_tokens_token unique (token),
    constraint chk_password_reset_tokens_status
        check (status in ('PENDING', 'CONSUMED', 'CANCELLED', 'EXPIRED'))
);

create index idx_password_reset_tokens_user_id
    on password_reset_tokens (user_id);

create index idx_password_reset_tokens_email_status
    on password_reset_tokens (email, status);
