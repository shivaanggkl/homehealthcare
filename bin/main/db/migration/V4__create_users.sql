create table users (
    id uuid primary key,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(320) not null,
    phone varchar(30) null,
    status varchar(32) not null,
    last_login_at timestamp with time zone null,
    deactivated_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_users_email unique (email),
    constraint chk_users_status
        check (status in ('INVITED', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'DEACTIVATED'))
);
