# ADR 0018: Epic 15 Analytics Foundation

## Status

Accepted

## Context

Epic 15 introduces dashboard and analytics capabilities across scheduling, EVV, documentation, review, compliance, and revenue-readiness data.

The platform already has source domains for operational workflow state, but it does not yet have a shared analytics vocabulary, refresh contract, or audit taxonomy for dashboard snapshots and metric refresh operations.

Without a dedicated foundation, dashboard metrics would be derived inconsistently and authorization rules would drift across command-center and leadership-facing analytics surfaces.

## Decision

We introduce an Epic 15 analytics foundation with:

- explicit analytics categories for metric definitions, dashboard snapshots, branch performance, utilization, backlog, readiness/compliance summaries, trend snapshots, and refresh requests
- a shared dashboard metric vocabulary for today visits, unfilled visits, late starts, missed visits, documentation aging, QA backlog, caregiver utilization, branch performance, revenue readiness, and compliance exceptions
- a refresh contract that distinguishes:
  - near-real-time source-derived metrics
  - scheduled summary refreshes
  - on-demand refreshes by authorized actors
- Epic 15 audit actions for dashboard snapshot generation and metric refresh activity
- explicit permissions for analytics workspace, branch performance, caregiver utilization, QA backlog, revenue-readiness, compliance-exception visibility, and metric refresh actions

## Source-of-truth linkage

Epic 15 derives analytics from existing domains instead of redefining workflow ownership:

- Epic 5 scheduling provides today visits, unfilled visits, and assignment/open-shift state
- Epic 7 EVV provides missed-visit and verification exception context
- Epic 8 documentation provides documentation state and aging context
- Epic 10 review provides QA backlog state
- Epic 11 compliance provides compliance exception posture
- Epic 14 revenue readiness provides finance-ready warning and blocked posture

## Consequences

- dashboard and analytics services must remain projection-driven and read-oriented
- analytics endpoints must enforce agency and branch-aware visibility consistent with the source domains
- dashboard refresh and summary generation can be observed through the shared audit event infrastructure
- future Epic 15 API and projection work will build on this vocabulary instead of introducing ad hoc metric names or refresh rules
