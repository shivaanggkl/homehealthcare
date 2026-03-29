create table patients (
    id uuid primary key,
    agency_id uuid not null,
    external_reference varchar(100),
    first_name varchar(100) not null,
    middle_name varchar(100),
    last_name varchar(100) not null,
    preferred_name varchar(100),
    date_of_birth date not null,
    sex_marker varchar(50),
    primary_phone varchar(30),
    secondary_phone varchar(30),
    email varchar(320),
    language varchar(35),
    notes_summary varchar(1000),
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patients_agency
        foreign key (agency_id) references agencies (id),
    constraint chk_patients_status
        check (status in ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'DUPLICATE_CANDIDATE', 'DUPLICATE_MERGED'))
);

create index idx_patients_agency_id on patients (agency_id);
create index idx_patients_name on patients (agency_id, last_name, first_name);

create table patient_contacts (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    relationship_type varchar(100),
    full_name varchar(200) not null,
    phone varchar(30),
    email varchar(320),
    address varchar(500),
    emergency_contact boolean not null,
    primary_contact boolean not null,
    responsible_party boolean not null,
    notes varchar(1000),
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_contacts_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_contacts_patient
        foreign key (patient_id) references patients (id),
    constraint chk_patient_contacts_status
        check (status in ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'DUPLICATE_CANDIDATE', 'DUPLICATE_MERGED'))
);

create index idx_patient_contacts_agency_id on patient_contacts (agency_id);
create index idx_patient_contacts_patient_id on patient_contacts (patient_id);

create table patient_addresses (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    address_line_1 varchar(250) not null,
    address_line_2 varchar(250),
    city varchar(120) not null,
    state varchar(80) not null,
    postal_code varchar(20) not null,
    country varchar(80),
    latitude numeric(9,6),
    longitude numeric(9,6),
    geocode_status varchar(40),
    timezone varchar(64),
    location_notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_addresses_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_addresses_patient
        foreign key (patient_id) references patients (id),
    constraint uk_patient_addresses_patient_id unique (patient_id)
);

create index idx_patient_addresses_agency_id on patient_addresses (agency_id);

create table patient_service_eligibilities (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    service_line_id uuid,
    status varchar(32) not null,
    effective_from date not null,
    effective_to date,
    verification_source varchar(120),
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_service_eligibilities_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_service_eligibilities_patient
        foreign key (patient_id) references patients (id),
    constraint fk_patient_service_eligibilities_service_line
        foreign key (service_line_id) references service_lines (id),
    constraint chk_patient_service_eligibility_status
        check (status in ('ELIGIBLE', 'INELIGIBLE', 'PENDING', 'EXPIRED'))
);

create index idx_patient_service_eligibilities_agency_id on patient_service_eligibilities (agency_id);
create index idx_patient_service_eligibilities_patient_id on patient_service_eligibilities (patient_id);

create table patient_diagnosis_conditions (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    diagnosis_code varchar(50),
    description varchar(500) not null,
    diagnosis_type varchar(100),
    primary_condition boolean not null,
    onset_date date,
    resolved_date date,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_patient_diagnosis_conditions_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_patient_diagnosis_conditions_patient
        foreign key (patient_id) references patients (id),
    constraint chk_patient_diagnosis_status
        check (status in ('ACTIVE', 'RESOLVED', 'HISTORICAL', 'INACTIVE'))
);

create index idx_patient_diagnosis_conditions_agency_id on patient_diagnosis_conditions (agency_id);
create index idx_patient_diagnosis_conditions_patient_id on patient_diagnosis_conditions (patient_id);
