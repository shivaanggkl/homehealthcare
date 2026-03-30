create table review_work_items (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    source_type varchar(48) not null,
    source_record_id uuid not null,
    branch_id uuid references branches (id),
    patient_id uuid references patients (id),
    visit_occurrence_id uuid references visit_occurrences (id),
    documentation_record_id uuid references visit_documentation_records (id),
    status varchar(32) not null,
    priority varchar(24),
    exception_driven boolean not null default false,
    entered_queue_at timestamp with time zone not null,
    due_at timestamp with time zone,
    last_activity_at timestamp with time zone not null,
    resolved_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_review_work_items_source
    on review_work_items (agency_id, source_type, source_record_id);

create index idx_review_work_items_status
    on review_work_items (agency_id, status, exception_driven, entered_queue_at);

create table completeness_check_results (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    evaluated_source_type varchar(48) not null,
    evaluated_source_id uuid not null,
    run_number integer not null,
    pass_count integer not null,
    warning_count integer not null,
    fail_count integer not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table review_findings (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    completeness_check_result_id uuid references completeness_check_results (id),
    finding_kind varchar(32) not null,
    evaluated_source_type varchar(48) not null,
    evaluated_source_id uuid not null,
    rule_code varchar(100) not null,
    severity varchar(24) not null,
    finding_status varchar(24) not null,
    field_path varchar(255),
    logical_section varchar(120),
    explanation varchar(2000) not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table missing_field_results (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    completeness_check_result_id uuid not null references completeness_check_results (id),
    review_finding_id uuid not null references review_findings (id),
    evaluated_source_type varchar(48) not null,
    evaluated_source_id uuid not null,
    rule_code varchar(100) not null,
    severity varchar(24) not null,
    finding_status varchar(24) not null,
    field_path varchar(255) not null,
    logical_section varchar(120),
    explanation varchar(2000) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table review_assignments (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    reviewer_membership_id uuid not null references agency_memberships (id),
    assigned_by_membership_id uuid not null references agency_memberships (id),
    assigned_at timestamp with time zone not null,
    released_at timestamp with time zone,
    assignment_note varchar(1000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_review_assignments_active
    on review_assignments (work_item_id, released_at);

create table review_decisions (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    decision_type varchar(32) not null,
    decided_by_membership_id uuid not null references agency_memberships (id),
    decided_at timestamp with time zone not null,
    reason_code varchar(100),
    reviewer_notes varchar(2000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table return_for_fix_events (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    target_source_type varchar(48) not null,
    target_source_record_id uuid not null,
    return_reason varchar(1000) not null,
    required_corrections varchar(2000),
    returned_by_membership_id uuid not null references agency_memberships (id),
    returned_at timestamp with time zone not null,
    resubmitted_at timestamp with time zone,
    resolved_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table review_exceptions (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    source_type varchar(48) not null,
    source_record_id uuid not null,
    exception_type varchar(48) not null,
    severity varchar(24) not null,
    detected_at timestamp with time zone not null,
    resolved_at timestamp with time zone,
    resolution_note varchar(1000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_review_exceptions_queue
    on review_exceptions (agency_id, exception_type, severity, resolved_at);

create table signoff_requests (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    work_item_id uuid not null references review_work_items (id),
    requested_from_membership_id uuid references agency_memberships (id),
    requested_from_role varchar(64),
    requested_by_membership_id uuid not null references agency_memberships (id),
    requested_at timestamp with time zone not null,
    status varchar(24) not null,
    signoff_note varchar(2000),
    completed_by_membership_id uuid references agency_memberships (id),
    completed_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);
