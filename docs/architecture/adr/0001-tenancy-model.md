# ADR 0001: Tenancy Model

## Status

Accepted

## Story

`E1-S01 — Define tenancy model`

## Context

The HomeHealthCare platform must support many agencies in one SaaS deployment without cross-tenant data leakage. Before entity implementation starts, the tenancy boundary, branch model, membership model, naming rules, and ID standards must be explicit.

This ADR is the approval record for the tenancy model to be used by all subsequent schema, backend, and UI work in Epic 1.

## Decisions

### 1. Tenant definition

The platform tenant is an `Agency`.

An `Agency` is the top-level customer container for:

- data isolation
- configuration ownership
- billing ownership
- user membership
- branch hierarchy

Every tenant-owned business record must carry `agency_id`.

### 2. Branch definition

A `Branch` is a child of one `Agency`.

A branch:

- cannot exist without an agency
- belongs to exactly one agency
- is never shared across agencies
- represents an operational location or operating unit

`Branch` is not a tenant. It is an internal scope boundary within a tenant.

### 3. User membership model

User access to an agency is granted through `AgencyMembership`.

Rules:

- a `User` may exist without agency access
- a `User` may have zero or more agency memberships
- each membership belongs to exactly one agency
- roles are assigned at the agency membership level
- users do not gain agency access directly from branch assignment

This keeps identity separate from tenant access and allows future support for one user participating in multiple agencies.

### 4. Branch assignment model

Branch visibility is granted through `BranchAssignment`.

Rules:

- branch assignment is separate from agency membership
- a branch assignment requires an active agency membership in the same agency
- a user may be assigned to zero, one, or many branches in that agency
- branch-restricted roles only see assigned branches
- agency-wide roles may see all branches without explicit branch assignments when policy allows

This preserves a clean distinction between:

- "can access agency"
- "can access which branches inside that agency"

### 5. Isolation model

The initial tenancy strategy is:

- shared database
- shared schema
- strict row-level application enforcement using `agency_id`

Isolation rules:

- all tenant-owned records must include `agency_id`
- branch-scoped records must include both `agency_id` and `branch_id`
- `branch_id` never replaces `agency_id`
- backend enforcement is mandatory; frontend filtering is not sufficient

### 6. Internal super admin model

`InternalSuperAdmin` exists outside tenant membership.

Rules:

- internal platform operators are not represented as agency members by default
- internal actions are explicitly audited
- internal access paths must still pass through auditable service logic

This is included here because it affects the tenancy boundary.

## Canonical entities for Epic 1 foundation

### Agency

Top-level tenant record.

### Branch

Operational child of an agency.

### User

Global identity record for authentication and profile data.

### AgencyMembership

Join record that grants a user access to an agency and stores agency-level role data.

### BranchAssignment

Join record that grants a user access to a branch within an agency.

## Naming conventions

These conventions are finalized for the project.

### Domain names

- Tenant = `Agency`
- Branch = `Branch`
- Membership = `AgencyMembership`
- Branch assignment = `BranchAssignment`
- Internal admin = `InternalSuperAdmin`

Avoid generic names such as:

- `Tenant`
- `Organization`
- `Office`
- `LocationMembership`

Those terms may appear in architecture explanations, but not as canonical domain entity names unless a future ADR changes the model.

### Table names

Use plural snake_case table names:

- `agencies`
- `branches`
- `users`
- `agency_memberships`
- `branch_assignments`

### Primary key columns

All primary keys use:

- `id`

### Foreign key columns

Use explicit foreign key names:

- `agency_id`
- `branch_id`
- `user_id`
- `agency_membership_id`

### Timestamp columns

Use:

- `created_at`
- `updated_at`

Soft-delete or lifecycle timestamps should use explicit names such as:

- `deactivated_at`
- `suspended_at`
- `deleted_at`

### Status columns

Use a single `status` column for lifecycle state where applicable.

Status values should be lowercase snake_case enum values.

### Unique external identifiers

Human-visible identifiers should be explicit and separate from primary keys, for example:

- `slug` for agency URL-safe identity
- `code` for branch short code

## ID standard

The ID strategy is finalized as:

- primary keys use `UUID`
- application-generated UUID version should be `UUIDv7` when supported by the chosen stack
- IDs are opaque and never encode agency or branch meaning

Rationale:

- safe for distributed creation
- avoids sequential enumeration
- consistent across services and clients
- easier future integration than integer sequences

Implementation note:

If the chosen Java stack lacks first-class UUIDv7 support in the first migration pass, UUIDv4 may be used temporarily behind the same `UUID` column type, but the architecture target remains UUIDv7.

## Ownership rules for future entities

Future entities must be classified into one of these ownership types.

### Type A: Agency-owned

Entity belongs to an agency and does not require branch scope.

Examples:

- service lines
- payer configuration
- task templates
- agency-wide roles or policies

Standard:

- must include `agency_id`
- must not include `branch_id` unless branch scope is real

### Type B: Branch-scoped

Entity belongs to an agency and is operationally tied to one branch.

Examples:

- branch schedules
- branch alerts
- branch staffing artifacts

Standard:

- must include `agency_id`
- must include `branch_id`
- `branch_id` must reference a branch in the same agency

### Type C: Global platform

Entity is not tenant-owned.

Examples:

- internal super admin accounts
- platform feature flags
- global audit categories

Standard:

- must not carry `agency_id` unless the record itself becomes tenant-scoped

## Guardrails

- Never infer agency membership from branch assignment.
- Never model branch as a second tenant layer.
- Never store branch-owned records with `branch_id` only.
- Never allow a user to operate in an agency without `AgencyMembership`.
- Never bypass audit logging for internal admin actions.

## ERD

The source ERD for this decision is stored at:

`docs/architecture/erd/e1-s01-tenancy-model.mmd`

## Acceptance criteria mapping

- Tenant is defined as Agency: approved by Decision 1.
- Branch is defined as child of Agency: approved by Decision 2.
- User membership is defined at Agency level: approved by Decision 3.
- Branch assignments are defined separately: approved by Decision 4.
- ERD is updated and approved: the ERD source was created with this accepted ADR.
- Naming conventions and IDs are finalized: finalized in the naming and ID sections above.

## Consequences

Positive:

- gives engineering a stable base for Epic 1
- prevents ambiguous branch-vs-tenant modeling
- supports future multi-agency users without redesign
- makes branch restrictions explicit and testable

Tradeoff:

- some tables will carry both `agency_id` and `branch_id`, which is intentionally redundant for stronger isolation and easier query enforcement
