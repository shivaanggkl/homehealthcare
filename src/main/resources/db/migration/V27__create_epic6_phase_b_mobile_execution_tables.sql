create table mobile_visit_execution_sessions (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid not null,
    patient_id uuid not null,
    branch_id uuid null,
    started_at timestamp with time zone not null,
    ended_at timestamp with time zone null,
    started_latitude numeric(9, 6) null,
    started_longitude numeric(9, 6) null,
    ended_latitude numeric(9, 6) null,
    ended_longitude numeric(9, 6) null,
    start_source varchar(32) null,
    end_source varchar(32) null,
    execution_status varchar(32) not null,
    sync_status varchar(32) null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_visit_execution_sessions_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_visit_execution_sessions_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_mobile_visit_execution_sessions_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_mobile_visit_execution_sessions_patient
        foreign key (patient_id) references patients (id),
    constraint fk_mobile_visit_execution_sessions_branch
        foreign key (branch_id) references branches (id)
);

create index idx_mobile_visit_execution_sessions_visit
    on mobile_visit_execution_sessions (visit_occurrence_id, caregiver_profile_id, started_at desc);

create table mobile_visit_task_checklist_entries (
    id uuid primary key,
    agency_id uuid not null,
    execution_session_id uuid not null,
    task_template_id uuid null,
    title varchar(200) not null,
    description varchar(1000) null,
    category varchar(64) null,
    sort_order integer not null,
    completed boolean not null,
    completed_at timestamp with time zone null,
    completion_notes varchar(1000) null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_visit_task_entries_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_visit_task_entries_session
        foreign key (execution_session_id) references mobile_visit_execution_sessions (id),
    constraint fk_mobile_visit_task_entries_template
        foreign key (task_template_id) references task_templates (id)
);

create index idx_mobile_visit_task_entries_session
    on mobile_visit_task_checklist_entries (execution_session_id, sort_order, created_at);

create table mobile_quick_note_entries (
    id uuid primary key,
    agency_id uuid not null,
    execution_session_id uuid not null,
    caregiver_profile_id uuid not null,
    authored_at timestamp with time zone not null,
    note_text varchar(2000) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_mobile_quick_note_entries_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_mobile_quick_note_entries_session
        foreign key (execution_session_id) references mobile_visit_execution_sessions (id),
    constraint fk_mobile_quick_note_entries_caregiver
        foreign key (caregiver_profile_id) references caregiver_profiles (id)
);

create index idx_mobile_quick_note_entries_session
    on mobile_quick_note_entries (execution_session_id, authored_at);
