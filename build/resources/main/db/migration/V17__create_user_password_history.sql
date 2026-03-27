create table user_password_history (
    id uuid primary key,
    user_id uuid not null,
    password_hash varchar(255) not null,
    recorded_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_user_password_history_user foreign key (user_id) references users (id)
);

create index idx_user_password_history_user_recorded_at
    on user_password_history (user_id, recorded_at desc);
