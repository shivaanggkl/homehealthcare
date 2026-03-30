create table dashboard_metric_definitions (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    metric_type varchar(48) not null,
    metric_scope varchar(24) not null,
    metric_name varchar(160) not null,
    description varchar(1000) not null,
    source_domain_key varchar(80) not null,
    refresh_mode varchar(24) not null,
    max_staleness_minutes integer not null,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_dashboard_metric_definitions_metric
    on dashboard_metric_definitions (agency_id, metric_type);

create table dashboard_metric_snapshots (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    metric_definition_id uuid not null references dashboard_metric_definitions (id),
    branch_id uuid references branches (id),
    snapshot_date date not null,
    primary_value integer not null,
    secondary_value integer not null,
    status_code varchar(64),
    drilldown_reference_json varchar(4000) not null,
    captured_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_dashboard_metric_snapshots_scope
    on dashboard_metric_snapshots (agency_id, snapshot_date, branch_id, captured_at desc);

create table branch_performance_summaries (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid not null references branches (id),
    snapshot_date date not null,
    todays_visit_count integer not null,
    unfilled_visit_count integer not null,
    late_start_count integer not null,
    missed_visit_count integer not null,
    documentation_aging_count integer not null,
    qa_backlog_count integer not null,
    posture varchar(24) not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_branch_performance_summaries_scope
    on branch_performance_summaries (agency_id, snapshot_date, branch_id, evaluated_at desc);

create table utilization_summaries (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    caregiver_profile_id uuid not null references caregiver_profiles (id),
    snapshot_date date not null,
    assigned_visit_count integer not null,
    completed_visit_count integer not null,
    scheduled_minutes integer not null,
    posture varchar(24) not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_utilization_summaries_scope
    on utilization_summaries (agency_id, snapshot_date, branch_id, caregiver_profile_id);

create table backlog_summaries (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid not null references branches (id),
    snapshot_date date not null,
    documentation_aging_count integer not null,
    aging_bucket_json varchar(2000) not null,
    pending_review_count integer not null,
    returned_for_fix_count integer not null,
    overdue_review_count integer not null,
    posture varchar(24) not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_backlog_summaries_scope
    on backlog_summaries (agency_id, snapshot_date, branch_id, evaluated_at desc);

create table readiness_compliance_summaries (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid not null references branches (id),
    snapshot_date date not null,
    revenue_ready_count integer not null,
    revenue_warning_count integer not null,
    revenue_blocked_count integer not null,
    compliance_ready_count integer not null,
    compliance_warning_count integer not null,
    compliance_exception_count integer not null,
    evaluated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_readiness_compliance_summaries_scope
    on readiness_compliance_summaries (agency_id, snapshot_date, branch_id, evaluated_at desc);

create table metric_trend_snapshots (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    metric_type varchar(48) not null,
    branch_id uuid references branches (id),
    snapshot_date date not null,
    current_value integer not null,
    previous_value integer not null,
    delta_value integer not null,
    direction varchar(16) not null,
    calculated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_metric_trend_snapshots_scope
    on metric_trend_snapshots (agency_id, metric_type, snapshot_date, branch_id);

create table dashboard_snapshots (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    snapshot_date date not null,
    todays_visit_count integer not null,
    unfilled_visit_count integer not null,
    late_start_count integer not null,
    missed_visit_count integer not null,
    documentation_aging_count integer not null,
    qa_backlog_count integer not null,
    caregiver_utilization_count integer not null,
    revenue_blocked_count integer not null,
    compliance_exception_count integer not null,
    generated_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_dashboard_snapshots_scope
    on dashboard_snapshots (agency_id, snapshot_date, branch_id, generated_at desc);
