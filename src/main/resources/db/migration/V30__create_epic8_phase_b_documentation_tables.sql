alter table task_templates
    add column default_sort_order integer not null default 0;
alter table task_templates
    add column default_completion_expectation varchar(255);
alter table task_templates
    add column required_by_default boolean not null default false;

alter table documentation_templates
    add column service_line_id uuid references service_lines (id);
alter table documentation_templates
    add column visit_type_id uuid references visit_types (id);
alter table documentation_templates
    add column branch_id uuid references branches (id);
alter table documentation_templates
    add column help_text varchar(1000);
alter table documentation_templates
    add column allowed_actor_roles varchar(500);
alter table documentation_templates
    add column requires_signature_verification boolean not null default false;

create table documentation_template_sections (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    documentation_template_id uuid not null references documentation_templates (id),
    section_key varchar(100) not null,
    title varchar(200) not null,
    help_text varchar(1000),
    sort_order integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_documentation_template_sections_template_key
    on documentation_template_sections (documentation_template_id, section_key);

create table documentation_template_fields (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    documentation_template_id uuid not null references documentation_templates (id),
    section_id uuid references documentation_template_sections (id),
    field_key varchar(100) not null,
    label varchar(200) not null,
    field_type varchar(32) not null,
    required_field boolean not null,
    sort_order integer not null default 0,
    options_json clob,
    help_text varchar(1000),
    visible_actor_roles varchar(500),
    editable_actor_roles varchar(500),
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_documentation_template_fields_template_key
    on documentation_template_fields (documentation_template_id, field_key);

create table documentation_template_tasks (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    documentation_template_id uuid not null references documentation_templates (id),
    section_id uuid references documentation_template_sections (id),
    task_template_id uuid references task_templates (id),
    title_override varchar(200),
    description_override varchar(1000),
    required_override boolean,
    sort_order integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table visit_documentation_records (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    visit_occurrence_id uuid not null references visit_occurrences (id),
    patient_id uuid not null references patients (id),
    branch_id uuid references branches (id),
    selected_template_id uuid not null references documentation_templates (id),
    author_membership_id uuid not null references agency_memberships (id),
    last_editor_membership_id uuid not null references agency_memberships (id),
    status varchar(32) not null,
    started_at timestamp with time zone,
    submitted_at timestamp with time zone,
    last_saved_at timestamp with time zone not null,
    printable_summary_version integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_visit_documentation_records_visit_template
    on visit_documentation_records (visit_occurrence_id, selected_template_id);

create table documentation_field_responses (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    documentation_record_id uuid not null references visit_documentation_records (id),
    template_field_id uuid not null references documentation_template_fields (id),
    field_key varchar(100) not null,
    normalized_value clob,
    display_value varchar(1000),
    response_notes varchar(1000),
    completion_state varchar(32) not null,
    completed_at timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_documentation_field_responses_record_field
    on documentation_field_responses (documentation_record_id, template_field_id);

create table documentation_task_responses (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    documentation_record_id uuid not null references visit_documentation_records (id),
    template_task_id uuid not null references documentation_template_tasks (id),
    task_template_id uuid references task_templates (id),
    task_title varchar(200) not null,
    task_description varchar(1000),
    completion_required boolean not null,
    completion_state varchar(32) not null,
    completion_notes varchar(1000),
    completed_at timestamp with time zone,
    sort_order integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_documentation_task_responses_record_task
    on documentation_task_responses (documentation_record_id, template_task_id);

create table documentation_attachment_links (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    documentation_record_id uuid not null references visit_documentation_records (id),
    patient_attachment_id uuid references patient_attachments (id),
    mobile_artifact_id uuid references mobile_field_artifacts (id),
    linked_by_membership_id uuid not null references agency_memberships (id),
    caption varchar(255),
    description varchar(1000),
    linked_at timestamp with time zone not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_documentation_attachment_links_record
    on documentation_attachment_links (documentation_record_id);
