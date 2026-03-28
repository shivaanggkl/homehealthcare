create table agency_profiles (
    id uuid primary key,
    agency_id uuid not null unique references agencies (id),
    display_name varchar(200),
    legal_name varchar(200),
    primary_phone varchar(30),
    primary_address varchar(500),
    operations_contact_name varchar(200),
    operations_contact_email varchar(320),
    support_contact_name varchar(200),
    support_contact_email varchar(320),
    default_timezone varchar(64) not null,
    default_locale varchar(35) not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table service_lines (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    name varchar(200) not null,
    code varchar(50) not null,
    description varchar(1000),
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_service_lines_agency_name on service_lines (agency_id, name);
create unique index ux_service_lines_agency_code on service_lines (agency_id, code);

create table visit_types (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    service_line_id uuid references service_lines (id),
    name varchar(200) not null,
    code varchar(50) not null,
    description varchar(1000),
    default_duration_minutes integer not null,
    billable boolean not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_visit_types_agency_name on visit_types (agency_id, name);
create unique index ux_visit_types_agency_code on visit_types (agency_id, code);

create table caregiver_skills (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    name varchar(200) not null,
    code varchar(50) not null,
    description varchar(1000),
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_caregiver_skills_agency_name on caregiver_skills (agency_id, name);
create unique index ux_caregiver_skills_agency_code on caregiver_skills (agency_id, code);

create table caregiver_certifications (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    name varchar(200) not null,
    code varchar(50) not null,
    description varchar(1000),
    expiration_required boolean not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_caregiver_certifications_agency_name on caregiver_certifications (agency_id, name);
create unique index ux_caregiver_certifications_agency_code on caregiver_certifications (agency_id, code);
