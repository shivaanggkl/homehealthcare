# Analytics Domain Standard

## Status

Accepted

## Purpose

This standard defines the shared Epic 15 analytics vocabulary for dashboard metrics, branch performance, utilization, backlog summaries, readiness/compliance summaries, and metric refresh behavior.

## Shared analytics categories

Epic 15 uses these analytics categories:

- `DASHBOARD_METRIC_DEFINITION`
- `DASHBOARD_METRIC_SNAPSHOT`
- `BRANCH_PERFORMANCE_SUMMARY`
- `UTILIZATION_SUMMARY`
- `BACKLOG_SUMMARY`
- `READINESS_COMPLIANCE_SUMMARY`
- `METRIC_TREND_SNAPSHOT`
- `DASHBOARD_REFRESH_REQUEST`

## Shared dashboard metric vocabulary

Epic 15 metric definitions and snapshots should use the shared metric type vocabulary:

- `TODAYS_VISITS`
- `UNFILLED_VISITS`
- `LATE_STARTS`
- `MISSED_VISITS`
- `DOCUMENTATION_AGING`
- `QA_BACKLOG`
- `CAREGIVER_UTILIZATION`
- `BRANCH_PERFORMANCE`
- `REVENUE_READINESS`
- `COMPLIANCE_EXCEPTIONS`

## Metric scope

Analytics metrics may be defined at:

- `AGENCY`
- `BRANCH`
- `CAREGIVER`

The narrowest real operational scope should be used for drilldown and authorization.

## Source-of-truth rules

Epic 15 summaries must derive from approved source domains:

- scheduling for visit-load and staffing posture
- EVV for missed-visit and late-start supporting context
- documentation for aging and completeness posture
- review for QA backlog posture
- compliance for exception posture
- revenue-readiness for finance blocker posture

Analytics must not re-own workflow lifecycle state already defined in those domains.

## Refresh contract

Epic 15 refresh behavior uses:

- `NEAR_REAL_TIME` for source-derived dashboard values that can be recomputed from current state
- `SCHEDULED` for summary or trend refreshes that can tolerate bounded staleness
- `ON_DEMAND` for explicit refreshes triggered by authorized actors

Refresh expectations should define:

- metric type
- refresh mode
- maximum staleness window in minutes
- short explanation of the recalculation expectation

## Audit expectations

Epic 15 audit actions must include:

- `ANALYTICS_DASHBOARD_SNAPSHOT_GENERATED`
- `ANALYTICS_METRIC_REFRESH_TRIGGERED`
- `ANALYTICS_BRANCH_PERFORMANCE_REFRESHED`
- `ANALYTICS_UTILIZATION_SUMMARY_REFRESHED`
- `ANALYTICS_QA_BACKLOG_SUMMARY_REFRESHED`
- `ANALYTICS_REVENUE_READINESS_SUMMARY_REFRESHED`
- `ANALYTICS_COMPLIANCE_SUMMARY_REFRESHED`

Audit target types must remain explicit and map to analytics projections rather than leaking internal implementation classes.

## Privacy rules

Epic 15 analytics should remain operationally useful while minimizing unnecessary patient and caregiver identifiers in summary output.

Summary responses should expose only the detail necessary for authorized drilldown and route-back behavior.
