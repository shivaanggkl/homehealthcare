# Epic 1 and Epic 2 Manual E2E Guide

This guide is for manual end-to-end validation of the current backend in this repository and the frontend app in `/Users/shiva/Documents/GitHub/homehealthcarefe`.

Use it to compare:

- requirement intent
- frontend behavior
- backend/API behavior
- audit/security behavior

This guide is optimized for local development with:

- backend: `/Users/shiva/Documents/GitHub/homehealthcare`
- frontend: `/Users/shiva/Documents/GitHub/homehealthcarefe`
- backend URL: `http://localhost:8080`
- frontend URL: `http://localhost:5173`

## 1. Preconditions

Before testing, make sure you have:

- Java 21
- Node/npm
- backend dependencies resolved
- frontend dependencies resolved

Important local-dev behavior in this codebase:

- the backend uses in-memory H2 by default, so seeded test data is lost on backend restart
- browser CORS must allow `http://localhost:5173`
- secure cookies do not round-trip on plain `http://localhost`, so frontend localhost testing should use the built-in dev token fallback

## 2. Start Backend

Start the backend with:

- CORS enabled for the frontend origin
- a seeded `AGENCY_OWNER`

Run:

```bash
cd /Users/shiva/Documents/GitHub/homehealthcare

./gradlew bootRun --args='--security.web.allowed-origins=http://localhost:5173 --app.dev-bootstrap.owner.enabled=true --app.dev-bootstrap.owner.internal-super-admin-email=ops@platform.local --app.dev-bootstrap.owner.agency-name=North Star Home Care --app.dev-bootstrap.owner.agency-slug=north-star-home-care --app.dev-bootstrap.owner.agency-timezone=America/Chicago --app.dev-bootstrap.owner.agency-contact-email=hello@northstar.example --app.dev-bootstrap.owner.owner-first-name=Alicia --app.dev-bootstrap.owner.owner-last-name=Owner --app.dev-bootstrap.owner.owner-email=alicia.owner@northstar.example --app.dev-bootstrap.owner.owner-phone=+1 312 555 0101 --app.dev-bootstrap.owner.owner-password=StartPassword1!'
```

Expected backend startup result:

- Flyway migrations run successfully
- the backend starts on `http://localhost:8080`
- logs include the dev bootstrap success message from [DevAgencyOwnerBootstrapRunner.java](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/platform/provisioning/application/DevAgencyOwnerBootstrapRunner.java)

Seeded owner credentials:

- email: `alicia.owner@northstar.example`
- password: `StartPassword1!`

## 3. Start Frontend

Run:

```bash
cd /Users/shiva/Documents/GitHub/homehealthcarefe
npm install
npm run dev
```

Expected frontend startup result:

- dev server starts on `http://localhost:5173`
- opening the app shows the login screen

## 4. Browser Setup Before Login

Use a fresh incognito/private window for the cleanest run.

If you previously tested with stale cookies or tokens, clear:

- browser cookies for `localhost`
- local storage for `http://localhost:5173`

Expected pre-login behavior:

- `GET /api/auth/session` returns `401`
- frontend still shows login normally

This is expected and not a failure.

## 5. Login Validation

Open `http://localhost:5173/login`.

Enter:

- email: `alicia.owner@northstar.example`
- password: `StartPassword1!`

For localhost testing, keep this checkbox enabled:

- `Store token fallback locally for plain localhost development`

Expected frontend result:

- login succeeds without a raw browser error
- the app routes through `/app` and then immediately redirects to the role-based default landing page
- for the seeded `AGENCY_OWNER`, the expected landing page is `/app/settings/security`
- no generic `403` or CORS error appears

Expected backend/API result:

- `POST /api/auth/login` returns `200`
- secure cookies are set
- JSON includes `sessionId`, `accessToken`, and `refreshToken`
- audit event `USER_LOGGED_IN` is written

If MFA is enforced for the user, expected result changes to:

- frontend routes to `/login/mfa`
- backend returns `mfaRequired=true`

## 6. Epic 1 User Self-Service Tests

### 6.1 Self Profile

Open:

- `/app/settings/profile`

Do:

- update first name
- update last name
- update phone
- update preferred language
- update time zone

Expected FE behavior:

- form loads current values
- save button completes successfully
- success feedback is shown
- reloading the page shows the saved values

Expected BE behavior:

- `GET /api/me/profile` returns current profile
- `PUT /api/me/profile` returns updated profile
- invalid language or invalid timezone returns `400`
- audit event is written

### 6.2 Change Password

Open:

- `/app/settings/password`

Do:

- confirm password policy is visible
- enter current password
- enter a valid new password
- optionally choose to invalidate other sessions
- save

Expected FE behavior:

- password policy is shown from backend values
- weak password errors are shown clearly
- wrong current password is shown clearly
- success feedback is shown

Expected BE behavior:

- `GET /api/auth/password-policy` returns policy summary and flags
- `POST /api/auth/change-password` returns success
- audit event `PASSWORD_CHANGED` is written
- other sessions are revoked if requested

### 6.3 MFA Enrollment

Open:

- `/app/settings/mfa`

Do:

- review current MFA status
- enter current password
- start enrollment
- scan QR code or use manual entry key
- submit a valid TOTP code

Expected FE behavior:

- MFA status loads at page open
- QR code and manual key appear after start
- recovery codes are shown once
- confirm success message appears
- status refresh shows MFA enabled

Expected BE behavior:

- `GET /api/auth/mfa/status` returns current MFA state
- `POST /api/auth/mfa/enrollment/start` returns enrollment token, secret URI, recovery codes
- `POST /api/auth/mfa/enrollment/confirm` enables MFA
- audit events are written for MFA enrollment

### 6.4 Logout

Do:

- use the logout action from the app shell

Expected FE behavior:

- user is redirected to `/login?loggedOut=1`
- protected routes are no longer accessible

Expected BE behavior:

- `POST /api/auth/logout` returns logout redirect handling
- current session is revoked
- logout audit event is written

## 7. Epic 1 Authentication Recovery Tests

### 7.1 Forgot Password

Open:

- `/forgot-password`

Do:

- submit the seeded owner email

Expected FE behavior:

- generic success messaging appears
- UI does not reveal whether the email exists

Expected BE behavior:

- `POST /api/auth/forgot-password` returns success-safe response
- reset request is audited
- reset token is created with expiry

### 7.2 Reset Password

This flow requires a valid reset token. In local dev, obtain it from your local helper path or DB/log workflow.

Open:

- `/reset-password?token=<token>`

Do:

- review password rules
- enter a valid new password
- optionally revoke existing sessions
- submit

Expected FE behavior:

- expired or invalid token states show controlled UI
- valid reset completes and routes the user back to login

Expected BE behavior:

- `POST /api/auth/reset-password` enforces token validity and password policy
- audit event `PASSWORD_RESET_COMPLETED` is written

## 8. Epic 1 Admin User Management Tests

Use the seeded `AGENCY_OWNER`.

### 8.1 User Directory

Open:

- `/app/admin/users`

Do:

- search by name/email
- filter by status
- filter by role
- filter by branch
- page forward/backward

Expected FE behavior:

- user list loads with filters and pagination
- visible columns include invite state, last login, and MFA status
- loading and empty states are controlled

Expected BE behavior:

- `GET /api/users` supports paging and filtering
- unauthorized roles get `403`

### 8.2 Invite User

Open:

- the invite user flow from directory/admin navigation

Do:

- invite a branch-scoped user
- assign one or more branches where appropriate

Expected FE behavior:

- form validates required fields
- success feedback appears after invite

Expected BE behavior:

- `POST /api/users/invitations` creates or replaces a pending invite
- invite audit event is written

### 8.3 Accept Invitation

This requires a valid invitation token from local dev bootstrap/testing workflow.

Open:

- `/accept-invitation?token=<token>`

Do:

- verify invite details render
- set password
- submit

Expected FE behavior:

- invalid, expired, and used invitations show controlled states
- valid invitation completes and leads into login

Expected BE behavior:

- `GET /api/invitations/{token}` returns invitation details
- `POST /api/invitations/{token}/accept` activates the user
- invite acceptance is audited

### 8.4 Edit User

From the user directory:

Do:

- edit name
- edit phone
- edit role
- edit branch assignments

Expected FE behavior:

- editable fields load into the form cleanly
- restricted role transitions are blocked with clear error messaging

Expected BE behavior:

- `PUT /api/users/{userId}` updates allowed fields only
- protected/restricted edits return `403` or `409` as appropriate
- audit events for role and branch assignment changes are written

### 8.5 User Status Management

From the user directory/admin flow:

Do:

- lock a user
- suspend a user
- deactivate a user
- reactivate a user

Expected FE behavior:

- status actions are visible only where permitted
- confirmation is shown for destructive actions
- success/error feedback is explicit

Expected BE behavior:

- `PUT /api/users/{userId}/status` updates status
- suspended and deactivated users lose active sessions
- audit event `USER_STATUS_CHANGED` is written

### 8.6 Audit Log

Open:

- `/app/admin/audit`

Do:

- load audit events
- filter by action
- filter by date range
- page through results
- export CSV if available in your UI flow

Expected FE behavior:

- audit table renders with stable filtering and paging
- deep links from setup/security pages can prefill filters

Expected BE behavior:

- `GET /api/audit-events` returns paged events
- `GET /api/audit-events/export` returns export payload/file

## 9. Epic 1 Admin Security Tests

### 9.1 Agency MFA Policy

Open:

- `/app/settings/admin-mfa-policy`

Do:

- set MFA policy to `OFF`
- set MFA policy to `ALL_USERS`
- set MFA policy to `SELECTED_ROLES`

Expected FE behavior:

- policy loads and saves correctly
- unauthorized users see controlled denied state

Expected BE behavior:

- `GET /api/security/mfa-policy` returns current policy
- `PUT /api/security/mfa-policy` persists updates
- audit event is written

### 9.2 Admin Notification Preferences

Open:

- `/app/settings/admin-notifications`

Do:

- enable/disable repeated failed login alerts
- enable/disable locked account alerts
- enable/disable new admin alerts

Expected FE behavior:

- toggles reflect saved state after reload

Expected BE behavior:

- `GET /api/security/admin-notifications` returns current values
- `PUT /api/security/admin-notifications` saves preferences
- preference changes are audited

### 9.3 Agency Settings

Open:

- `/app/settings/agency`

Do:

- edit agency name
- edit slug if supported by the UI
- edit timezone
- edit contact email

Expected FE behavior:

- owner-only route
- values reload correctly after save

Expected BE behavior:

- `GET /api/agency/settings` returns current agency settings
- `PUT /api/agency/settings` updates them
- audit event is written

### 9.4 Branch Management

Open:

- `/app/admin/branches`

Do:

- create a branch
- edit a branch
- deactivate a branch

Expected FE behavior:

- list loads with search/filter support if provided
- create/edit forms are clear and stable
- owner-only actions are visually restricted where needed

Expected BE behavior:

- `GET /api/branches` returns branches
- `POST /api/branches` creates
- `PUT /api/branches/{branchId}` updates
- `DELETE /api/branches/{branchId}` deactivates
- audit events are written

## 10. Epic 1 Route and Permission Validation

Test at least these access profiles if you can provision them:

- `AGENCY_OWNER`
- `BRANCH_ADMIN`
- `READ_ONLY_AUDITOR`
- lower-privilege operational user

Expected FE behavior:

- unauthorized menu items are hidden or disabled
- deep links show controlled access denied state
- `/app` landing behavior changes by role/access profile

Expected BE behavior:

- backend still blocks unauthorized actions with `403`
- branch-limited roles cannot operate outside assigned branches

## 11. Epic 2 Setup Overview and Catalog Tests

Use the seeded owner.

Open:

- `/app/setup`

Expected FE behavior:

- a setup overview/dashboard is visible
- configuration modules are grouped and navigable
- counts or status summaries reflect current setup state

Expected BE behavior:

- all Epic 2 setup APIs are reachable through the authenticated owner session

### 11.1 Agency Profile

Open:

- `/app/setup/profile`

Do:

- edit display name
- edit legal name
- edit phone/address
- edit operations/support contacts
- edit default timezone and locale

Expected FE behavior:

- form loads existing profile
- owner-only access is respected

Expected BE behavior:

- `GET /api/agency/profile`
- `PUT /api/agency/profile`
- audit event is written

### 11.2 Service Lines

Open:

- `/app/setup/catalog/service-lines`

Do:

- create a service line
- edit it
- try a duplicate code/name case if supported
- deactivate it

Expected FE behavior:

- list and form are backend-backed
- duplicate/dependency conflicts show clear error messaging

Expected BE behavior:

- `GET /api/service-lines`
- `POST /api/service-lines`
- `PUT /api/service-lines/{id}`
- `DELETE /api/service-lines/{id}`
- duplicate returns `409`

### 11.3 Visit Types

Open:

- `/app/setup/catalog/visit-types`

Do:

- create a visit type
- link it to a service line
- edit default duration and billable flag
- deactivate it

Expected FE behavior:

- linked service line options load correctly
- filters and search work

Expected BE behavior:

- `GET /api/visit-types`
- `POST /api/visit-types`
- `PUT /api/visit-types/{id}`
- `DELETE /api/visit-types/{id}`

### 11.4 Workforce Catalog

Open:

- `/app/setup/workforce/catalogs`

Do:

- create/edit/deactivate caregiver skills
- create/edit/deactivate caregiver certifications
- test `expirationRequired`

Expected FE behavior:

- skills and certifications are clearly separated
- both lists persist changes correctly

Expected BE behavior:

- `GET/POST/PUT/DELETE /api/caregiver-skills`
- `GET/POST/PUT/DELETE /api/caregiver-certifications`

## 12. Epic 2 Advanced Configuration Tests

### 12.1 Task Templates

Open:

- `/app/setup/templates/tasks`

Do:

- create template
- set category
- optionally link service line and visit type
- edit and deactivate

Expected BE behavior:

- `GET/POST/PUT/DELETE /api/task-templates`

### 12.2 Documentation Templates

Open:

- `/app/setup/templates/documentation`

Do:

- create draft
- edit JSON/template payload
- save
- create next version
- publish

Expected FE behavior:

- invalid JSON is surfaced clearly
- version and publish status are visible

Expected BE behavior:

- `GET/POST/PUT /api/documentation-templates`
- `POST /api/documentation-templates/{id}/version`
- `POST /api/documentation-templates/{id}/publish`

### 12.3 Branch Policies

Open:

- `/app/setup/policies/branches`

Do:

- create branch-specific policy
- create fallback-to-agency-default policy
- edit and deactivate

Expected BE behavior:

- `GET/POST/PUT/DELETE /api/branch-policies`

### 12.4 Alert Rules

Open:

- `/app/setup/policies/alerts`

Do:

- create agency-wide rule
- create branch-specific rule
- set delivery modes
- edit configuration payload
- deactivate

Expected BE behavior:

- `GET/POST/PUT/DELETE /api/alert-rules`

### 12.5 Mileage and Pay Settings

Open:

- `/app/setup/compensation/mileage-pay`

Do:

- edit agency default setting
- create branch override
- check effective values

Expected BE behavior:

- `GET /api/mileage-pay-settings`
- `PUT /api/mileage-pay-settings/default`
- `PUT /api/mileage-pay-settings/branches/{branchId}`

## 13. Epic 2 Conflict and Hardening Tests

Specifically validate:

- duplicate create/update returns controlled conflict UI
- dependency-protected deactivation returns controlled conflict UI
- destructive actions show confirmation UX
- audit-sensitive actions can be cross-checked in audit log

Expected backend behavior:

- dependency conflicts return `409`
- unauthorized actions return `403`
- audit events are written for major configuration changes

## 14. Expected Localhost-Specific Behavior

These are normal in local dev:

- `/api/auth/session` returns `401` before login
- cookie-only auth is not sufficient on plain `http://localhost`
- the frontend localhost token fallback should be enabled during login

These are not normal:

- `403 Invalid CORS request`
  - backend was started without `--security.web.allowed-origins=http://localhost:5173`
- repeated `403` login failures with valid credentials after CORS is fixed
  - usually stale cookies/local storage or a frontend request-shape issue

## 15. Quick Pass/Fail Checklist

Epic 1 is aligned if:

- login works
- password flows work
- MFA flows work
- user directory/invite/edit/status work
- audit log works
- agency/branch/admin security settings work
- unauthorized routes are blocked in FE and BE

Epic 2 is aligned if:

- all setup modules load
- create/edit/deactivate works across catalogs and templates
- dependency/conflict cases are controlled
- setup overview reflects current configuration state

## 16. When Comparing Against Requirements

For each story, compare three things:

1. FE shape
   - correct page exists
   - correct fields/actions are visible
   - correct success/error/denied states are shown

2. BE contract
   - correct API is called
   - correct status code is returned
   - correct persistence side effect happens

3. Security/audit behavior
   - unauthorized access is denied
   - destructive or security-sensitive actions are audited
   - session/auth flows remain secure and predictable
