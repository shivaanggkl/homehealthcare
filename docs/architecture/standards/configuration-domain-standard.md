# Epic 2 Configuration Domain Standard

Purpose

This standard defines the shared modeling rules for Epic 2 agency setup and configuration modules.

Applies to

- agency profile extensions
- service lines
- visit types
- caregiver skills
- caregiver certifications
- task templates
- documentation templates
- branch policies
- alert rules
- mileage and pay settings

Approved configuration categories

1. `AGENCY_MASTER_DATA`
   Use for agency-scoped records such as service lines, visit types, skills, certifications, and compensation defaults.
2. `BRANCH_OVERRIDE_POLICY`
   Use for branch-level policy records where a branch override is a real business concept.
3. `TEMPLATE_CATALOG`
   Use for reusable definitions such as task templates and documentation templates.

Shared modeling rules

- all Epic 2 records remain tenant-owned and must be agency-scoped at minimum
- branch-scoped records must use the existing branch-aware security model from Epic 1
- records should prefer soft lifecycle transitions over destructive deletion
- display-order support should be included where the UI will render an ordered catalog
- code/name uniqueness must be enforced within agency for human-managed catalogs
- configuration records intended for future-dated behavior should support an effective window

Recommended shared fields

- `id`
- `agency_id`
- `status`
- `display_order`
- `effective_from`
- `effective_to`
- `created_at`
- `updated_at`

Recommended optional fields

- `code`
- `description`
- `metadata_json`
- `version`

Audit rules

- all create, update, deactivate, and publish actions are audit-sensitive
- target types must identify the configuration domain clearly
- audit metadata should record enough business context for support and compliance review

Deletion rules

- default behavior is deactivate or archive, not hard delete
- hard delete is allowed only for records with no references and only when explicitly approved
- when dependencies exist, APIs should return a controlled conflict response
