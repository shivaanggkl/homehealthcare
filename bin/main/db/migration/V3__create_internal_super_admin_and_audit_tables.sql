create table internal_super_admins (
    id uuid primary key,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(320) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_internal_super_admins_email unique (email),
    constraint chk_internal_super_admins_status
        check (status in ('ACTIVE', 'INACTIVE'))
);

create table agency_owner_bootstraps (
    id uuid primary key,
    agency_id uuid not null,
    owner_first_name varchar(100) not null,
    owner_last_name varchar(100) not null,
    owner_email varchar(320) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_agency_owner_bootstraps_agency
        foreign key (agency_id) references agencies (id),
    constraint chk_agency_owner_bootstraps_status
        check (status in ('PENDING', 'COMPLETED', 'CANCELLED'))
);

create index idx_agency_owner_bootstraps_agency_id
    on agency_owner_bootstraps (agency_id);

create table audit_events (
    id uuid primary key,
    actor_type varchar(64) not null,
    actor_id uuid not null,
    actor_email varchar(320) not null,
    action_type varchar(128) not null,
    target_type varchar(128) not null,
    target_id uuid not null,
    agency_id uuid null,
    metadata_json clob not null,
    occurred_at timestamp with time zone not null,
    constraint fk_audit_events_agency
        foreign key (agency_id) references agencies (id)
);

create index idx_audit_events_actor_id
    on audit_events (actor_id);

create index idx_audit_events_agency_id
    on audit_events (agency_id);
