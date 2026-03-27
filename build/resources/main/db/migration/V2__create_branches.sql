create table branches (
    id uuid primary key,
    agency_id uuid not null,
    name varchar(200) not null,
    code varchar(50) not null,
    address varchar(500) not null,
    timezone varchar(64) not null,
    status varchar(32) not null,
    deactivated_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_branches_agency
        foreign key (agency_id) references agencies (id),
    constraint uk_branches_agency_name unique (agency_id, name),
    constraint uk_branches_agency_code unique (agency_id, code),
    constraint chk_branches_status
        check (status in ('ACTIVE', 'INACTIVE'))
);
