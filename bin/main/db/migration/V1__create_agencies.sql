create table agencies (
    id uuid primary key,
    name varchar(200) not null,
    slug varchar(120) not null,
    status varchar(32) not null,
    timezone varchar(64) not null,
    contact_email varchar(320) not null,
    deactivated_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_agencies_slug unique (slug),
    constraint chk_agencies_status
        check (status in ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);
