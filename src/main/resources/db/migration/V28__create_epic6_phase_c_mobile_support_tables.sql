create table mobile_field_artifacts (
    id uuid primary key,
    agency_id uuid not null,
    execution_session_id uuid not null,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid not null,
    patient_id uuid not null,
    branch_id uuid null,
    artifact_type varchar(32) not null,
    file_name varchar(255) not null,
    content_type varchar(150) not null,
    size_bytes bigint not null,
    storage_key varchar(500) not null,
    description varchar(1000) null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_field_artifacts_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_field_artifacts_session
        foreign key (execution_session_id) references mobile_visit_execution_sessions (id),
    constraint fk_mobile_field_artifacts_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_mobile_field_artifacts_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_mobile_field_artifacts_patient
        foreign key (patient_id) references patients (id),
    constraint fk_mobile_field_artifacts_branch
        foreign key (branch_id) references branches (id)
);

create table mobile_incident_reports (
    id uuid primary key,
    agency_id uuid not null,
    execution_session_id uuid not null,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid not null,
    patient_id uuid not null,
    branch_id uuid null,
    incident_type varchar(80) not null,
    severity varchar(40) null,
    narrative varchar(4000) not null,
    reported_at timestamp with time zone not null,
    status varchar(32) not null,
    escalation_hook varchar(120) null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_incident_reports_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_incident_reports_session
        foreign key (execution_session_id) references mobile_visit_execution_sessions (id),
    constraint fk_mobile_incident_reports_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_mobile_incident_reports_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_mobile_incident_reports_patient
        foreign key (patient_id) references patients (id),
    constraint fk_mobile_incident_reports_branch
        foreign key (branch_id) references branches (id)
);

create table mobile_incident_artifact_refs (
    incident_report_id uuid not null,
    artifact_id uuid not null,
    primary key (incident_report_id, artifact_id),
    constraint fk_mobile_incident_artifact_refs_incident
        foreign key (incident_report_id) references mobile_incident_reports (id),
    constraint fk_mobile_incident_artifact_refs_artifact
        foreign key (artifact_id) references mobile_field_artifacts (id)
);

create table mobile_message_threads (
    id uuid primary key,
    agency_id uuid not null,
    caregiver_profile_id uuid not null,
    branch_id uuid null,
    patient_id uuid null,
    visit_occurrence_id uuid null,
    subject varchar(200) not null,
    last_message_at timestamp with time zone null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_message_threads_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_message_threads_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_mobile_message_threads_branch
        foreign key (branch_id) references branches (id),
    constraint fk_mobile_message_threads_patient
        foreign key (patient_id) references patients (id),
    constraint fk_mobile_message_threads_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id)
);

create table mobile_message_entries (
    id uuid primary key,
    agency_id uuid not null,
    thread_id uuid not null,
    sender_membership_id uuid not null,
    sent_at timestamp with time zone not null,
    message_text varchar(4000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_message_entries_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_message_entries_thread
        foreign key (thread_id) references mobile_message_threads (id),
    constraint fk_mobile_message_entries_sender
        foreign key (sender_membership_id) references agency_memberships (id)
);

create index idx_mobile_field_artifacts_session on mobile_field_artifacts (execution_session_id, created_at);
create index idx_mobile_incident_reports_session on mobile_incident_reports (execution_session_id, reported_at desc);
create index idx_mobile_message_threads_caregiver on mobile_message_threads (caregiver_profile_id, last_message_at desc, created_at desc);
create index idx_mobile_message_entries_thread on mobile_message_entries (thread_id, sent_at);
