create table patient_attachments (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    file_name varchar(255) not null,
    content_type varchar(120) not null,
    size_bytes bigint not null,
    storage_key varchar(255) not null,
    category varchar(80) not null,
    uploader_membership_id uuid not null,
    uploader_email varchar(320) not null,
    status varchar(32) not null,
    description varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_attachments_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_attachments_patient
        foreign key (patient_id) references patients (id),
    constraint fk_patient_attachments_uploader_membership
        foreign key (uploader_membership_id) references agency_memberships (id),
    constraint chk_patient_attachments_status
        check (status in ('ACTIVE', 'ARCHIVED'))
);

create index idx_patient_attachments_agency_id on patient_attachments (agency_id);
create index idx_patient_attachments_patient_id on patient_attachments (patient_id);
