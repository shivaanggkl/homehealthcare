# Migration Pattern

## Purpose

This document defines how schema migrations must introduce tenant and branch scoping.

## Versioning

- use sequential Flyway versions: `V1__...`, `V2__...`, `V3__...`
- one migration should represent one cohesive schema change
- do not bundle unrelated module work into the same migration

## Required pattern for new tenant-owned tables

1. Create the table with `id`, `agency_id`, audit timestamps, and domain columns.
2. Add a named foreign key to `agencies(id)`.
3. Add tenant-local unique constraints where needed.
4. Add an index on `agency_id`.
5. Add any lifecycle check constraints such as `status`.

## Required pattern for new branch-scoped tables

1. Create the table with `id`, `agency_id`, `branch_id`, audit timestamps, and domain columns.
2. Add a named foreign key to `agencies(id)`.
3. Add a named foreign key to `branches(id)`.
4. Add indexes on both `agency_id` and `branch_id`.
5. Add scoped unique constraints using the actual ownership boundary.

## Constraint naming

Use explicit names:

- foreign keys: `fk_<table>_<target>`
- unique keys: `uk_<table>_<scope>_<field>`
- indexes: `idx_<table>_<column>`
- checks: `chk_<table>_<rule>`

## Backfill rule for future migrations

If a pre-existing table gains tenant scope later:

1. add nullable `agency_id` or `branch_id`
2. backfill from authoritative parent records
3. validate null-free state
4. add `not null`
5. add foreign keys and indexes
6. add scoped unique constraints last

Do not add a non-null scoped column before data backfill exists.

## Review checklist

Before a migration is merged, verify:

- correct ownership type was chosen
- `agency_id` exists on every tenant-owned table
- `branch_id` exists only when branch scope is real
- uniqueness is scoped correctly
- indexes exist on scoping keys
- foreign key names are explicit
