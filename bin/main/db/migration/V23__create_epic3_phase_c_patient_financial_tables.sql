create table patient_payer_links (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    payer_name varchar(200),
    payer_external_id varchar(100),
    member_policy_number varchar(120),
    group_number varchar(120),
    effective_from date not null,
    effective_to date,
    primary_payer boolean not null,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_payer_links_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_payer_links_patient
        foreign key (patient_id) references patients (id),
    constraint chk_patient_payer_link_status
        check (status in ('ACTIVE', 'PENDING', 'INACTIVE', 'TERMINATED', 'EXPIRED')),
    constraint chk_patient_payer_links_identity
        check (payer_name is not null or payer_external_id is not null)
);

create index idx_patient_payer_links_agency_id on patient_payer_links (agency_id);
create index idx_patient_payer_links_patient_id on patient_payer_links (patient_id);

create table patient_episode_authorizations (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    patient_payer_link_id uuid,
    service_line_id uuid,
    authorization_number varchar(120),
    start_date date not null,
    end_date date not null,
    authorized_units integer,
    used_units integer,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_episode_authorizations_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_episode_authorizations_patient
        foreign key (patient_id) references patients (id),
    constraint fk_patient_episode_authorizations_payer_link
        foreign key (patient_payer_link_id) references patient_payer_links (id),
    constraint fk_patient_episode_authorizations_service_line
        foreign key (service_line_id) references service_lines (id),
    constraint chk_patient_episode_authorization_status
        check (status in ('PENDING', 'ACTIVE', 'EXPIRED', 'EXHAUSTED', 'CANCELLED')),
    constraint chk_patient_episode_authorizations_units
        check (authorized_units is null or authorized_units >= 0),
    constraint chk_patient_episode_authorizations_used_units
        check (used_units is null or used_units >= 0)
);

create index idx_patient_episode_authorizations_agency_id on patient_episode_authorizations (agency_id);
create index idx_patient_episode_authorizations_patient_id on patient_episode_authorizations (patient_id);
