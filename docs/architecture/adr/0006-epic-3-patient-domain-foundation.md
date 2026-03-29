# ADR 0006: Epic 3 Patient Domain Foundation

Status

Accepted

Context

Epic 3 introduces patient / client management modules:

- patient demographics
- contacts and emergency contacts
- address and geo-location
- service eligibility
- diagnoses / conditions
- payer linkage
- episode / service authorization tracking
- document attachments

Epic 1 already established shared-schema tenancy, branch-aware authorization, audit infrastructure, secure sessions, and admin shells.
Epic 2 already established reusable agency configuration and catalog models.

Epic 3 needs a shared patient-domain foundation so patient modules do not each invent their own lifecycle, duplicate-handling rules, permission model, or audit behavior.

Decision

Epic 3 patient data will use six approved patient entity categories:

1. `CORE_IDENTITY`
2. `CONTACT_RELATIONSHIP`
3. `ADDRESS_AND_GEO`
4. `OPERATIONAL_QUALIFIER`
5. `FINANCIAL_LINKAGE`
6. `ATTACHMENT_METADATA`

Epic 3 patient lifecycle will use a shared status model:

1. `ACTIVE`
2. `INACTIVE`
3. `ARCHIVED`
4. `DUPLICATE_CANDIDATE`
5. `DUPLICATE_MERGED`

The shared lifecycle rules are:

- `ACTIVE` is the normal usable state
- `INACTIVE` disables future operational use while preserving history
- `ARCHIVED` is a retained terminal state for records that should not re-enter normal use
- `DUPLICATE_CANDIDATE` marks a record that requires review before merge or dismissal
- `DUPLICATE_MERGED` is a retained state for records merged into a canonical patient

Epic 3 authorization will extend the agency permission matrix with patient-specific permissions instead of relying on generic admin checks.

Epic 3 audit behavior will use:

- standardized action types:
  - `PATIENT_RECORD_CREATED`
  - `PATIENT_RECORD_UPDATED`
  - `PATIENT_RECORD_DEACTIVATED`
  - `PATIENT_RECORD_ARCHIVED`
  - `PATIENT_DUPLICATE_FLAGGED`
  - `PATIENT_ATTACHMENT_UPLOADED`
  - `PATIENT_ATTACHMENT_DOWNLOADED`
- explicit Epic 3 target types for each patient domain

Consequences

- later Epic 3 modules can share lifecycle and audit behavior without redefining it
- frontend and audit tooling can rely on consistent patient action and target naming
- attachment handling remains explicitly permission-sensitive
- duplicate review and archival behavior are standardized before higher-risk patient workflows are added
