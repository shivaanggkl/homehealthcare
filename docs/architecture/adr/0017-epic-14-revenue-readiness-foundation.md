# Epic 14 Revenue Readiness Foundation

## Status

Accepted

## Context

Epic 14 introduces upstream revenue-readiness and operational finance handoff behavior.

The platform already has source-of-truth records for:

- visits and branch-scoped scheduling context
- patient payer and authorization linkage
- EVV and signature verification
- visit documentation submission and completeness
- QA and compliance findings that can affect readiness

Epic 14 must not become a claim engine. It exists to classify operational readiness, expose blockers early, and produce export-ready payroll or invoice handoff outputs.

## Decision

Epic 14 will use a dedicated `revenuereadiness` foundation package with explicit shared vocabulary for:

- revenue-readiness projection categories
- pass/warning/fail validation outcomes
- readiness lifecycle states
- export lifecycle states
- authorization-usage posture
- revenue guardrails
- audit actions and target types

Revenue-readiness projections and related outputs are branch-scoped operational artifacts. They must carry both `agency_id` and `branch_id` once modeled as persisted entities or materialized projections.

Audit coverage is required for:

- readiness recalculation
- exception-flag changes
- export generation
- authorization-usage refresh
- payer/service summary refresh

## Consequences

- Epic 14 backend work can share one stable audit and authorization model
- frontend Epic 14 work can rely on stable action types for filtered audit links
- later revenue-cycle epics can extend these foundations without redefining readiness vocabulary
- claim generation, remittance posting, denial queues, and physician-signature workflows remain out of scope for this ADR
