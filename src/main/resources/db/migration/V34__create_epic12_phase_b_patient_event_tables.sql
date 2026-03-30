create table incident_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    visit_occurrence_id uuid references visit_occurrences (id),
    incident_type varchar(100) not null,
    severity_label varchar(60),
    occurred_at timestamp with time zone not null,
    reported_at timestamp with time zone not null,
    summary varchar(2000) not null,
    status varchar(32) not null,
    reported_by_membership_id uuid references agency_memberships (id),
    resolved_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_incident_records_patient
    on incident_records (agency_id, patient_id, occurred_at desc);

create table infection_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    related_incident_id uuid references incident_records (id),
    onset_date date,
    identified_at timestamp with time zone not null,
    infection_type varchar(120) not null,
    summary varchar(2000) not null,
    status varchar(32) not null,
    resolved_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_infection_records_patient
    on infection_records (agency_id, patient_id, identified_at desc);

create table wound_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    identified_at timestamp with time zone not null,
    wound_type_or_site varchar(160) not null,
    current_status varchar(32) not null,
    baseline_summary varchar(2000),
    active boolean not null,
    resolved_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_wound_records_patient
    on wound_records (agency_id, patient_id, identified_at desc);

create table wound_history_entries (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    wound_record_id uuid not null references wound_records (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    captured_at timestamp with time zone not null,
    observation_summary varchar(2000) not null,
    length_cm numeric(10, 2),
    width_cm numeric(10, 2),
    depth_cm numeric(10, 2),
    progression_marker varchar(120),
    captured_by_membership_id uuid references agency_memberships (id),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_wound_history_entries_wound
    on wound_history_entries (agency_id, wound_record_id, captured_at);

create table patient_event_evidence_links (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    target_type varchar(48) not null,
    target_id uuid not null,
    source_type varchar(48) not null,
    patient_attachment_id uuid references patient_attachments (id),
    mobile_artifact_id uuid references mobile_field_artifacts (id),
    documentation_attachment_link_id uuid references documentation_attachment_links (id),
    linked_by_membership_id uuid not null references agency_memberships (id),
    linked_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_patient_event_evidence_links_target
    on patient_event_evidence_links (agency_id, target_type, target_id, linked_at);

create table patient_event_follow_up_assignments (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    target_type varchar(48) not null,
    target_id uuid not null,
    owner_membership_id uuid references agency_memberships (id),
    owner_role varchar(48),
    assigned_at timestamp with time zone not null,
    due_at timestamp with time zone not null,
    completion_at timestamp with time zone,
    follow_up_note varchar(2000),
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_patient_event_follow_up_assignments_target
    on patient_event_follow_up_assignments (agency_id, target_type, target_id, assigned_at);

create index idx_patient_event_follow_up_assignments_patient
    on patient_event_follow_up_assignments (agency_id, patient_id, due_at);

create table patient_event_escalation_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    target_type varchar(48) not null,
    target_id uuid not null,
    status varchar(32) not null,
    severity_label varchar(60),
    reason_tag varchar(255),
    escalated_by_membership_id uuid not null references agency_memberships (id),
    escalated_at timestamp with time zone not null,
    cleared_by_membership_id uuid references agency_memberships (id),
    cleared_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_patient_event_escalation_records_target
    on patient_event_escalation_records (agency_id, target_type, target_id, escalated_at);

create index idx_patient_event_escalation_records_patient
    on patient_event_escalation_records (agency_id, patient_id, escalated_at);
