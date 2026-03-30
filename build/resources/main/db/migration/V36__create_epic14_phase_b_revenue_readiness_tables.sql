create table visit_completion_validation_results (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    documentation_record_id uuid references visit_documentation_records (id),
    outcome varchar(16) not null,
    reason_code varchar(120) not null,
    summary varchar(2000) not null,
    visit_execution_completed boolean not null,
    documentation_submitted boolean not null,
    unresolved_review_return boolean not null,
    critical_exception_open boolean not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_visit_completion_validation_visit
    on visit_completion_validation_results (visit_occurrence_id);

create table signed_visit_validation_results (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    verification_session_id uuid references evv_verification_sessions (id),
    outcome varchar(16) not null,
    reason_code varchar(120) not null,
    summary varchar(2000) not null,
    caregiver_signature_status varchar(32) not null,
    patient_signature_status varchar(32) not null,
    evv_verification_status varchar(32),
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_signed_visit_validation_visit
    on signed_visit_validation_results (visit_occurrence_id);

create table revenue_exception_flags (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    target_type varchar(48) not null,
    target_id uuid not null,
    exception_type varchar(64) not null,
    severity varchar(16) not null,
    reason_code varchar(120) not null,
    summary varchar(2000) not null,
    detected_at timestamp with time zone not null,
    cleared_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_revenue_exception_flags_target
    on revenue_exception_flags (agency_id, target_type, target_id, detected_at desc);

create table authorization_usage_snapshots (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    authorization_id uuid references patient_episode_authorizations (id),
    authorized_units integer,
    used_units integer not null,
    remaining_units integer,
    usage_posture varchar(32) not null,
    counted_visit_count integer not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_authorization_usage_snapshots_authorization
    on authorization_usage_snapshots (authorization_id);

create index idx_authorization_usage_snapshots_patient
    on authorization_usage_snapshots (agency_id, patient_id, evaluated_at desc);

create table payer_service_summary_projections (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    patient_payer_link_id uuid references patient_payer_links (id),
    authorization_id uuid references patient_episode_authorizations (id),
    payer_name varchar(255),
    payer_external_id varchar(120),
    member_policy_number varchar(120),
    authorization_number varchar(120),
    service_line_code varchar(50),
    service_line_name varchar(200),
    primary_payer boolean not null,
    summarized_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_payer_service_summary_visit
    on payer_service_summary_projections (visit_occurrence_id);

create table revenue_readiness_projections (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    completion_validation_id uuid references visit_completion_validation_results (id),
    signature_validation_id uuid references signed_visit_validation_results (id),
    payer_service_summary_id uuid references payer_service_summary_projections (id),
    authorization_usage_snapshot_id uuid references authorization_usage_snapshots (id),
    readiness_status varchar(32) not null,
    exception_count integer not null,
    warning_count integer not null,
    export_lifecycle_status varchar(32) not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_revenue_readiness_projection_visit
    on revenue_readiness_projections (visit_occurrence_id);

create index idx_revenue_readiness_projection_branch
    on revenue_readiness_projections (agency_id, branch_id, readiness_status, evaluated_at desc);

create table payroll_export_rows (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid references patients (id),
    caregiver_profile_id uuid references caregiver_profiles (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    scheduled_start_at timestamp with time zone not null,
    scheduled_end_at timestamp with time zone not null,
    performed_start_at timestamp with time zone,
    performed_end_at timestamp with time zone,
    duration_minutes integer not null,
    readiness_status varchar(32) not null,
    export_lifecycle_status varchar(32) not null,
    generated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_payroll_export_rows_branch_generated
    on payroll_export_rows (agency_id, branch_id, generated_at desc);

create table invoice_export_rows (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid references patients (id),
    caregiver_profile_id uuid references caregiver_profiles (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    patient_payer_link_id uuid references patient_payer_links (id),
    authorization_id uuid references patient_episode_authorizations (id),
    payer_name varchar(255),
    authorization_number varchar(120),
    scheduled_start_at timestamp with time zone not null,
    scheduled_end_at timestamp with time zone not null,
    performed_start_at timestamp with time zone,
    performed_end_at timestamp with time zone,
    billable_units integer not null,
    readiness_status varchar(32) not null,
    export_lifecycle_status varchar(32) not null,
    generated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_invoice_export_rows_branch_generated
    on invoice_export_rows (agency_id, branch_id, generated_at desc);
