# Tenant Isolation Guardrails

## Status

Accepted

## Story

`E1-S07 — Enforce tenant isolation in repository/query layer`

## Purpose

This standard defines how tenant-owned repository access must be constrained by `agency_id` so controllers are not the only isolation boundary.

## Repository guardrails

- Tenant-owned repositories run on `TenantAwareJpaRepository`.
- When tenant context is present, inherited `findById(...)` and `existsById(...)` are automatically constrained by `agency_id`.
- Global entities such as `Agency`, `InternalSuperAdmin`, and `AuditEvent` keep normal unscoped repository behavior.
- Tenant-owned repositories must still expose explicit scoped query methods for normal business reads.

Required examples:

- `findByIdAndAgency_Id(...)`
- `existsByIdAndAgency_Id(...)`
- `findByAgency_IdAndCode(...)`

## Service guardrails

- Tenant-aware services must derive tenant scope from `CurrentTenant`.
- Do not trust controller-supplied agency identifiers on authenticated tenant requests.
- Cross-tenant reads should behave as inaccessible records and resolve as not found.
- Cross-tenant writes must fail fast with access denied.

## Allowed exception

Platform-global workflows may use unscoped repository access only when no tenant context is bound and the use case is explicitly global.

Examples:

- agency provisioning
- internal super admin support flows

## Testing rule

- Repository tests must prove guessed IDs from another agency are not returned when tenant context is present.
- Integration tests must prove cross-tenant ID enumeration attempts do not leak records.

## Adoption checklist

- Tenant-owned entity extends `AgencyScopedEntity` or `BranchScopedEntity`.
- Repository includes explicit `agency_id`-scoped query methods.
- Tenant-aware service reads tenant scope from `CurrentTenant`.
- Cross-tenant read and write tests exist.
