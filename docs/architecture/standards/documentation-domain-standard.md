# Documentation Domain Standard

This standard defines the shared backend rules for Epic 8 visit-documentation, form, task-completion, and printable-summary entities.

## Documentation categories

Epic 8 entities must fit one of these categories:

- `VISIT_NOTE_TEMPLATE`
- `FORM_TEMPLATE`
- `TASK_LIBRARY_ITEM`
- `TEMPLATE_TASK_INSTANCE`
- `VISIT_DOCUMENTATION_RECORD`
- `NOTE_FIELD_RESPONSE`
- `TASK_COMPLETION_RESPONSE`
- `PRINTABLE_SUMMARY_PROJECTION`

## Shared modeling rules

- Documentation templates and task-library items are agency-owned records with optional branch, visit-type, service-line, and actor-role scoping overlays.
- Visit documentation records must reference Epic 5 visit occurrences rather than inventing parallel visit identities.
- Documentation records must link back to patient, branch, acting membership, and selected template context.
- Structured responses must preserve normalized values for downstream validation and print-safe reuse.
- Required-field and role-based visibility behavior must be enforceable from backend template metadata rather than inferred only in the frontend.
- Printable-summary contracts must minimize PHI exposure and exclude internal-only operational metadata.

## Phase A shared contracts

Phase A establishes these shared contracts:

- documentation record lifecycle states:
  - `DRAFT`
  - `IN_PROGRESS`
  - `SUBMITTED`
  - `AMENDED`
  - `LOCKED`
- supported field types:
  - `TEXT`
  - `LONG_TEXT`
  - `BOOLEAN`
  - `NUMBER`
  - `DATE_TIME`
  - `SELECT_CODED_VALUE`
  - `FREE_TEXT_BLOCK`

## Audit expectations

Epic 8 actions that change template or documentation state must be auditable, including:

- template creation
- template update
- task library update
- documentation draft save
- documentation submission
- documentation amendment
- attachment linkage
- printable summary generation

Audit metadata should carry identifiers, scope, and outcome context without storing broad note bodies or unnecessary PHI.

## Scope boundary

Epic 8 documentation domain does not yet implement:

- generalized messaging expansion from Epic 9
- QA review queue ownership from Epic 10
- broader care-plan progression workflows from later epics
- claims, billing, or export-to-payer workflows
