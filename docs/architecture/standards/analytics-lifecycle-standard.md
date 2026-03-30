# Analytics Lifecycle Standard

## Status

Accepted

## Purpose

This standard defines the shared Epic 15 lifecycle and guardrail rules for dashboard snapshots, metric refresh requests, and analytics summary projections.

## Lifecycle expectations

### 1. Metric definitions

Metric definitions are stable reference vocabulary.

They define:

- metric name
- scope
- source-of-truth domain
- refresh mode
- expected staleness window

### 2. Dashboard snapshots

Dashboard snapshots are deterministic summaries generated from current or recently refreshed source data.

Snapshots may be:

- source-derived near-real-time summaries
- scheduled summary materializations
- on-demand refresh results

### 3. Refresh requests

Refresh requests represent explicit analytics recalculation intent.

They should record:

- actor
- scope
- metric or dashboard target
- trigger mode
- refresh timestamp

### 4. Trend snapshots

Trend snapshots are optional summary history records for bounded recent windows such as day-over-day or last-7-day views.

They should not replace source-of-truth operational history records.

## Guardrails

- analytics projections must remain read-oriented and must not mutate upstream operational workflow state
- branch-aware summaries must validate that branch scope matches the actor's authorized branch access
- patient or caregiver identifying detail must be minimized in high-level summary outputs
- refresh actions must record audit events through the shared audit infrastructure
- refresh behavior should distinguish clearly between near-real-time, scheduled, and on-demand outputs
