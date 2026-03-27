# HomeHealthCare Documentation

## Architecture

- `docs/architecture/adr/0001-tenancy-model.md` defines the approved multi-tenant model for `E1-S01`.
- `docs/architecture/adr/0002-internal-super-admin-model.md` defines the approved internal operator model for `E1-S05`.
- `docs/architecture/adr/0003-role-permission-matrix.md` records the approved RBAC model for `E1-S27`.
- `docs/architecture/adr/0004-secure-session-token-strategy.md` records the approved auth session model for `E1-S32`.
- `docs/architecture/erd/e1-s01-tenancy-model.mmd` is the source ERD for the tenancy foundation.
- `docs/architecture/standards/entity-scoping-standard.md` defines `agency_id` and `branch_id` standards for future entities.
- `docs/architecture/standards/migration-pattern.md` defines the migration checklist for tenant-aware tables.
- `docs/architecture/standards/branch-scoping-guardrails.md` defines branch visibility rules for `E1-S08`.
- `docs/architecture/standards/role-permission-matrix.md` defines CRUD permissions and scope rules for tenant roles.
- `docs/architecture/standards/session-security-standard.md` defines cookie, refresh, and expiration rules for authenticated web sessions.
- `docs/architecture/standards/mfa-admin-guardrails.md` defines admin MFA visibility and enforcement rules for `E1-S26`.
- `docs/architecture/standards/tenant-isolation-guardrails.md` defines repository and service isolation rules for `E1-S07`.

These files are the source of truth for Epic 1 design decisions until implementation starts.
