create table communication_threads (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    thread_type varchar(32) not null,
    subject varchar(200),
    status varchar(32) not null,
    created_by_membership_id uuid not null references agency_memberships (id),
    branch_id uuid references branches (id),
    patient_id uuid references patients (id),
    visit_occurrence_id uuid references visit_occurrences (id),
    escalation_status varchar(32) not null,
    last_message_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table thread_participants (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    thread_id uuid not null references communication_threads (id),
    membership_id uuid not null references agency_memberships (id),
    participant_role varchar(100),
    added_at timestamp with time zone not null,
    removed_at timestamp with time zone,
    muted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_thread_participants_active_unique
    on thread_participants (thread_id, membership_id, removed_at);

create table communication_messages (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    thread_id uuid not null references communication_threads (id),
    sender_membership_id uuid not null references agency_memberships (id),
    message_body varchar(4000) not null,
    created_at_at_source timestamp with time zone not null,
    edited_at timestamp with time zone,
    message_type varchar(32) not null,
    attachment_reference varchar(255),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table message_read_receipts (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    message_id uuid not null references communication_messages (id),
    recipient_membership_id uuid not null references agency_memberships (id),
    read_at timestamp with time zone,
    delivery_state varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_message_read_receipts_message_recipient
    on message_read_receipts (message_id, recipient_membership_id);

create table communication_context_links (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    thread_id uuid not null references communication_threads (id),
    context_type varchar(32) not null,
    context_id uuid not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_communication_context_links_context
    on communication_context_links (context_type, context_id);

create table staff_groups (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    name varchar(200) not null,
    description varchar(1000),
    branch_id uuid references branches (id),
    active boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table staff_group_members (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    staff_group_id uuid not null references staff_groups (id),
    membership_id uuid not null references agency_memberships (id),
    added_at timestamp with time zone not null,
    removed_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_staff_group_members_active_unique
    on staff_group_members (staff_group_id, membership_id, removed_at);

create table branch_broadcasts (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid not null references branches (id),
    eligible_roles_csv varchar(500),
    subject varchar(200) not null,
    body varchar(4000) not null,
    created_by_membership_id uuid not null references agency_memberships (id),
    thread_id uuid not null references communication_threads (id),
    expires_at timestamp with time zone,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table escalation_markers (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    thread_id uuid not null references communication_threads (id),
    status varchar(32) not null,
    tag varchar(100) not null,
    reason varchar(1000),
    tagged_by_membership_id uuid not null references agency_memberships (id),
    tagged_at timestamp with time zone not null,
    cleared_by_membership_id uuid references agency_memberships (id),
    cleared_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);
