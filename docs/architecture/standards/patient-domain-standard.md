# Epic 3 Patient Domain Standard

Purpose

This standard defines the shared modeling rules for Epic 3 patient / client management modules.

Applies to

- patient demographics
- contacts and emergency contacts
- address and geo-location
- service eligibility
- diagnoses / conditions
- payer linkage
- episode / service authorization tracking
- document attachments

Approved patient entity categories

1. `CORE_IDENTITY`
   Use for the patient master record and stable identity or demographic fields.
2. `CONTACT_RELATIONSHIP`
   Use for family, responsible-party, and emergency-contact records.
3. `ADDRESS_AND_GEO`
   Use for patient service locations and optional geocode metadata.
4. `OPERATIONAL_QUALIFIER`
   Use for service eligibility, diagnoses, and similar qualifiers used by later workflows.
5. `FINANCIAL_LINKAGE`
   Use for payer links, authorization windows, and related financial-service context.
6. `ATTACHMENT_METADATA`
   Use for patient document metadata and storage references.

Shared modeling rules

- all Epic 3 patient records remain tenant-owned and must be agency-scoped at minimum
- patient modules should prefer soft lifecycle transitions over destructive deletion
- records with future scheduling, QA, or revenue impact should preserve history rather than disappear
- attachments must separate business metadata from raw file content storage
- duplicate handling must be explicit and auditable

Recommended shared fields

- `id`
- `agency_id`
- `status`
- `created_at`
- `updated_at`

Recommended optional fields

- `external_reference`
- `effective_from`
- `effective_to`
- `notes`
- `metadata_json`

Audit rules

- all create, update, deactivate, archive, duplicate-review, and attachment actions are audit-sensitive
- target types must identify the patient domain clearly
- audit metadata should record enough business context for support and compliance review

Deletion rules

- default behavior is deactivate or archive, not hard delete
- hard delete is allowed only for records with no attachments, no authorizations, and no downstream references, and only when explicitly approved
- when dependencies exist, APIs should return a controlled conflict response

Extension rules

- allergies and medications are later-phase and should not reshape the Epic 3 base model prematurely
- future care-plan, scheduling, and visit-execution modules should reference Epic 3 patient identity rather than copy patient fields
