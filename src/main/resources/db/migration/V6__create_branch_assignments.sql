create table branch_assignments (
    id uuid primary key,
    agency_id uuid not null,
    agency_membership_id uuid not null,
    branch_id uuid not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_branch_assignments_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_branch_assignments_agency_membership
        foreign key (agency_membership_id) references agency_memberships (id),
    constraint fk_branch_assignments_branch
        foreign key (branch_id) references branches (id),
    constraint uk_branch_assignments_membership_branch unique (agency_membership_id, branch_id),
    constraint chk_branch_assignments_status
        check (status in ('ACTIVE', 'INACTIVE'))
);

create index idx_branch_assignments_agency_id
    on branch_assignments (agency_id);

create index idx_branch_assignments_agency_membership_id
    on branch_assignments (agency_membership_id);

create index idx_branch_assignments_branch_id
    on branch_assignments (branch_id);
