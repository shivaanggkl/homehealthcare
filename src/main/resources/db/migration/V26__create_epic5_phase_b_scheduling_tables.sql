create table recurring_visit_rules (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    branch_id uuid,
    service_line_id uuid,
    visit_type_id uuid,
    cadence varchar(32) not null,
    weekday_pattern varchar(100),
    effective_start date not null,
    effective_end date,
    planned_start_time time not null,
    planned_end_time time not null,
    timezone varchar(64) not null,
    priority varchar(32),
    creation_mode varchar(32),
    notes varchar(1000),
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_recurring_visit_rules_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_recurring_visit_rules_patient
        foreign key (patient_id) references patients (id),
    constraint fk_recurring_visit_rules_branch
        foreign key (branch_id) references branches (id),
    constraint fk_recurring_visit_rules_service_line
        foreign key (service_line_id) references service_lines (id),
    constraint fk_recurring_visit_rules_visit_type
        foreign key (visit_type_id) references visit_types (id),
    constraint chk_recurring_visit_rules_cadence
        check (cadence in ('DAILY', 'SELECTED_WEEKDAYS', 'WEEKLY', 'BIWEEKLY')),
    constraint chk_recurring_visit_rules_status
        check (status in ('ACTIVE', 'INACTIVE'))
);

create index idx_recurring_visit_rules_agency_id on recurring_visit_rules (agency_id);
create index idx_recurring_visit_rules_patient_id on recurring_visit_rules (patient_id);

create table visit_occurrences (
    id uuid primary key,
    agency_id uuid not null,
    patient_id uuid not null,
    branch_id uuid,
    service_line_id uuid,
    visit_type_id uuid,
    recurring_visit_rule_id uuid,
    planned_start_at timestamp with time zone not null,
    planned_end_at timestamp with time zone not null,
    timezone varchar(64) not null,
    status varchar(32) not null,
    priority varchar(32),
    creation_mode varchar(32),
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_visit_occurrences_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_visit_occurrences_patient
        foreign key (patient_id) references patients (id),
    constraint fk_visit_occurrences_branch
        foreign key (branch_id) references branches (id),
    constraint fk_visit_occurrences_service_line
        foreign key (service_line_id) references service_lines (id),
    constraint fk_visit_occurrences_visit_type
        foreign key (visit_type_id) references visit_types (id),
    constraint fk_visit_occurrences_recurring_visit_rule
        foreign key (recurring_visit_rule_id) references recurring_visit_rules (id),
    constraint chk_visit_occurrences_status
        check (status in ('PLANNED', 'ASSIGNED', 'OPEN_SHIFT', 'RESCHEDULED', 'CANCELLED'))
);

create index idx_visit_occurrences_agency_id on visit_occurrences (agency_id);
create index idx_visit_occurrences_patient_id on visit_occurrences (patient_id);
create index idx_visit_occurrences_planned_start_at on visit_occurrences (planned_start_at);

create table caregiver_visit_assignments (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    caregiver_profile_id uuid not null,
    branch_id uuid,
    assigned_by_membership_id uuid not null,
    assigned_at timestamp with time zone not null,
    assignment_status varchar(32) not null,
    assignment_source varchar(32),
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_caregiver_visit_assignments_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_caregiver_visit_assignments_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_caregiver_visit_assignments_profile
        foreign key (caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_caregiver_visit_assignments_branch
        foreign key (branch_id) references branches (id),
    constraint fk_caregiver_visit_assignments_assigned_by
        foreign key (assigned_by_membership_id) references agency_memberships (id),
    constraint chk_caregiver_visit_assignments_status
        check (assignment_status in ('ACTIVE', 'REMOVED', 'REASSIGNED', 'CANCELLED'))
);

create index idx_caregiver_visit_assignments_agency_id on caregiver_visit_assignments (agency_id);
create index idx_caregiver_visit_assignments_visit_id on caregiver_visit_assignments (visit_occurrence_id);
create index idx_caregiver_visit_assignments_profile_id on caregiver_visit_assignments (caregiver_profile_id);

create table open_shifts (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    branch_id uuid,
    opened_by_membership_id uuid not null,
    closed_by_membership_id uuid,
    opened_at timestamp with time zone not null,
    closed_at timestamp with time zone,
    status varchar(32) not null,
    priority varchar(32),
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_open_shifts_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_open_shifts_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_open_shifts_branch
        foreign key (branch_id) references branches (id),
    constraint fk_open_shifts_opened_by
        foreign key (opened_by_membership_id) references agency_memberships (id),
    constraint fk_open_shifts_closed_by
        foreign key (closed_by_membership_id) references agency_memberships (id),
    constraint chk_open_shifts_status
        check (status in ('OPEN', 'CLAIMED_OR_ASSIGNED', 'CANCELLED', 'EXPIRED'))
);

create index idx_open_shifts_agency_id on open_shifts (agency_id);
create index idx_open_shifts_visit_id on open_shifts (visit_occurrence_id);

create table visit_reschedule_events (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    previous_caregiver_profile_id uuid,
    new_caregiver_profile_id uuid,
    rescheduled_by_membership_id uuid not null,
    previous_planned_start_at timestamp with time zone not null,
    previous_planned_end_at timestamp with time zone not null,
    new_planned_start_at timestamp with time zone not null,
    new_planned_end_at timestamp with time zone not null,
    reason varchar(1000),
    rescheduled_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_visit_reschedule_events_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_visit_reschedule_events_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_visit_reschedule_events_previous_profile
        foreign key (previous_caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_visit_reschedule_events_new_profile
        foreign key (new_caregiver_profile_id) references caregiver_profiles (id),
    constraint fk_visit_reschedule_events_rescheduled_by
        foreign key (rescheduled_by_membership_id) references agency_memberships (id)
);

create index idx_visit_reschedule_events_agency_id on visit_reschedule_events (agency_id);
create index idx_visit_reschedule_events_visit_id on visit_reschedule_events (visit_occurrence_id);

create table visit_cancellation_events (
    id uuid primary key,
    agency_id uuid not null,
    visit_occurrence_id uuid not null,
    cancelled_by_membership_id uuid not null,
    cancelled_at timestamp with time zone not null,
    cancellation_party varchar(32),
    reason varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_visit_cancellation_events_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_visit_cancellation_events_visit
        foreign key (visit_occurrence_id) references visit_occurrences (id),
    constraint fk_visit_cancellation_events_cancelled_by
        foreign key (cancelled_by_membership_id) references agency_memberships (id),
    constraint chk_visit_cancellation_events_party
        check (cancellation_party in ('PATIENT_SIDE', 'CAREGIVER_SIDE', 'ADMIN_SIDE') or cancellation_party is null)
);

create index idx_visit_cancellation_events_agency_id on visit_cancellation_events (agency_id);
create index idx_visit_cancellation_events_visit_id on visit_cancellation_events (visit_occurrence_id);
