create table compliance_checklist_definitions (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    item_code varchar(100) not null,
    description varchar(1000) not null,
    severity_label varchar(60),
    weight_score integer,
    active boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_compliance_checklist_definitions_code
    on compliance_checklist_definitions (agency_id, item_code);

create table required_documentation_requirements (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    source_category varchar(80) not null,
    requirement_code varchar(100) not null,
    description varchar(1000) not null,
    patient_applicable boolean not null default true,
    episode_applicable boolean not null default false,
    due_days integer,
    recency_days integer,
    requires_signature_verification boolean not null default false,
    requires_attachment_evidence boolean not null default false,
    active boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_required_documentation_requirements_code
    on required_documentation_requirements (agency_id, requirement_code);

create table compliance_checklist_results (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    evaluation_category varchar(40) not null,
    checklist_definition_id uuid references compliance_checklist_definitions (id),
    documentation_requirement_id uuid references required_documentation_requirements (id),
    result_code varchar(100) not null,
    result_status varchar(32) not null,
    evidence_source_type varchar(80),
    evidence_source_id uuid,
    evidence_summary varchar(1000),
    evaluation_origin varchar(80),
    context_period_start date,
    context_period_end date,
    satisfied_by_record_type varchar(80),
    satisfied_by_record_id uuid,
    missing_reason varchar(255),
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_compliance_checklist_results_patient
    on compliance_checklist_results (agency_id, patient_id, evaluated_at);

create table consent_acknowledgment_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    acknowledgment_type varchar(100) not null,
    effective_at timestamp with time zone not null,
    expires_at timestamp with time zone,
    captured_by_membership_id uuid references agency_memberships (id),
    capture_method varchar(80),
    supporting_artifact_type varchar(80),
    supporting_artifact_id uuid,
    status varchar(32) not null,
    revoked_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_consent_acknowledgment_records_patient
    on consent_acknowledgment_records (agency_id, patient_id, acknowledgment_type, effective_at);

create table certification_period_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    patient_payer_link_id uuid references patient_payer_links (id),
    program_context varchar(120),
    start_date date not null,
    end_date date not null,
    record_state varchar(24) not null,
    source varchar(120),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_certification_period_records_patient
    on certification_period_records (agency_id, patient_id, start_date desc);

create table patient_risk_reminders (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    visit_occurrence_id uuid references visit_occurrences (id),
    documentation_record_id uuid references visit_documentation_records (id),
    risk_type varchar(100) not null,
    severity_label varchar(60),
    summary varchar(1000) not null,
    effective_at timestamp with time zone not null,
    expires_at timestamp with time zone,
    source_context_type varchar(80),
    source_record_id uuid,
    status varchar(32) not null,
    resolved_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_patient_risk_reminders_patient
    on patient_risk_reminders (agency_id, patient_id, status, effective_at);

create table compliance_status_projections (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    checklist_pass_count integer not null,
    checklist_warning_count integer not null,
    checklist_fail_count integer not null,
    documentation_satisfied_count integer not null,
    documentation_warning_count integer not null,
    documentation_unsatisfied_count integer not null,
    missing_acknowledgment_count integer not null,
    expired_acknowledgment_count integer not null,
    certification_period_status varchar(32) not null,
    active_risk_reminder_count integer not null,
    readiness_status varchar(32) not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_compliance_status_projections_dashboard
    on compliance_status_projections (agency_id, branch_id, readiness_status, evaluated_at);
