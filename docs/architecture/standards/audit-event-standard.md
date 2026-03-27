# Audit Event Standard

## Status

Accepted

## Story

`E1-S37 — Create audit event model`

## Unified event schema

- All audit rows use the `audit_events` table.
- Required event columns are:
  - `actor_type`
  - `actor_id`
  - `actor_email`
  - `action_type`
  - `target_type`
  - `target_id`
  - `outcome`
  - `occurred_at`
  - `metadata_json`
- Tenant and scope columns are:
  - `agency_id`
  - `branch_id`

## Outcome rules

- `outcome` must be `SUCCESS` or `FAILURE`.
- Success events record completed security-relevant or data-changing actions.
- Failure events record denied, blocked, or unsuccessful critical actions when the actor and target are known.

## Storage approach

- Audit events are stored in the primary relational database for transactional consistency with application writes.
- `metadata_json` carries structured context that does not justify a first-class column.
- Events are append-only. Application code must not update or delete individual audit rows.
- Indexes must support investigation by actor, agency, branch, outcome, and event time.

## Retention approach

- Audit events must be retained for at least 7 years.
- Production operations may archive older rows to colder storage after 24 months of hot-database retention, but the archive must preserve the full event payload.
- Purge operations must be controlled, documented, and executed only after the retention window expires.

## Engineering guardrails

- Use the shared `AuditEvent` factory methods instead of constructing rows ad hoc.
- Do not place secrets, raw tokens, password material, MFA seeds, or recovery codes in `metadata_json`.
- Prefer concrete action names such as `USER_LOGGED_IN` or `USER_LOGIN_FAILED`.
- Include `branch_id` whenever the audited action is branch-scoped.
