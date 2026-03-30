create table goal_templates (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    service_line_id uuid references service_lines (id),
    name varchar(200) not null,
    description varchar(2000),
    target_outcome_guidance varchar(2000),
    default_intervention_scaffold varchar(2000),
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_goal_templates_agency_status on goal_templates (agency_id, status);
create index idx_goal_templates_agency_branch on goal_templates (agency_id, branch_id);

create table patient_goals (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    goal_template_id uuid references goal_templates (id),
    owner_membership_id uuid references agency_memberships (id),
    title varchar(200) not null,
    description varchar(2000),
    target_date date,
    status varchar(32) not null,
    created_at timestamp not null,
    resolved_at timestamp,
    updated_at timestamp not null
);

create index idx_patient_goals_agency_patient on patient_goals (agency_id, patient_id, created_at desc);
create index idx_patient_goals_agency_branch on patient_goals (agency_id, branch_id, created_at desc);
create index idx_patient_goals_agency_status on patient_goals (agency_id, status, created_at desc);

create table goal_interventions (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_goal_id uuid not null references patient_goals (id),
    branch_id uuid references branches (id),
    owner_membership_id uuid references agency_memberships (id),
    title varchar(200) not null,
    description varchar(2000),
    target_date date,
    status varchar(32) not null,
    derived_from_template boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_goal_interventions_goal on goal_interventions (patient_goal_id, target_date, id);

create table goal_progress_notes (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_goal_id uuid not null references patient_goals (id),
    goal_intervention_id uuid references goal_interventions (id),
    branch_id uuid references branches (id),
    captured_by_membership_id uuid not null references agency_memberships (id),
    note_text varchar(4000) not null,
    captured_at timestamp not null,
    progression_summary varchar(2000),
    status_impact varchar(100),
    lifecycle_status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_goal_progress_notes_goal on goal_progress_notes (patient_goal_id, captured_at desc);

create table goal_version_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_goal_id uuid not null references patient_goals (id),
    branch_id uuid references branches (id),
    changed_by_membership_id uuid references agency_memberships (id),
    version_number integer not null,
    change_type varchar(80) not null,
    changed_at timestamp not null,
    snapshot_json varchar(8000) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index idx_goal_version_goal_version on goal_version_records (patient_goal_id, version_number);
create index idx_goal_version_goal_changed_at on goal_version_records (patient_goal_id, changed_at desc);

create table careplan_sync_links (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    patient_goal_id uuid not null references patient_goals (id),
    branch_id uuid references branches (id),
    careplan_identifier varchar(120) not null,
    sync_status varchar(32) not null,
    last_synced_at timestamp,
    sync_source varchar(120),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_careplan_sync_goal on careplan_sync_links (patient_goal_id, careplan_identifier);
