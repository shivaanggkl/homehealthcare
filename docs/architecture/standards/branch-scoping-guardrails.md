# Branch Scoping Guardrails

## Status

Accepted

## Story

`E1-S08 — Enforce branch scoping rules`

## Purpose

This standard defines how branch visibility must be resolved inside an agency once tenant context has already been established.

## Access rules

- Branch assignment is separate from agency membership.
- A user may be assigned to zero, one, or many branches in the current agency.
- Branch-scoped roles only see assigned branches.
- `Agency Owner` has agency-wide branch visibility without explicit branch assignments.
- Cross-branch access should resolve as inaccessible, not merely hidden in the UI.

## Backend contract

- Authenticated principals that participate in branch scoping implement `BranchAccessPrincipal`.
- Branch scope is expressed as:
  - current agency
  - agency role
  - assigned branch IDs
- Service-layer branch checks must use `CurrentBranchAccess`.
- Agency scoping still runs first. Branch scoping never bypasses tenant isolation.

## Repository and service pattern

- Repositories continue filtering by `agency_id`.
- Services apply branch visibility on top of the agency-scoped result.
- For branch lists:
  - agency-wide roles may load all agency branches
  - branch-scoped roles load only assigned branch IDs
- For branch detail access:
  - return not found when the branch is outside the current tenant
  - return not found when the branch is in-tenant but not assigned to the current user

## Testing rule

- API tests must prove multi-branch assignment works.
- API tests must prove branch-scoped roles do not see unassigned branches.
- API tests must prove `Agency Owner` can see all branches in the agency.
