# Entity Scoping Standard

## Status

Accepted

## Story

`E1-S04 — Add tenant and branch scoping fields to all core future entities`

## Purpose

This standard defines how all future tables and JPA entities must carry tenant scope.

It exists to make tenant isolation repeatable instead of relying on ad hoc developer choices.

## Core rules

### 1. Agency-owned entities

If an entity belongs to a tenant, it must include `agency_id`.

Use this pattern for:

- agency configuration
- patient records
- caregivers
- service lines
- task templates
- payer configuration
- memberships

Standard:

- table must contain `agency_id uuid not null`
- table must have a foreign key to `agencies(id)`
- JPA entity should extend `AgencyScopedEntity` when practical
- repository/service queries must filter by `agency_id`

### 2. Branch-scoped entities

If an entity is operationally tied to a single branch, it must include both `agency_id` and `branch_id`.

Use this pattern for:

- visits
- schedules
- branch alerts
- branch-specific staffing data
- branch-level dashboards

Standard:

- table must contain `agency_id uuid not null`
- table must contain `branch_id uuid not null`
- `branch_id` does not replace `agency_id`
- JPA entity should extend `BranchScopedEntity` when practical
- services must validate that the chosen branch belongs to the same agency

### 3. Global platform entities

If an entity is not tenant-owned, it must not include `agency_id` or `branch_id` unless a later ADR changes its scope.

Use this pattern for:

- internal super admin accounts
- platform configuration
- global feature flags

## Code standard

### Agency-scoped entity base class

Use [AgencyScopedEntity](/Users/shiva/Documents/Java%20Projects/HomeHealthCare/src/main/java/com/homehealthcare/shared/persistence/AgencyScopedEntity.java) for tenant-owned JPA models.

Expectations:

- `agency` relation maps to `agency_id`
- `getAgencyId()` is available for service and test use
- constructors/factories must assign agency immediately

### Branch-scoped entity base class

Use [BranchScopedEntity](/Users/shiva/Documents/Java%20Projects/HomeHealthCare/src/main/java/com/homehealthcare/shared/persistence/BranchScopedEntity.java) for entities that belong to both an agency and a branch.

Expectations:

- `branch` relation maps to `branch_id`
- branch assignment must also set the inherited agency
- `getBranchId()` and `getAgencyId()` must both resolve

### Query standard

Repository methods must scope by relation path, not guessed IDs in application memory.

Examples:

- `existsByAgency_IdAndCode(...)`
- `findByAgency_IdAndStatus(...)`
- `findByAgency_IdAndBranch_Id(...)`

Do not use unscoped lookups for tenant-owned records unless the caller is a platform-global workflow.

## Schema standard

### Agency-owned table template

```sql
create table example_agency_owned (
    id uuid primary key,
    agency_id uuid not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_example_agency_owned_agency
        foreign key (agency_id) references agencies (id)
);

create index idx_example_agency_owned_agency_id
    on example_agency_owned (agency_id);
```

### Branch-scoped table template

```sql
create table example_branch_scoped (
    id uuid primary key,
    agency_id uuid not null,
    branch_id uuid not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_example_branch_scoped_agency
        foreign key (agency_id) references agencies (id),
    constraint fk_example_branch_scoped_branch
        foreign key (branch_id) references branches (id)
);

create index idx_example_branch_scoped_agency_id
    on example_branch_scoped (agency_id);

create index idx_example_branch_scoped_branch_id
    on example_branch_scoped (branch_id);
```

### Uniqueness standard

When uniqueness is tenant-local, include `agency_id` in the constraint.

Examples:

- `unique (agency_id, code)`
- `unique (agency_id, name)`

When uniqueness is branch-local, include the narrowest real scope.

Examples:

- `unique (branch_id, start_time, caregiver_id)`
- `unique (agency_id, branch_id, external_reference)`

## Validation standard

For branch-scoped records:

- validate the branch exists
- validate the branch belongs to the same agency
- reject writes when agency and branch do not match

For agency-owned records:

- reject writes without agency context
- reject unscoped repository access from tenant-aware services

## Acceptance criteria mapping

- `agency_id` pattern is defined for all tenant-owned domain entities: covered by Core rule 1, code standard, and schema standard.
- `branch_id` pattern is defined for branch-owned or branch-scoped entities: covered by Core rule 2, code standard, and schema standard.
- engineering standard document exists for all new tables/entities: this document is that standard.
- migration pattern is documented: documented in the schema standard and companion migration guideline.
