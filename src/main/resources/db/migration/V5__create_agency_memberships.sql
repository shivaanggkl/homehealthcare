create table agency_memberships (
    id uuid primary key,
    user_id uuid not null,
    agency_id uuid not null,
    role_key varchar(64) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_agency_memberships_user
        foreign key (user_id) references users (id),
    constraint fk_agency_memberships_agency
        foreign key (agency_id) references agencies (id),
    constraint uk_agency_memberships_user_agency unique (user_id, agency_id),
    constraint chk_agency_memberships_status
        check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_agency_memberships_role_key
        check (role_key in (
            'AGENCY_OWNER',
            'BRANCH_ADMIN',
            'SCHEDULER_COORDINATOR',
            'CAREGIVER',
            'QA_CLINICAL_REVIEWER',
            'BILLING_BACK_OFFICE',
            'READ_ONLY_AUDITOR'
        ))
);

create index idx_agency_memberships_user_id
    on agency_memberships (user_id);

create index idx_agency_memberships_agency_id
    on agency_memberships (agency_id);
