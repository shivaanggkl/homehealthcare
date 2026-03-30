# HomeHealthCare Complete Product Guide

## 1. Document Purpose

This document is the single consolidated guide for the implemented HomeHealthCare product across:

- backend repository: `/Users/shiva/Documents/GitHub/homehealthcare`
- frontend repository: `/Users/shiva/Documents/GitHub/homehealthcarefe`

It is written to help:

- founders and product stakeholders understand what has been built
- backend and frontend engineers understand the current product surface
- QA and reviewers understand how to test the platform
- future contributors understand module boundaries, route behavior, and system expectations

This guide reflects the product that has been planned and implemented through Epic 15.

---

## 2. Product Summary

### 2.1 What this product is

HomeHealthCare is a multi-tenant agency operating system for home-based care providers. It combines daily operations, caregiver field execution, documentation, compliance, QA review, revenue readiness, and leadership analytics into one role-aware platform.

The product is built around the reality that home-based care agencies do not run on one isolated workflow. They run on a connected chain:

- agencies and branches need secure setup and administration
- schedulers need workforce and visit control
- caregivers need mobile execution and EVV support
- documentation must become reviewable and auditable
- compliance and patient-event tracking must stay current
- revenue readiness depends on upstream operational completeness
- leadership needs cross-domain analytics and command-center visibility

### 2.2 Product positioning

The platform is not implemented as a narrow scheduling tool or a narrow clinical note tool. It is implemented as an agency operating system with:

- secure tenancy and RBAC
- branch-aware operational control
- caregiver mobile workflows
- patient and workforce records
- scheduling and assignment
- visit verification and exceptions
- documentation and task completion
- messaging and coordination
- QA and review
- compliance and patient-event tracking
- care progression
- revenue readiness
- analytics and command-center reporting

### 2.3 Primary user groups

- agency owner
- branch admin
- scheduler or coordinator
- caregiver or field clinician
- QA or clinical reviewer
- compliance lead
- billing or back-office user
- read-only auditor or oversight user

---

## 3. What Is Included In The Product Today

### 3.1 Implemented epic map

| Epic | Area | Current outcome |
| --- | --- | --- |
| Epic 1 | Platform foundation | tenancy, auth, MFA, sessions, audit, security settings |
| Epic 2 | Configuration foundation | service lines, visit types, task templates, documentation templates, alert and policy setup |
| Epic 3 | Patient domain | patient master record, contacts, payers, diagnoses, attachments, eligibility, authorizations |
| Epic 4 | Workforce domain | caregiver records, workforce catalog, mileage pay, branch assignments, schedulability context |
| Epic 5 | Scheduling | scheduling workspace, assignments, conflicts, recurring visits, cancellations, audit-aware scheduling UX |
| Epic 6 | Mobile execution | caregiver mobile app, today-work, visit execution, checklist, notes, artifacts, messaging, offline-aware flows |
| Epic 7 | EVV | clock-in and clock-out, geofence-aware verification, missed visits, exceptions, supervisor escalation |
| Epic 8 | Documentation | visit-note templates, task libraries, visit-note editing, attachments, printable summaries |
| Epic 9 | Messaging | secure inbox, threads, patient or visit context, staff groups, broadcasts, escalation tags |
| Epic 10 | Review | QA queue, completeness review, return-for-fix, signoff, coordinator review command center |
| Epic 11 | Compliance | compliance dashboard, patient compliance workspace, acknowledgments, certification periods, reminders |
| Epic 12 | Patient events | incident, infection, wound, evidence, follow-up, escalation, patient-event command center |
| Epic 13 | Care progression | goal templates, patient goals, interventions, progress notes, sync, progression command center |
| Epic 14 | Revenue readiness | readiness validation, exception flags, payroll/invoice previews, authorization usage, finance command center |
| Epic 15 | Analytics | dashboard, operational drilldowns, utilization, branch performance, readiness/compliance analytics, command center |

### 3.2 Product boundary of the current build

The current build is strong in operational, workflow, visibility, audit, and readiness layers.

The current build is not a fully finished external ecosystem platform yet. In particular:

- no physician portal is described as fully implemented
- no family portal is described as fully implemented
- no advanced payer-claim lifecycle beyond revenue-readiness and export preparation is described as fully implemented
- integrations are not described here as complete external production connectors

The implemented product should be understood as:

`secure operations + field execution + documentation + QA + compliance + readiness + analytics`

---

## 4. Repository Overview

### 4.1 Backend repository

Path:

- `/Users/shiva/Documents/GitHub/homehealthcare`

Primary stack:

- Java 21
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Flyway
- H2 for local and test execution
- Gradle

Primary implementation style:

- domain-oriented packages per module
- API controller + application service + domain repository pattern
- agency and branch scope enforcement in services and authorization guards
- audit-first behavior for sensitive operations
- migration-driven schema evolution

### 4.2 Frontend repository

Path:

- `/Users/shiva/Documents/GitHub/homehealthcarefe`

Primary stack:

- React
- TypeScript
- React Router
- Vite
- Vitest
- Testing Library

Primary implementation style:

- protected route model
- route-driven workspaces
- shared foundation components per major epic area
- centralized session API client in `src/app/auth/session-api.ts`
- role and permission mapping in `src/app/access/access-control.ts`
- responsive CSS in `src/styles.css`

---

## 5. High-Level Architecture

### 5.1 Core platform layers

1. Identity and security
2. Agency and branch tenancy
3. Shared configuration and catalogs
4. Operational records: patients, workforce, scheduling
5. Field execution and EVV
6. Documentation and communication
7. Review and compliance
8. Clinical or care follow-up domains
9. Revenue-readiness and analytics

### 5.2 Core cross-cutting concerns

- role-based access control
- branch-aware access restrictions
- audit logging
- session security
- tenant isolation
- standard lifecycle vocabularies per domain
- consistent frontend route-guard behavior
- CI task coverage per epic

---

## 6. Backend Implementation Catalog

This section groups the backend by product area. Each area lists the implemented API controllers and what behavior they are expected to provide.

### 6.1 Platform, auth, and security

Controllers:

- `auth/api/LoginController.java`
- `auth/api/MfaLoginController.java`
- `auth/api/RefreshSessionController.java`
- `auth/api/LogoutController.java`
- `auth/api/SessionStatusController.java`
- `auth/api/UserSessionsController.java`
- `auth/api/ForgotPasswordController.java`
- `auth/api/ResetPasswordController.java`
- `auth/api/ChangePasswordController.java`
- `auth/api/MfaController.java`
- `auth/api/PasswordPolicyController.java`
- `agency/api/AgencyMfaPolicyController.java`
- `notification/api/AdminNotificationPreferencesController.java`
- `security/api/SecuritySettingsController.java`
- `me/api/CurrentAccessController.java`

What this area does:

- authenticates users
- manages secure sessions and refresh rotation
- supports MFA enrollment and MFA challenge completion
- supports password reset and password change
- exposes current access and route-guard context to the frontend
- manages agency-level security controls and admin notification settings

What to expect:

- secure session lifecycle behavior
- controlled invalid-credential and reset behavior
- audit events for sensitive auth and security operations
- role-aware access payload used by frontend route protection

### 6.2 Agency, branch, and administration

Controllers:

- `agencyprofile/api/AgencyProfileController.java`
- `agency/api/AgencySettingsController.java`
- `branch/api/BranchManagementController.java`
- `branchpolicy/api/BranchPolicyController.java`
- `user/api/UserDirectoryController.java`
- `user/api/UserProfileAssignmentController.java`
- `user/api/UserSelfProfileController.java`
- `user/api/UserStatusController.java`
- `invitation/api/UserInvitationController.java`

What this area does:

- manages agency profile and agency-level settings
- manages branches and branch policies
- manages user directory and status lifecycle
- assigns user access profiles and branch scope
- supports invitation and onboarding flows
- supports self-profile maintenance

What to expect:

- agency admins can control organization structure
- branch-aware rules are enforced
- user-access changes are auditable
- onboarding and user lifecycle are security-aware

### 6.3 Configuration and setup modules

Controllers:

- `serviceline/api/ServiceLineController.java`
- `visittype/api/VisitTypeController.java`
- `tasktemplate/api/TaskTemplateController.java`
- `documentationtemplate/api/DocumentationTemplateController.java`
- `alertrule/api/AlertRuleController.java`
- `mileagepay/api/MileagePaySettingController.java`
- `workforce/api/WorkforceCatalogController.java`

What this area does:

- manages reusable operating configuration
- defines service lines and visit types
- defines reusable task and documentation templates
- manages alerting rules
- supports workforce and mileage-related setup

What to expect:

- setup data drives later patient, visit, documentation, and review workflows
- template and catalog behavior is shared by multiple downstream modules

### 6.4 Patient domain

Controllers:

- `patient/api/PatientController.java`
- `patientaddress/api/PatientAddressController.java`
- `patientattachment/api/PatientAttachmentController.java`
- `patientauthorization/api/PatientEpisodeAuthorizationController.java`
- `patientcontact/api/PatientContactController.java`
- `patientdiagnosis/api/PatientDiagnosisController.java`
- `patienteligibility/api/PatientServiceEligibilityController.java`
- `patientpayer/api/PatientPayerLinkController.java`

What this area does:

- creates and maintains patient master records
- stores demographics and address information
- links diagnoses, contacts, payers, attachments, authorizations, and eligibility

What to expect:

- patient data is the source context for scheduling, documentation, compliance, and analytics
- supporting records are branch-aware and tenant-aware
- attachment and financial context can feed later readiness and compliance logic

### 6.5 Workforce domain

Controllers:

- `workforce/api/CaregiverController.java`
- `workforce/api/WorkforceCatalogController.java`

What this area does:

- manages caregiver profiles and staffing records
- supports workforce catalog and role-aligned operational data

What to expect:

- caregiver records can be referenced in scheduling, mobile, EVV, utilization, and analytics
- branch assignments and permissions control what each caregiver or supervisor can access

### 6.6 Scheduling domain

Controllers:

- `scheduling/api/SchedulingController.java`

What this area does:

- manages visit occurrences and scheduling views
- supports assignment workflows
- supports recurring visit rules
- validates conflicts, overlaps, and decision-support outcomes
- supports reschedule and cancellation flows

What to expect:

- branch-aware scheduling control
- explicit workflow states for assignment and decision support
- audit events on high-impact scheduling mutations

### 6.7 Mobile execution domain

Controllers:

- `mobile/api/MobileExecutionController.java`

What this area does:

- powers the caregiver mobile app
- exposes mobile home, route, visit detail, patient summary, care instructions
- starts and ends mobile execution sessions
- stores task checklist entries, quick notes, artifacts, incidents, and message-thread actions

What to expect:

- mobile routes use live backend APIs
- caregiver ownership and branch scope are enforced
- mobile actions are auditable
- API responses support field workflows and offline-oriented UI patterns

### 6.8 EVV domain

Controllers:

- `evv/api/EvvController.java`

What this area does:

- clock-in and clock-out handling
- geofence and verification outcome capture
- EVV exception handling
- missed-visit workflows
- supervisor notification and escalation actions

What to expect:

- verification behavior is linked to mobile execution and later compliance or revenue workflows
- EVV operations are strongly audited
- exception and missed-visit flows are separate first-class workflows

### 6.9 Documentation domain

Controllers:

- `documentation/api/DocumentationController.java`
- `documentationtemplate/api/DocumentationTemplateController.java`
- `tasktemplate/api/TaskTemplateController.java`

What this area does:

- manages documentation templates and task libraries
- creates and updates visit documentation records
- saves drafts, validates submission readiness, and submits records
- manages attachment linkage and printable summary generation

What to expect:

- documentation is not only text capture; it is structured workflow state
- task completion and required-field logic are handled as part of documentation
- printable outputs and attachment relationships are part of the core module

### 6.10 Messaging domain

Controllers:

- `messaging/api/MessagingController.java`

What this area does:

- manages secure threads and messages
- supports patient-linked, visit-linked, and task-linked discussion context
- supports staff groups and branch broadcasts
- supports escalation tagging and read state

What to expect:

- secure internal coordination rather than consumer messaging
- thread context matters
- broadcasts and escalation tags are part of operational communication

### 6.11 Review domain

Controllers:

- `review/api/ReviewController.java`

What this area does:

- powers the QA and review queue
- exposes completeness findings and missing-field results
- supports assignment, reassignment, release, approval, rejection, return-for-fix, signoff, and resubmission
- exposes review history and exception views

What to expect:

- review is a workflow layer above documentation and exception records
- queue behavior is separate from documentation editing
- branch and reviewer scope matter

### 6.12 Compliance domain

Controllers:

- `compliance/api/ComplianceController.java`

What this area does:

- exposes compliance dashboard and patient-level compliance summary
- manages acknowledgments
- manages certification periods
- manages reminders and recalculation workflows
- exposes history and change visibility

What to expect:

- compliance readiness is derived from multiple upstream modules
- checklist and documentation evidence feed into readiness state
- compliance is visible at dashboard and patient scope

### 6.13 Patient-event domain

Controllers:

- `patientevent/api/PatientEventController.java`

What this area does:

- manages incidents, infections, wounds, wound-history entries, evidence links, follow-up assignments, escalations, alerts, and patient-level longitudinal timelines

What to expect:

- patient events are structured records, not loose notes
- follow-up and escalation are explicit lifecycle steps
- patient-event history is longitudinal and auditable

### 6.14 Care progression domain

Controllers:

- `careprogression/api/CareProgressionController.java`

What this area does:

- manages goal templates, patient goals, interventions, progress notes, version history, and care-plan sync records

What to expect:

- goals and interventions are structured stateful records
- progression is visible longitudinally
- care-plan sync status is explicit

### 6.15 Revenue-readiness domain

Controllers:

- `revenuereadiness/api/RevenueReadinessController.java`

What this area does:

- evaluates readiness for finance handoff
- manages readiness flags and summaries
- exposes payroll and invoice export preview or generation
- exposes authorization-usage and payer-service summaries
- exposes history for finance-related actions

What to expect:

- finance readiness is derived from upstream operational completeness
- blocked and warning states are first-class
- export generation is controlled and audited

### 6.16 Analytics domain

Controllers:

- `analytics/api/AnalyticsController.java`

What this area does:

- exposes dashboard summary
- exposes operational, backlog, revenue, and compliance drilldowns
- exposes branch performance and caregiver utilization
- exposes trend and refresh history
- triggers controlled analytics refresh

What to expect:

- analytics are branch-aware and permission-aware
- analytics rely on upstream modules rather than separate source-of-truth data entry
- refresh is a controlled backend operation, not a casual UI-only action

---

## 7. Frontend Page Catalog

This section catalogs the implemented frontend pages and what behavior to expect from them. Pages are grouped by product area.

### 7.1 Auth and security pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `LoginPage.tsx` | main web login | email/password login, session bootstrap, backend error handling |
| `MfaChallengePage.tsx` | MFA step after login | TOTP or recovery-code challenge flow |
| `ForgotPasswordPage.tsx` | password recovery start | safe forgot-password submission |
| `ResetPasswordPage.tsx` | password reset completion | token-driven reset flow with policy handling |
| `ChangePasswordPage.tsx` | signed-in password change | policy-aware change-password behavior |
| `MfaSettingsPage.tsx` | self-service MFA settings | MFA status and enrollment actions |
| `ActiveSessionsPage.tsx` | current and recent sessions | user session visibility and revocation |
| `SecuritySettingsPage.tsx` | admin security settings | MFA policy and security-oriented admin settings |
| `AdminMfaPolicyPage.tsx` | agency MFA rules | agency-level MFA policy editing |
| `AdminNotificationPreferencesPage.tsx` | admin notifications | critical security or admin-notification preferences |
| `AcceptInvitationPage.tsx` | invitation onboarding | accept invite, establish password, join agency |

### 7.2 Agency, branch, user, and setup pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `HomePage.tsx` | application landing page | role-driven navigation entrypoint |
| `AgencyProfileSetupPage.tsx` | agency profile setup | profile information maintenance |
| `AgencySettingsPage.tsx` | agency settings | organization-level operating settings |
| `BranchManagementPage.tsx` | branch administration | branch CRUD-style management and visibility |
| `BranchPolicySetupPage.tsx` | branch rules | branch-policy configuration |
| `UserDirectoryPage.tsx` | user administration | user listing, status, and identity visibility |
| `InviteUserPage.tsx` | invite flow | issue onboarding invites |
| `SelfProfilePage.tsx` | own profile | current user profile maintenance |
| `SetupOverviewPage.tsx` | setup landing area | entry point into catalog and setup modules |
| `ServiceLineSetupPage.tsx` | service-line configuration | service-line CRUD and effective behavior |
| `VisitTypeSetupPage.tsx` | visit-type configuration | visit-type definitions and status handling |
| `TaskTemplateSetupPage.tsx` | task-template configuration | reusable task definitions |
| `DocumentationTemplateSetupPage.tsx` | older setup route | setup-oriented entry into documentation template management |
| `WorkforceCatalogSetupPage.tsx` | workforce catalog setup | staffing and workforce catalog configuration |
| `MileagePaySetupPage.tsx` | mileage-pay settings | mileage pay defaults and settings |
| `AlertRuleSetupPage.tsx` | alert rule setup | configurable alert rules |
| `ConfigurationModuleScaffoldPage.tsx` | scaffold or placeholder surface | generic support scaffold for setup modules where used |

### 7.3 Patient pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `PatientWorkspacePage.tsx` | patient list and workspace | patient-level search, selection, and module entry |
| `PatientRecordWorkspacePage.tsx` | patient detail workspace | patient master data, linked records, and operational context |

### 7.4 Workforce pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `CaregiverWorkspacePage.tsx` | caregiver list and workspace | workforce browsing and high-level caregiver management |
| `CaregiverRecordWorkspacePage.tsx` | caregiver detail workspace | caregiver profile, assignments, and staffing details |

### 7.5 Scheduling pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `SchedulingWorkspacePage.tsx` | main scheduling workspace | assignment board, decision support, recurring visit actions, reschedule and cancellation workflows |

### 7.6 Mobile execution and EVV pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `MobileLoginPage.tsx` | caregiver mobile login | field-first authenticated mobile entry |
| `MobileWorkspacePage.tsx` | caregiver mobile shell | today-work, route, visit detail, execution, EVV, notes, artifacts, incidents, messages, offline-aware behavior |
| `MobileDocumentationPage.tsx` | mobile documentation route | visit-note editing from the mobile workflow |
| `EvvIssueListPage.tsx` | coordinator EVV issue view | missed-visit and EVV exception visibility for coordinators |

### 7.7 Documentation pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `DocumentationWorkspacePage.tsx` | documentation landing page | route entry to templates, task library, visit notes, and status |
| `DocumentationTemplateWorkspacePage.tsx` | template workspace | build and manage documentation templates |
| `DocumentationTaskLibraryPage.tsx` | task library | build and manage reusable documentation tasks |
| `DocumentationRecordWorkspacePage.tsx` | visit-note detail | draft, edit, validate, submit, and link evidence |
| `DocumentationStatusPage.tsx` | coordinator-facing visibility | draft, incomplete, and submitted documentation status view |
| `PrintableDocumentationPage.tsx` | printable summary view | formatted read-only printable summary output |

### 7.8 Messaging pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `MessagingWorkspacePage.tsx` | secure messaging workspace | inbox, thread detail, staff group, broadcast, escalation, and context-linked discussion flows |
| `MessagingCommandCenterPage.tsx` | messaging summary route | coordinator or leadership message visibility, escalation, unread, and broadcast awareness |

### 7.9 Review pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `ReviewWorkspacePage.tsx` | review queue and detail | review worklist, findings, assignment, decision, and signoff flows |
| `ReviewCommandCenterPage.tsx` | review summary route | coordinator visibility into pending, overdue, exception, and signoff work |

### 7.10 Compliance pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `ComplianceWorkspacePage.tsx` | compliance dashboard and patient summary | dashboard, patient-level gaps, acknowledgments, certification periods, reminders |
| `ComplianceCommandCenterPage.tsx` | compliance command center | coordinator-facing compliance triage with filtered patient visibility |

### 7.11 Patient-event pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `PatientEventWorkspacePage.tsx` | incident/infection/wound workspace | create, update, resolve, follow-up, escalation, evidence, and patient timeline workflows |
| `PatientEventCommandCenterPage.tsx` | patient-event summary route | open incident, infection, escalation, and overdue follow-up visibility |

### 7.12 Care-progression pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `GoalWorkspacePage.tsx` | goals and progression workspace | templates, patient goals, interventions, notes, history, sync |
| `GoalCommandCenterPage.tsx` | progression command center | overdue goals, unmet goals, sync risk, progression visibility |

### 7.13 Revenue-readiness pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `RevenueReadinessWorkspacePage.tsx` | finance readiness workspace | readiness list/detail, exceptions, payroll preview, invoice preview, authorization usage |
| `RevenueCommandCenterPage.tsx` | finance command center | blocked readiness, missing signatures, completion failures, authorization warnings |

### 7.14 Analytics pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `AnalyticsWorkspacePage.tsx` | analytics dashboard and drilldown workspace | dashboard, operational drilldowns, backlog detail, branch performance, utilization, readiness/compliance analytics, shared refresh and filters |
| `AnalyticsCommandCenterPage.tsx` | leadership analytics command center | summary lanes for operational pressure, backlog, utilization, revenue, and compliance visibility |

### 7.15 Other core and support pages

| Page file | What it is | What to expect |
| --- | --- | --- |
| `AuditLogPage.tsx` | audit workspace | filtered audit review for authorized users |
| `NotFoundPage.tsx` | fallback route | controlled route miss behavior |

---

## 8. Frontend Shared Component Catalog

These are the major reusable components and what they contribute.

| Component file | Purpose |
| --- | --- |
| `ProtectedRoute.tsx` | role-aware web route guard |
| `MobileProtectedRoute.tsx` | caregiver-mobile route guard |
| `AccessDeniedPanel.tsx` | consistent unauthorized or unsupported route state |
| `AccessProfileCard.tsx` | access-profile summary UI |
| `RouteAccessBoundary.tsx` | boundary treatment for route-level access control |
| `UnauthorizedRouteState.tsx` | standardized unauthorized-state treatment |
| `SessionTimeoutWarning.tsx` | session expiration warning UI |
| `PasswordPolicyPanel.tsx` | password-rule explanation UI |
| `ConfigurationFoundation.tsx` | reusable configuration workspace shell patterns |
| `ConfigurationSupport.tsx` | shared configuration helpers |
| `PatientWorkspaceFoundation.tsx` | shared patient workspace layout and state treatment |
| `WorkforceWorkspaceFoundation.tsx` | shared workforce workspace layout and state treatment |
| `SchedulingWorkspaceFoundation.tsx` | shared scheduling workspace foundation |
| `MobileWorkspaceFoundation.tsx` | shared caregiver-mobile layout and module states |
| `MobileEvvFoundation.tsx` | shared mobile EVV UI foundation |
| `DocumentationWorkspaceFoundation.tsx` | shared documentation shell, banners, and audit callouts |
| `DocumentationRecordEditor.tsx` | shared visit-note editor used across desktop and mobile documentation routes |
| `MessagingWorkspaceFoundation.tsx` | shared inbox, thread, state, and command-center UI patterns |
| `ReviewWorkspaceFoundation.tsx` | shared review queue and review-detail presentation |
| `ComplianceWorkspaceFoundation.tsx` | shared compliance workspace and audit callout presentation |
| `PatientEventWorkspaceFoundation.tsx` | shared patient-event workspace presentation |
| `GoalWorkspaceFoundation.tsx` | shared care-progression workspace presentation |
| `RevenueReadinessWorkspaceFoundation.tsx` | shared revenue-readiness shell, banners, and audit treatment |
| `AnalyticsWorkspaceFoundation.tsx` | shared analytics shell, summary-card, table, audit-callout, and command-center presentation |

What to expect from these foundations:

- consistent layout language inside each product area
- shared loading, empty, failure, unauthorized, and read-only states
- audit-aware UX patterns where the epic requires it
- reduced duplication between page-level route implementations

---

## 9. Page Behavior Guide By Product Area

This section explains what users should experience on the major workspaces.

### 9.1 Scheduling workspace

Expected behavior:

- view scheduled work and assignment context
- assign or reassign caregivers
- review conflict and overlap guidance before committing changes
- manage recurring visit rules
- reschedule or cancel visits
- see audit-aware messaging for sensitive scheduling mutations

Expected backend support:

- `SchedulingController`
- scheduling domain and audit services

Primary frontend route:

- `SchedulingWorkspacePage.tsx`

Primary tests:

- `SchedulingWorkspacePage.test.tsx`
- backend scheduling service and API integration tests

### 9.2 Mobile execution and EVV

Expected behavior:

- caregiver signs in to mobile app
- sees route and today-work summary
- opens visit detail
- starts and ends visit execution
- captures checklist items, notes, artifacts, incidents, and messages
- handles EVV clock-in, clock-out, missed visits, exceptions, and escalations
- sees offline-aware and sync-aware messaging

Expected backend support:

- `MobileExecutionController`
- `EvvController`

Primary frontend routes:

- `MobileLoginPage.tsx`
- `MobileWorkspacePage.tsx`
- `MobileDocumentationPage.tsx`

Primary tests:

- `MobileWorkspacePage.test.tsx`
- `MobileDocumentationPage.test.tsx`
- `EvvIssueListPage.test.tsx`
- backend mobile and EVV integration suites

### 9.3 Documentation and review

Expected behavior:

- templates and task libraries can be managed by authorized users
- visit documentation can be created, drafted, validated, and submitted
- attachments can be linked
- printable summaries are available
- review queue receives downstream completeness and QA work
- returned-for-fix and signoff loops are explicit

Expected backend support:

- `DocumentationController`
- `DocumentationTemplateController`
- `TaskTemplateController`
- `ReviewController`

Primary frontend routes:

- `DocumentationTemplateWorkspacePage.tsx`
- `DocumentationTaskLibraryPage.tsx`
- `DocumentationRecordWorkspacePage.tsx`
- `DocumentationStatusPage.tsx`
- `PrintableDocumentationPage.tsx`
- `ReviewWorkspacePage.tsx`
- `ReviewCommandCenterPage.tsx`

Primary tests:

- documentation route tests
- `DocumentationRecordEditor.test.tsx`
- review route tests
- backend documentation and review suites

### 9.4 Compliance, patient events, care progression

Expected behavior:

- compliance dashboard shows patient and branch-level readiness
- patient-event workspace captures incidents, infections, wounds, and follow-up
- goals workspace manages care-progression structure and status
- command-center pages surface the highest-risk work without exposing unnecessary detail

Expected backend support:

- `ComplianceController`
- `PatientEventController`
- `CareProgressionController`

Primary frontend routes:

- `ComplianceWorkspacePage.tsx`
- `ComplianceCommandCenterPage.tsx`
- `PatientEventWorkspacePage.tsx`
- `PatientEventCommandCenterPage.tsx`
- `GoalWorkspacePage.tsx`
- `GoalCommandCenterPage.tsx`

Primary tests:

- compliance, patient-event, and goal route tests
- backend compliance, patient-event, and care-progression test suites

### 9.5 Revenue readiness and analytics

Expected behavior:

- revenue workspace shows which visits are blocked, warning, or ready
- export-preview behavior is visible without exposing unnecessary finance internals
- analytics workspace summarizes operational, backlog, utilization, revenue, and compliance posture
- command-center surfaces prioritize leadership triage
- audit-aware messaging makes it clear that refresh-like operations are controlled and logged

Expected backend support:

- `RevenueReadinessController`
- `AnalyticsController`

Primary frontend routes:

- `RevenueReadinessWorkspacePage.tsx`
- `RevenueCommandCenterPage.tsx`
- `AnalyticsWorkspacePage.tsx`
- `AnalyticsCommandCenterPage.tsx`

Primary tests:

- revenue route tests
- analytics route tests
- backend revenue and analytics test suites

---

## 10. Backend-to-Frontend Feature Mapping

This section links the main user-facing frontend areas to the backend modules that power them.

| Frontend area | Main frontend pages | Main backend controllers |
| --- | --- | --- |
| Auth and sessions | `LoginPage`, `MfaChallengePage`, `ActiveSessionsPage` | login, MFA, session, logout, current access controllers |
| Agency and setup | setup and settings pages | agency, branch, user, alert, service-line, visit-type, template controllers |
| Patients | `PatientWorkspacePage`, `PatientRecordWorkspacePage` | patient and supporting patient controllers |
| Workforce | caregiver pages | caregiver and workforce catalog controllers |
| Scheduling | `SchedulingWorkspacePage` | scheduling controller |
| Mobile | `MobileLoginPage`, `MobileWorkspacePage`, `MobileDocumentationPage` | mobile execution controller |
| EVV | mobile EVV flows, `EvvIssueListPage` | EVV controller |
| Documentation | documentation workspace pages | documentation, template, and task controllers |
| Messaging | messaging pages | messaging controller |
| Review | review pages | review controller |
| Compliance | compliance pages | compliance controller |
| Patient events | patient-event pages | patient-event controller |
| Care progression | goal pages | care-progression controller |
| Revenue readiness | revenue pages | revenue-readiness controller |
| Analytics | analytics pages | analytics controller |

---

## 11. Testing Guide

This section explains how the current project is tested and how to approach validating every major area.

### 11.1 Backend testing strategy

The backend uses:

- application-service tests
- repository tests
- API integration tests
- authorization guard tests
- audit integration tests
- epic-specific regression tasks in Gradle

Common backend commands:

```bash
./gradlew test
./gradlew authMfaTest
./gradlew epic2ApiTest
./gradlew epic3ApiTest
./gradlew epic4ApiTest
./gradlew epic5ApiTest
./gradlew epic6ApiTest
./gradlew epic7ApiTest
./gradlew epic8ApiTest
./gradlew epic9ApiTest
./gradlew epic10ApiTest
./gradlew epic11ApiTest
./gradlew epic12ApiTest
./gradlew epic13ApiTest
./gradlew epic14ApiTest
./gradlew epic15ApiTest
```

CI coverage:

- `.github/workflows/ci.yml` runs `authMfaTest` and `epic2ApiTest` through `epic15ApiTest`

### 11.2 Backend test coverage map

Representative backend test groups:

- auth and security:
  - login, logout, refresh, MFA, password policy, session status, security hardening
- agency and branch:
  - agency settings, branch management, tenant isolation
- catalogs and setup:
  - service-line, visit-type, configuration foundation tests
- patients:
  - patient API, patient supporting APIs, patient service and repository tests
- workforce:
  - caregiver API and workforce service tests
- scheduling:
  - scheduling API, scheduling service, scheduling rules, scheduling audit tests
- mobile and EVV:
  - mobile execution API, mobile support services, EVV API, EVV verification tests
- documentation:
  - documentation API, failure handling, documentation audit, documentation service tests
- messaging:
  - messaging API, messaging coordination service, messaging audit tests
- review:
  - review API, review workspace service, review audit tests
- compliance:
  - compliance API, compliance workspace service, compliance audit tests
- patient events:
  - patient-event API, patient-event service, patient-event audit tests
- care progression:
  - care-progression API, care-progression audit tests
- revenue readiness:
  - revenue API, readiness service, readiness audit tests
- analytics:
  - analytics API, analytics service, analytics audit tests

### 11.3 Frontend testing strategy

The frontend uses:

- route-level Vitest and Testing Library coverage
- component-level tests for shared or stateful components
- TypeScript no-emit validation
- production build verification with Vite

Common frontend commands:

```bash
cd /Users/shiva/Documents/GitHub/homehealthcarefe
npx tsc -p tsconfig.json --noEmit
npm test -- --run
npm run build
```

### 11.4 Frontend test inventory

Current frontend tests cover:

- route access and permission mapping:
  - `src/app/access/access-control.test.ts`
- route guards:
  - `src/app/components/ProtectedRoute.test.tsx`
- documentation editor:
  - `src/app/components/DocumentationRecordEditor.test.tsx`
- patient area:
  - `PatientWorkspacePage.test.tsx`
  - `PatientRecordWorkspacePage.test.tsx`
- workforce area:
  - `CaregiverWorkspacePage.test.tsx`
  - `CaregiverRecordWorkspacePage.test.tsx`
- setup area:
  - `SetupOverviewPage.test.tsx`
  - `ServiceLineSetupPage.test.tsx`
- scheduling:
  - `SchedulingWorkspacePage.test.tsx`
- mobile and EVV:
  - `MobileWorkspacePage.test.tsx`
  - `MobileDocumentationPage.test.tsx`
  - `EvvIssueListPage.test.tsx`
- documentation:
  - `DocumentationWorkspacePage.test.tsx`
  - `DocumentationTemplateWorkspacePage.test.tsx`
  - `DocumentationTaskLibraryPage.test.tsx`
  - `DocumentationStatusPage.test.tsx`
  - `PrintableDocumentationPage.test.tsx`
- messaging:
  - `MessagingWorkspacePage.test.tsx`
  - `MessagingCommandCenterPage.test.tsx`
- review:
  - `ReviewWorkspacePage.test.tsx`
  - `ReviewCommandCenterPage.test.tsx`
- compliance:
  - `ComplianceWorkspacePage.test.tsx`
  - `ComplianceCommandCenterPage.test.tsx`
- patient events:
  - `PatientEventWorkspacePage.test.tsx`
  - `PatientEventCommandCenterPage.test.tsx`
- care progression:
  - `GoalWorkspacePage.test.tsx`
  - `GoalCommandCenterPage.test.tsx`
- revenue readiness:
  - `RevenueReadinessWorkspacePage.test.tsx`
  - `RevenueCommandCenterPage.test.tsx`
- analytics:
  - `AnalyticsWorkspacePage.test.tsx`
  - `AnalyticsCommandCenterPage.test.tsx`

### 11.5 How to test each major area manually

#### Auth and security

Validate:

- login success
- login failure
- MFA-required path
- forgot-password submission
- reset-password completion
- change-password policy enforcement
- session timeout warning and forced logout behavior
- active session revocation

#### Setup and administration

Validate:

- agency settings save behavior
- branch create or update flows
- invite-user flow
- service-line and visit-type maintenance
- alert-rule and policy save behavior
- permission-aware page access

#### Patients and workforce

Validate:

- patient create or update flow
- linked patient records and tab behavior
- caregiver listing and caregiver detail visibility
- branch-scoped access expectations

#### Scheduling

Validate:

- board or schedule rendering
- assignment and reassignment actions
- conflict previews and decision support
- reschedule flow
- cancellation flow
- recurring rule behavior

#### Mobile and EVV

Validate:

- mobile login
- today-work visibility
- visit detail loading
- start-visit and end-visit
- checklist save
- quick notes
- artifact upload indicators
- incident report submission
- message center visibility
- clock-in, clock-out, missed visit, and exception flows

#### Documentation and review

Validate:

- template and task-library management
- visit-note draft save
- submission validation
- attachment linking
- printable summary
- review queue assignment
- completeness results
- return-for-fix and signoff

#### Compliance, patient events, care progression

Validate:

- compliance dashboard filtering
- patient compliance detail
- acknowledgment and certification actions
- patient-event create/update/resolve flows
- follow-up and escalation flows
- goal template and patient-goal management
- interventions and progress notes
- command-center summary routes

#### Revenue readiness and analytics

Validate:

- readiness list and detail
- export preview behavior
- authorization usage display
- command-center blocked or warning lanes
- analytics dashboard refresh
- operational drilldowns
- backlog, branch, utilization, readiness analytics
- leadership command-center visibility

### 11.6 What to look for during testing

- route guards behave correctly
- branch and role scope are respected
- loading, empty, failure, unauthorized, and read-only states are understandable
- mutation success states are explicit
- controlled operations mention audit relevance where appropriate
- links route into the correct downstream workspace
- sensitive context is minimized in summary views
- mobile and tablet layouts remain usable

---

## 12. Expected User Experience Principles

The implemented product consistently aims for:

- controlled workflows instead of ad hoc data entry
- role-aware visibility instead of one giant universal screen
- audit-aware treatment for sensitive operations
- branch-aware operational boundaries
- summary routes for coordinators and leadership
- mobile-first field execution patterns
- predictable loading, failure, and denied-state treatment

---

## 13. Documentation and Engineering References

The broader design and engineering context lives in:

- `Requirements.txt`
- `Epic1BE.txt` through `Epic15BE.txt`
- `Epic1FE.txt` through `Epic15FE.txt`
- `docs/architecture/adr/`
- `docs/architecture/standards/`
- backend `README.md`
- frontend code in `/Users/shiva/Documents/GitHub/homehealthcarefe/src`

This guide should be treated as the product-level implementation map. The ADRs and standards remain the source of truth for detailed engineering guardrails and domain standards.

---

## 14. Current State Summary

As implemented through Epic 15, HomeHealthCare is a role-aware, multi-tenant, branch-scoped home-based care operations platform with:

- secure authentication and administration
- configuration and catalog management
- patient and caregiver records
- scheduling and field execution
- EVV and visit exceptions
- documentation and coordination
- QA review and compliance workflows
- patient-event and goal tracking
- revenue readiness
- leadership analytics

The product already covers the core operating system needed for agency operations, field execution, review, readiness, and analytics. Future work should build from this foundation rather than redefining it.
