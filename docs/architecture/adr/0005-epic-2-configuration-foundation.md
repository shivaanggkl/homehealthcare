# ADR 0005: Epic 2 Configuration Foundation

Status

Accepted

Context

Epic 2 introduces agency setup and configuration modules:

- agency profile
- service lines
- visit types
- caregiver skills and certifications
- task templates
- documentation templates
- branch policies
- alert rules
- mileage and pay settings

Epic 1 already established shared-schema tenancy, branch-aware authorization, public audit events, and the admin web shell.
Epic 2 needs a shared foundation so these configuration modules do not each invent their own lifecycle, scope, audit behavior, or permission model.

Decision

Epic 2 configuration will use three approved configuration categories:

1. `AGENCY_MASTER_DATA`
2. `BRANCH_OVERRIDE_POLICY`
3. `TEMPLATE_CATALOG`

Epic 2 configuration lifecycles will use a shared status model:

1. `DRAFT`
2. `ACTIVE`
3. `INACTIVE`
4. `ARCHIVED`

The shared lifecycle rules are:

- `DRAFT` is allowed for template-oriented records and future unpublished changes
- `ACTIVE` is the normal usable state
- `INACTIVE` disables future use while preserving history and references
- `ARCHIVED` is a terminal retained state for records that should not re-enter normal use

Where a configuration record must support time-bounded behavior, the shared effective-window pattern is:

- `effective_from`
- `effective_to`

Epic 2 authorization will extend the agency permission matrix with configuration-specific permissions instead of relying on generic admin checks.

Epic 2 audit behavior will use:

- standardized action types:
  - `CONFIGURATION_CREATED`
  - `CONFIGURATION_UPDATED`
  - `CONFIGURATION_DEACTIVATED`
  - `CONFIGURATION_PUBLISHED`
- explicit Epic 2 target types for each configuration domain

Consequences

- later Epic 2 modules can share lifecycle and audit behavior without redefining it
- frontend and audit tooling can rely on consistent action and target naming
- branch policy and branch-scoped alert modules keep branch-aware permission semantics from day one
- destructive behavior can be standardized around deactivate/archive instead of hard delete
