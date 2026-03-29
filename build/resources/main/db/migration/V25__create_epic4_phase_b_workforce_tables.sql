create table caregiver_profiles (
    id uuid primary key,
    agency_id uuid not null,
    agency_membership_id uuid not null,
    primary_branch_id uuid,
    caregiver_code varchar(100),
    display_name varchar(200),
    employment_type varchar(60),
    start_date date,
    end_date date,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_profiles_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_profiles_membership
        foreign key (agency_membership_id) references agency_memberships (id),
    constraint fk_caregiver_profiles_branch
        foreign key (primary_branch_id) references branches (id),
    constraint uq_caregiver_profiles_membership
        unique (agency_id, agency_membership_id),
    constraint chk_caregiver_profiles_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_profiles_agency_id on caregiver_profiles (agency_id);
create index idx_caregiver_profiles_membership_id on caregiver_profiles (agency_membership_id);

create table caregiver_credentials (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    certification_id uuid,
    credential_type varchar(120) not null,
    license_number varchar(120),
    issuing_authority varchar(200),
    issued_on date,
    expires_on date,
    status varchar(32) not null,
    verification_status varchar(32),
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_credentials_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_credentials_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_caregiver_credentials_certification
        foreign key (certification_id) references caregiver_certifications (id),
    constraint chk_caregiver_credentials_status
        check (status in ('ACTIVE', 'EXPIRED', 'SUSPENDED', 'ARCHIVED')),
    constraint chk_caregiver_credentials_verification
        check (verification_status in ('UNVERIFIED', 'VERIFIED', 'REJECTED') or verification_status is null)
);

create index idx_caregiver_credentials_agency_id on caregiver_credentials (agency_id);
create index idx_caregiver_credentials_profile_id on caregiver_credentials (caregiver_profile_id);

create table caregiver_languages (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    language_code varchar(35) not null,
    proficiency_level varchar(40),
    primary_language boolean not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_languages_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_languages_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint uq_caregiver_languages_profile_code
        unique (caregiver_profile_id, language_code),
    constraint chk_caregiver_languages_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_languages_agency_id on caregiver_languages (agency_id);
create index idx_caregiver_languages_profile_id on caregiver_languages (caregiver_profile_id);

create table caregiver_skill_profiles (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    skill_id uuid not null,
    proficiency_level varchar(40),
    verified boolean not null,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_skill_profiles_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_skill_profiles_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_caregiver_skill_profiles_skill
        foreign key (skill_id) references caregiver_skills (id),
    constraint uq_caregiver_skill_profiles_profile_skill
        unique (caregiver_profile_id, skill_id),
    constraint chk_caregiver_skill_profiles_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_skill_profiles_agency_id on caregiver_skill_profiles (agency_id);
create index idx_caregiver_skill_profiles_profile_id on caregiver_skill_profiles (caregiver_profile_id);

create table caregiver_geography_preferences (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    branch_id uuid,
    preference_type varchar(32) not null,
    postal_code varchar(20),
    city varchar(120),
    state varchar(80),
    anchor_latitude numeric(10, 6),
    anchor_longitude numeric(10, 6),
    radius_miles numeric(8, 2),
    priority_rank integer,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_geography_preferences_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_geography_preferences_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_caregiver_geography_preferences_branch
        foreign key (branch_id) references branches (id),
    constraint chk_caregiver_geography_preference_type
        check (preference_type in ('BRANCH', 'POSTAL_CODE', 'CITY_STATE', 'RADIUS')),
    constraint chk_caregiver_geography_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_geography_preferences_agency_id on caregiver_geography_preferences (agency_id);
create index idx_caregiver_geography_preferences_profile_id on caregiver_geography_preferences (caregiver_profile_id);

create table caregiver_shift_preferences (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    day_of_week varchar(12),
    preferred_start_time time,
    preferred_end_time time,
    preferred_shift_length_minutes integer,
    preferred_visit_types varchar(500),
    preference_strength varchar(32),
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_shift_preferences_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_shift_preferences_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint chk_caregiver_shift_preference_strength
        check (preference_strength in ('PREFERRED', 'AVAILABLE_ONLY', 'AVOID') or preference_strength is null),
    constraint chk_caregiver_shift_preferences_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_shift_preferences_agency_id on caregiver_shift_preferences (agency_id);
create index idx_caregiver_shift_preferences_profile_id on caregiver_shift_preferences (caregiver_profile_id);

create table caregiver_availabilities (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    branch_id uuid,
    availability_type varchar(32) not null,
    starts_at timestamp with time zone,
    ends_at timestamp with time zone,
    day_of_week varchar(12),
    start_time time,
    end_time time,
    effective_from date,
    effective_to date,
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_availabilities_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_availabilities_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_caregiver_availabilities_branch
        foreign key (branch_id) references branches (id),
    constraint chk_caregiver_availability_type
        check (availability_type in ('RECURRING', 'DATE_SPECIFIC')),
    constraint chk_caregiver_availabilities_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_availabilities_agency_id on caregiver_availabilities (agency_id);
create index idx_caregiver_availabilities_profile_id on caregiver_availabilities (caregiver_profile_id);

create table caregiver_unavailabilities (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    reason_type varchar(32) not null,
    starts_at timestamp with time zone not null,
    ends_at timestamp with time zone not null,
    all_day boolean not null,
    approval_status varchar(32),
    status varchar(32) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_unavailabilities_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_unavailabilities_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint chk_caregiver_unavailability_reason
        check (reason_type in ('PTO', 'SICK', 'TRAINING', 'BLOCKED', 'OTHER')),
    constraint chk_caregiver_unavailability_approval
        check (approval_status in ('PENDING', 'APPROVED', 'REJECTED') or approval_status is null),
    constraint chk_caregiver_unavailabilities_status
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNSCHEDULABLE', 'ARCHIVED'))
);

create index idx_caregiver_unavailabilities_agency_id on caregiver_unavailabilities (agency_id);
create index idx_caregiver_unavailabilities_profile_id on caregiver_unavailabilities (caregiver_profile_id);
