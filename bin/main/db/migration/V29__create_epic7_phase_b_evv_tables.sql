create table evv_geofence_tolerance_rules (
    id uuid primary key,
    agency_id uuid not null,
    branch_id uuid,
    rule_name varchar(120) not null,
    tolerance_meters integer not null,
    warning_buffer_meters integer not null,
    hard_block_outside_tolerance boolean not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_evv_geofence_tolerance_rules_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_evv_geofence_tolerance_rules_branch
        foreign key (branch_id) references branches (id)
);

create index idx_evv_geofence_tolerance_rules_agency_id
    on evv_geofence_tolerance_rules (agency_id);

create index idx_evv_geofence_tolerance_rules_branch_id
    on evv_geofence_tolerance_rules (branch_id);

create table evv_verification_sessions (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid not null,
    patient_id uuid not null,
    branch_id uuid,
    execution_session_id uuid,
    opened_at timestamp with time zone not null,
    closed_at timestamp with time zone,
    verification_status varchar(40) not null,
    compliance_outcome varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_evv_verification_sessions_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_evv_verification_sessions_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_evv_verification_sessions_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_evv_verification_sessions_patient
        foreign key (patient_id) references patients (id),
    constraint fk_evv_verification_sessions_branch
        foreign key (branch_id) references branches (id),
    constraint fk_evv_verification_sessions_execution
        foreign key (execution_session_id) references mobile_visit_execution_sessions (id)
);

create index idx_evv_verification_sessions_agency_id
    on evv_verification_sessions (agency_id);

create index idx_evv_verification_sessions_visit_caregiver
    on evv_verification_sessions (visit_occurrence_id, caregiver_profile_id);

create table evv_clock_events (
    id uuid primary key,
    agency_id uuid not null,
    verification_session_id uuid not null,
    caregiver_profile_id uuid not null,
    patient_id uuid not null,
    branch_id uuid,
    event_type varchar(16) not null,
    captured_at timestamp with time zone not null,
    captured_latitude numeric(9,6),
    captured_longitude numeric(9,6),
    timezone varchar(64) not null,
    capture_source varchar(32) not null,
    verification_status varchar(40) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_evv_clock_events_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_evv_clock_events_session
        foreign key (verification_session_id) references evv_verification_sessions (id),
    constraint fk_evv_clock_events_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_evv_clock_events_patient
        foreign key (patient_id) references patients (id),
    constraint fk_evv_clock_events_branch
        foreign key (branch_id) references branches (id)
);

create index idx_evv_clock_events_session_id
    on evv_clock_events (verification_session_id);

create index idx_evv_clock_events_captured_at
    on evv_clock_events (captured_at);

create table evv_device_metadata_snapshots (
    id uuid primary key,
    agency_id uuid not null,
    clock_event_id uuid not null,
    platform_summary varchar(80),
    app_version varchar(40),
    device_class varchar(40),
    timezone_offset_minutes integer,
    user_agent_hash varchar(128),
    session_fingerprint_hash varchar(128),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_evv_device_metadata_snapshots_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_evv_device_metadata_snapshots_clock_event
        foreign key (clock_event_id) references evv_clock_events (id)
);

create unique index uq_evv_device_metadata_snapshots_clock_event
    on evv_device_metadata_snapshots (clock_event_id);

create table evv_geofence_evaluations (
    id uuid primary key,
    agency_id uuid not null,
    verification_session_id uuid not null,
    clock_event_id uuid not null,
    geofence_rule_id uuid,
    outcome varchar(40) not null,
    distance_from_expected_meters integer not null,
    tolerance_meters_used integer not null,
    blocking boolean not null,
    reason_code varchar(64) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_evv_geofence_evaluations_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_evv_geofence_evaluations_session
        foreign key (verification_session_id) references evv_verification_sessions (id),
    constraint fk_evv_geofence_evaluations_clock_event
        foreign key (clock_event_id) references evv_clock_events (id),
    constraint fk_evv_geofence_evaluations_rule
        foreign key (geofence_rule_id) references evv_geofence_tolerance_rules (id)
);

create index idx_evv_geofence_evaluations_session_id
    on evv_geofence_evaluations (verification_session_id);

create table evv_signature_verification_links (
    id uuid primary key,
    agency_id uuid not null,
    verification_session_id uuid not null,
    artifact_id uuid,
    signer_role varchar(24) not null,
    verification_status varchar(24) not null,
    recorded_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_evv_signature_verification_links_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_evv_signature_verification_links_session
        foreign key (verification_session_id) references evv_verification_sessions (id),
    constraint fk_evv_signature_verification_links_artifact
        foreign key (artifact_id) references mobile_field_artifacts (id)
);

create index idx_evv_signature_verification_links_session_id
    on evv_signature_verification_links (verification_session_id);

create table missed_visit_records (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid,
    patient_id uuid not null,
    branch_id uuid,
    reported_by_membership_id uuid not null,
    reason_code varchar(64) not null,
    narrative varchar(2000) not null,
    reported_at timestamp with time zone not null,
    status varchar(24) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_missed_visit_records_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_missed_visit_records_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_missed_visit_records_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_missed_visit_records_patient
        foreign key (patient_id) references patients (id),
    constraint fk_missed_visit_records_branch
        foreign key (branch_id) references branches (id),
    constraint fk_missed_visit_records_reported_by
        foreign key (reported_by_membership_id) references agency_memberships (id)
);

create index idx_missed_visit_records_visit_id
    on missed_visit_records (visit_occurrence_id);

create table visit_exception_records (
    id uuid primary key,
    agency_id uuid not null,
    verification_session_id uuid,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid,
    patient_id uuid not null,
    branch_id uuid,
    created_by_membership_id uuid not null,
    exception_type varchar(40) not null,
    severity varchar(16) not null,
    reason_code varchar(64) not null,
    narrative varchar(2000) not null,
    status varchar(24) not null,
    acknowledged_at timestamp with time zone,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_visit_exception_records_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_visit_exception_records_session
        foreign key (verification_session_id) references evv_verification_sessions (id),
    constraint fk_visit_exception_records_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_visit_exception_records_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_visit_exception_records_patient
        foreign key (patient_id) references patients (id),
    constraint fk_visit_exception_records_branch
        foreign key (branch_id) references branches (id),
    constraint fk_visit_exception_records_created_by
        foreign key (created_by_membership_id) references agency_memberships (id)
);

create index idx_visit_exception_records_session_id
    on visit_exception_records (verification_session_id);

create table supervisor_notification_events (
    id uuid primary key,
    agency_id uuid not null,
    missed_visit_record_id uuid,
    visit_exception_record_id uuid,
    recipient_membership_id uuid not null,
    created_by_membership_id uuid not null,
    branch_id uuid,
    channel varchar(32) not null,
    rationale varchar(1000) not null,
    created_at_event timestamp with time zone not null,
    status varchar(16) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_supervisor_notification_events_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_supervisor_notification_events_missed_visit
        foreign key (missed_visit_record_id) references missed_visit_records (id),
    constraint fk_supervisor_notification_events_exception
        foreign key (visit_exception_record_id) references visit_exception_records (id),
    constraint fk_supervisor_notification_events_recipient
        foreign key (recipient_membership_id) references agency_memberships (id),
    constraint fk_supervisor_notification_events_created_by
        foreign key (created_by_membership_id) references agency_memberships (id),
    constraint fk_supervisor_notification_events_branch
        foreign key (branch_id) references branches (id),
    constraint chk_supervisor_notification_events_source
        check (
            (missed_visit_record_id is not null and visit_exception_record_id is null)
            or (missed_visit_record_id is null and visit_exception_record_id is not null)
        )
);

create table escalation_requests (
    id uuid primary key,
    agency_id uuid not null,
    missed_visit_record_id uuid,
    visit_exception_record_id uuid,
    created_by_membership_id uuid not null,
    branch_id uuid,
    target_role_key varchar(64) not null,
    severity varchar(16) not null,
    rationale varchar(1000) not null,
    sla_due_at timestamp with time zone,
    status varchar(16) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_escalation_requests_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_escalation_requests_missed_visit
        foreign key (missed_visit_record_id) references missed_visit_records (id),
    constraint fk_escalation_requests_exception
        foreign key (visit_exception_record_id) references visit_exception_records (id),
    constraint fk_escalation_requests_created_by
        foreign key (created_by_membership_id) references agency_memberships (id),
    constraint fk_escalation_requests_branch
        foreign key (branch_id) references branches (id),
    constraint chk_escalation_requests_source
        check (
            (missed_visit_record_id is not null and visit_exception_record_id is null)
            or (missed_visit_record_id is null and visit_exception_record_id is not null)
        )
);
