# Role Permission Matrix

## Status

Accepted

## Story

`E1-S27 — Define role-permission matrix`

## Purpose

This standard defines the initial authorization matrix for tenant users. It exists so module teams can implement consistent access control and write predictable tests.

## Scope legend

- `A` = agency-wide within the current tenant
- `B` = branch-scoped to assigned branches
- `S` = self only
- `-` = no permission

## Module areas

The initial module areas for CRUD-style authorization are:

- agency configuration
- branch management
- user directory and membership administration
- branch assignments
- scheduling and coordination
- caregiver operations
- QA and clinical review
- billing operations
- reporting and read-only audit
- authentication and account security

## CRUD matrix

| Module area | Capability | Agency Owner | Branch Admin | Scheduler Coordinator | Caregiver | QA Clinical Reviewer | Billing Back Office | Read Only Auditor |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Agency configuration | Create | A | - | - | - | - | - | - |
| Agency configuration | Read | A | A | - | - | - | - | - |
| Agency configuration | Update | A | - | - | - | - | - | - |
| Agency configuration | Delete / deactivate | A | - | - | - | - | - | - |
| Branch management | Create | A | A | - | - | - | - | - |
| Branch management | Read | A | A | B | B | B | B | A |
| Branch management | Update | A | A | - | - | - | - | - |
| Branch management | Delete / deactivate | A | - | - | - | - | - | - |
| User directory and membership administration | Create / invite | A | A | - | - | - | - | - |
| User directory and membership administration | Read | A | A | - | S | - | - | A |
| User directory and membership administration | Update role / status | A | B | - | - | - | - | - |
| User directory and membership administration | Delete / deactivate | A | - | - | - | - | - | - |
| Branch assignments | Create | A | A | - | - | - | - | - |
| Branch assignments | Read | A | A | - | S | - | - | A |
| Branch assignments | Update | A | A | - | - | - | - | - |
| Branch assignments | Delete / deactivate | A | A | - | - | - | - | - |
| Scheduling and coordination | Create | A | B | B | - | - | - | - |
| Scheduling and coordination | Read | A | B | B | B | B | - | A |
| Scheduling and coordination | Update | A | B | B | - | - | - | - |
| Scheduling and coordination | Delete / cancel | A | B | B | - | - | - | - |
| Caregiver operations | Create clinical work artifacts | A | B | - | B | - | - | - |
| Caregiver operations | Read assigned / branch work | A | B | B | B | B | - | A |
| Caregiver operations | Update own branch work | A | B | - | B | - | - | - |
| Caregiver operations | Delete / void | A | B | - | - | - | - | - |
| QA and clinical review | Create review artifacts | A | B | - | - | B | - | - |
| QA and clinical review | Read | A | B | - | - | B | - | A |
| QA and clinical review | Update | A | B | - | - | B | - | - |
| QA and clinical review | Delete / close out | A | - | - | - | B | - | - |
| Billing operations | Create | A | - | - | - | - | A | - |
| Billing operations | Read | A | - | - | - | - | A | A |
| Billing operations | Update | A | - | - | - | - | A | - |
| Billing operations | Delete / reverse | A | - | - | - | - | - | - |
| Reporting and read-only audit | Create exports | A | A | B | - | B | A | A |
| Reporting and read-only audit | Read | A | A | B | S | B | A | A |
| Reporting and read-only audit | Update | - | - | - | - | - | - | - |
| Reporting and read-only audit | Delete | - | - | - | - | - | - | - |
| Authentication and account security | Create enrollment / reset artifacts | S | S | S | S | S | S | S |
| Authentication and account security | Read own status | S | S | S | S | S | S | S |
| Authentication and account security | Update own credentials / MFA | S | S | S | S | S | S | S |
| Authentication and account security | Admin update enforcement policy | A | A | - | - | - | - | - |

## Interpretation rules

### Agency Owner

- Has full tenant-administrative access across agency configuration, branches, users, security, and business operations.
- Is the only tenant role that may deactivate the agency or permanently remove high-risk administrative objects.
- Has agency-wide branch visibility by role design.

### Branch Admin

- Administers operational data and staff assignments inside permitted branches.
- May manage user and branch assignments only for users inside the same tenant and within branch-admin guardrails.
- Does not gain agency-owner-only governance powers such as agency deactivation.

### Scheduler Coordinator

- Operates scheduling and coordination workflows inside assigned branches.
- Does not manage agency settings, role assignment, MFA enforcement, or billing.

### Caregiver

- Performs direct operational work only for assigned branches and self-service account operations.
- Cannot administer users, branches, or tenant security policy.

### QA Clinical Reviewer

- Reads and updates review workflows for assigned branches.
- Does not administer staffing, agency configuration, or billing.

### Billing Back Office

- Manages billing workflows at agency scope.
- Does not manage staffing roles, branch configuration, or clinical review policy.

### Read Only Auditor

- Has read-only access to permitted reporting and directory surfaces.
- Never receives create, update, or delete capabilities.
- May read across the full agency for audit workflows, but cannot access secrets.

## Branch-scoped versus agency-wide rules

- `Agency Owner` is always agency-wide for tenant module access unless a later ADR narrows a specific module.
- `Branch Admin` is branch-scoped for operational data and may be agency-scoped only where the product explicitly requires tenant administration, such as directory visibility or MFA enforcement configuration.
- `Scheduler Coordinator`, `Caregiver`, and `QA Clinical Reviewer` are branch-scoped for business data.
- `Billing Back Office` is agency-scoped for billing records.
- `Read Only Auditor` is agency-scoped for read-only reporting and audit views.
- Self-service authentication actions remain `S` regardless of agency role.

## Guardrails for implementation

- Controllers must not encode raw role-name switches when a reusable authorization policy service can express the same rule.
- Service-layer permission checks must evaluate both capability and scope.
- Repository scoping rules from tenant and branch standards still apply even when a role has agency-wide permission.
- Admin visibility into MFA must expose status only, never TOTP secrets or recovery codes.
- Any capability not represented in this matrix must be documented before implementation.

## Test design rules

- Every module implementation must include allow/deny tests for each role that receives or is denied the module capability.
- Branch-scoped permissions must have tests proving cross-branch denial.
- Agency-wide permissions must have tests proving cross-agency denial still applies.
- Self-service permissions must have tests proving users cannot act on another user through the same endpoint.

## Approval note

This matrix is approved for implementation in Epic 1 authorization work. If product decisions change, update the ADR and this matrix before changing code behavior.
