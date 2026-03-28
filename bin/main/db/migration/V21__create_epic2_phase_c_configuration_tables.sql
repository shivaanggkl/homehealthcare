create table task_templates (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    service_line_id uuid references service_lines (id),
    visit_type_id uuid references visit_types (id),
    name varchar(200) not null,
    code varchar(50) not null,
    description varchar(1000),
    category varchar(64) not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_task_templates_agency_name on task_templates (agency_id, name);
create unique index ux_task_templates_agency_code on task_templates (agency_id, code);

create table documentation_templates (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    name varchar(200) not null,
    code varchar(50) not null,
    template_type varchar(64) not null,
    structured_definition_json clob not null,
    version integer not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_documentation_templates_agency_name on documentation_templates (agency_id, name);
create unique index ux_documentation_templates_agency_code on documentation_templates (agency_id, code);

create table branch_policies (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid not null references branches (id),
    policy_key varchar(100) not null,
    settings_payload_json clob,
    fallback_to_agency_default boolean not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_branch_policies_branch_key on branch_policies (branch_id, policy_key);

create table alert_rules (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    name varchar(200) not null,
    rule_type varchar(64) not null,
    config_payload_json clob not null,
    notify_email boolean not null,
    notify_sms boolean not null,
    notify_in_app boolean not null,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_alert_rules_scope_name on alert_rules (agency_id, branch_id, name);

create table mileage_pay_settings (
    id uuid primary key,
    agency_id uuid not null references agencies (id),
    branch_id uuid references branches (id),
    reimbursement_strategy varchar(64) not null,
    mileage_rate decimal(10,4) not null,
    travel_pay_enabled boolean not null,
    visit_type_pay_adjustments_json clob,
    status varchar(32) not null,
    display_order integer not null default 0,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index ux_mileage_pay_settings_scope on mileage_pay_settings (agency_id, branch_id);
