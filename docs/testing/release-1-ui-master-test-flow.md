# Release 1 UI Master Test Flow

This is the single, sequenced manual UI test run for Release 1.

Use this document exactly in order.

Do not skip ahead.

Many later screens only become meaningful after earlier setup and workflow data exists.

This guide covers:

- backend + frontend startup
- exact starter users
- exact core test dataset
- auth and security
- setup and admin
- patient, workforce, scheduling
- mobile, EVV, documentation, messaging
- review, compliance, patient events, goals
- revenue readiness and analytics
- final permission and audit checks

This guide assumes:

- backend repo: `/Users/shiva/Documents/GitHub/homehealthcare`
- frontend repo: `/Users/shiva/Documents/GitHub/homehealthcarefe`
- backend URL: `http://localhost:8080`
- frontend URL: `http://localhost:5173`
- backend uses in-memory H2, so all data resets after backend restart

How to use this document:

- treat the direct URL in each step as the source of truth
- use the sidebar label only as a convenience if you can already see it
- if a sidebar label is missing or the navigation grouping looks different, paste the direct URL into the browser and continue
- routes with placeholders such as `:visitId`, `:patientId`, `:threadId`, `:goalId`, `:authorizationId`, or `:documentationRecordId` must be opened from the workspace after you create or discover a real record

Current access behavior after the owner-access fix:

- authenticated sessions now prefer backend current access over any stale frontend role override stored in local browser storage
- the seeded owner should land on `/app/settings/security`
- the seeded owner should be able to open `/app/setup/profile`

If your browser still shows old behavior from before the fix:

- clear local storage for `http://localhost:5173`
- sign in again
- then continue with this document

## 1. Start Backend

Open Terminal window 1.

Run:

```bash
cd /Users/shiva/Documents/GitHub/homehealthcare

./gradlew bootRun --args='
--security.web.allowed-origins=http://localhost:5173
--app.dev-bootstrap.owner.enabled=true
--app.dev-bootstrap.owner.internal-super-admin-email=ops@platform.local
--app.dev-bootstrap.owner.agency-name=North Star Home Care
--app.dev-bootstrap.owner.agency-slug=north-star-home-care
--app.dev-bootstrap.owner.agency-timezone=America/Chicago
--app.dev-bootstrap.owner.agency-contact-email=hello@northstar.example
--app.dev-bootstrap.owner.owner-first-name=Alicia
--app.dev-bootstrap.owner.owner-last-name=Owner
--app.dev-bootstrap.owner.owner-email=alicia.owner@northstar.example
--app.dev-bootstrap.owner.owner-phone=+1-312-555-0101
--app.dev-bootstrap.owner.owner-password=StartPassword1!
'
```

Do not move on until backend startup is complete.

Look for this log line:

```text
Dev bootstrap owner ready for agency 'North Star Home Care' (slug='north-star-home-care'). Login email='alicia.owner@northstar.example' password='StartPassword1!'.
```

Also confirm the backend is listening on port `8080`.

If you do not see that bootstrap line:

- stop here
- fix backend startup first
- do not continue with frontend testing

## 2. Start Frontend

Open Terminal window 2.

Run:

```bash
cd /Users/shiva/Documents/GitHub/homehealthcarefe
VITE_API_BASE_URL=http://localhost:8080 npm install
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

Do not move on until frontend startup is complete.

Confirm:

- Vite prints a local URL for `http://localhost:5173`
- opening `http://localhost:5173` in the browser takes you to the login page

## 3. Prepare Browser

Use a fresh incognito/private browser window.

Before testing:

- clear cookies for `localhost`
- clear local storage for `http://localhost:5173`

Then open:

- `http://localhost:5173/login`

## 4. Confirm Login Page Is Correct

On the login page, confirm you can see:

- `MavieHealth` branding
- the headline `Built for modern home health operations.`
- the section title `Secure Sign In`
- the heading `Welcome back`
- an `Email` field
- a `Password` field
- a `Show` button beside password
- a `Sign in` button
- a `Forgot your password?` link
- an `Accept invitation` link

Expand `Local development tools`.

Make sure this checkbox is enabled:

- `Store token fallback locally when secure cookies do not round-trip on localhost`

## 5. Test Login Failure First

Stay on `/login`.

Enter:

- email: `alicia.owner@northstar.example`
- password: `WrongPassword1!`

Click:

- `Sign in`

Expected result:

- login does not proceed
- you stay on the login page
- you see a red error block starting with `Sign-in failed.`

## 6. Test Successful Login

Still on `/login`, replace the password with:

- password: `StartPassword1!`

Click:

- `Sign in`

Expected result:

- login succeeds
- you are redirected away from `/login`
- the seeded owner lands on `http://localhost:5173/app/settings/security`

When the app shell loads, confirm:

- left side: sidebar with `MavieHealth`
- left side sections such as `Workspace`, `Security`, `Configuration`, `People`, or similar role-based groups
- main content top area: `Active User`
- main content title: `Session-aware shell`
- top right area shows `User ID` and `Session ID`

If login fails:

- stop here
- do not continue until owner login works cleanly

## 6.1 Go To The First Dataset Setup Screen

You are currently on:

- `http://localhost:5173/app/settings/security`

Do this next:

1. look at the left sidebar
2. find the section labeled `Agency Setup`
3. inside `Agency Setup`, click `Agency Setup`

If you do not want to use the sidebar, open this URL directly:

- `http://localhost:5173/app/setup`

Expected result:

- the main area changes from the security page to the setup workspace
- you are now in the configuration/setup part of the product
- this is the starting area for creating the shared dataset used by the rest of Release 1 testing

## 7. Core Test Users And Data You Must Create

You will build one shared dataset and keep reusing it.

Start this section only after you have moved from `/app/settings/security` to `/app/setup`.

For the rest of this section, stay in the left sidebar group labeled `Agency Setup`.

Use this exact order.

Do not jump around.

If a sidebar link is hard to find, type the direct URL into the browser and continue.

### 7.0 Exact Page-By-Page Shared Dataset Setup

You should currently be on:

- `http://localhost:5173/app/setup`

Now do this in order:

1. in the left sidebar under `Agency Setup`, click `Agency Profile`
2. then click `Service Lines`
3. then click `Visit Types`
4. then click `Workforce Catalogs`
5. then click `Task Templates`
6. then click `Documentation Templates`
7. then click `Branch Policies`
8. then click `Alert Rules`
9. then click `Mileage & Pay`
10. then go to the left sidebar section `People & Audit`
11. click `Users`
12. send all invitations
13. then go to the left sidebar section `Patients`
14. click `Patient Workspace`
15. create both patients
16. then go to the left sidebar section `Workforce`
17. click `Workforce Workspace`
18. create both caregivers
19. then go to the left sidebar section `Scheduling`
20. click `Scheduling Workspace`
21. create the three visits

Use these direct URLs if you prefer:

- `http://localhost:5173/app/setup/profile`
- `http://localhost:5173/app/setup/catalog/service-lines`
- `http://localhost:5173/app/setup/catalog/visit-types`
- `http://localhost:5173/app/setup/workforce/catalogs`
- `http://localhost:5173/app/setup/templates/tasks`
- `http://localhost:5173/app/setup/templates/documentation`
- `http://localhost:5173/app/setup/policies/branches`
- `http://localhost:5173/app/setup/policies/alerts`
- `http://localhost:5173/app/setup/compensation/mileage-pay`
- `http://localhost:5173/app/admin/users`
- `http://localhost:5173/app/patients`
- `http://localhost:5173/app/workforce`
- `http://localhost:5173/app/scheduling`

### 7.0.1 What To Create On Each Screen Before Moving On

At `Agency Profile`:

Open:

- `http://localhost:5173/app/setup/profile`

You should see:

- `Current profile snapshot`
- `Edit agency profile`
- a button labeled `Save agency profile`

Enter exactly:

- `Display name` = `North Star Home Care`
- `Legal name` = `North Star Home Care LLC`
- `Primary phone` = `+1-312-555-0101`
- `Primary address` = `500 W Madison St, Chicago, IL 60661`
- `Operations contact name` = `Alicia Owner`
- `Operations contact email` = `alicia.owner@northstar.example`
- `Support contact name` = `North Star Support`
- `Support contact email` = `support@northstar.example`
- `Default timezone` = `America/Chicago`
- `Default locale` = `en-US`

Click:

- `Save agency profile`

Do not move on until:

- you see `Agency profile saved successfully.`
- the `Current profile snapshot` panel shows the same saved values instead of `Not set yet`

At `Service Lines`:

Open:

- `http://localhost:5173/app/setup/catalog/service-lines`

You should see:

- `Create service line`
- fields `Name`, `Code`, `Description`, `Display order`

Create record 1:

- `Name` = `Skilled Nursing`
- `Code` = `SN`
- `Description` = `Routine and skilled nursing services for home health patients.`
- `Display order` = `10`

Click:

- `Create service line`

Create record 2:

- `Name` = `Therapy`
- `Code` = `THERAPY`
- `Description` = `Therapy services including physical therapy follow-up visits.`
- `Display order` = `20`

Click:

- `Create service line`

Do not move on until both rows appear in the table:

- `Skilled Nursing`
- `Therapy`

At `Visit Types`:

Open:

- `http://localhost:5173/app/setup/catalog/visit-types`

You should see:

- `Create visit type`
- fields `Service line`, `Name`, `Code`, `Description`, `Default duration (minutes)`, `Display order`
- a checkbox `Billable by default`

Create record 1:

- `Service line` = `Skilled Nursing`
- `Name` = `RN Routine Visit`
- `Code` = `RN-ROUTINE`
- `Description` = `Standard routine nursing visit for ongoing home health care.`
- `Default duration (minutes)` = `60`
- `Display order` = `10`
- keep `Billable by default` checked

Click:

- `Create visit type`

Create record 2:

- `Service line` = `Therapy`
- `Name` = `PT Follow-up Visit`
- `Code` = `PT-FOLLOWUP`
- `Description` = `Follow-up physical therapy visit for mobility and recovery.`
- `Default duration (minutes)` = `45`
- `Display order` = `20`
- keep `Billable by default` checked

Click:

- `Create visit type`

Do not move on until both rows appear in the table:

- `RN Routine Visit`
- `PT Follow-up Visit`

At `Workforce Catalogs`:

Open:

- `http://localhost:5173/app/setup/workforce/catalogs`

You should first be on the `Caregiver skills` mode.

Create skill 1:

- `Name` = `Wound Care`
- `Code` = `WOUND`
- `Description` = `Can perform wound assessment and wound dressing tasks.`

Click:

- `Create skill`

Create skill 2:

- `Name` = `Medication Administration`
- `Code` = `MEDADMIN`
- `Description` = `Can administer and document medications during scheduled visits.`

Click:

- `Create skill`

Then click:

- `Certifications`

Create certification 1:

- `Name` = `Illinois RN License`
- `Code` = `IL-RN`
- `Description` = `Registered nurse state licensure for Illinois caregivers.`
- keep `Expiration required` checked

Click:

- `Create certification`

Do not move on until you can see at least:

- skill `Wound Care`
- skill `Medication Administration`
- certification `Illinois RN License`

At `Task Templates`:

Open:

- `http://localhost:5173/app/setup/templates/tasks`

You should see:

- `Create task template`
- fields `Service line`, `Visit type`, `Name`, `Code`, `Category`, `Display order`, `Description`

Create:

- `Service line` = `Skilled Nursing`
- `Visit type` = `RN Routine Visit`
- `Name` = `Daily Visit Checklist`
- `Code` = `DAILY-VISIT`
- `Category` = `CLINICAL`
- `Display order` = `10`
- `Description` = `Confirm identity, complete assessment, perform ordered care, review medications, and close visit.`

Click:

- `Create template`

Do not move on until `Daily Visit Checklist` appears in the table

At `Documentation Templates`:

Open:

- `http://localhost:5173/app/setup/templates/documentation`

You should see:

- `Create draft`
- fields `Name`, `Code`, `Template type`, `Display order`, `Structured definition JSON`

Create the draft with:

- `Name` = `Skilled Nursing Visit Note`
- `Code` = `SN-VISIT-NOTE`
- `Template type` = `VISIT_NOTE`
- `Display order` = `10`
- `Structured definition JSON` =

```json
{
  "sections": [
    {
      "id": "visit-summary",
      "title": "Visit Summary",
      "fields": ["subjective", "objective", "assessment", "plan"]
    }
  ]
}
```

Click:

- `Create draft`

Do not move on until `Skilled Nursing Visit Note` appears in the table

At `Branch Policies`:

Open:

- `http://localhost:5173/app/setup/policies/branches`

This screen creates policy records, not branch master records.

If the `Branch` dropdown is empty, do this first in a new tab:

- open `http://localhost:5173/app/admin/branches`
- create branch 1 with:
  - `Name` = `Downtown Branch`
  - `Code` = `DT`
  - `Address` = `500 W Madison St, Chicago, IL 60661`
  - `Timezone` = `America/Chicago`
- click `Create branch`
- create branch 2 with:
  - `Name` = `North Branch`
  - `Code` = `NORTH`
  - `Address` = `100 E Oak St, Chicago, IL 60611`
  - `Timezone` = `America/Chicago`
- click `Create branch`
- go back to `http://localhost:5173/app/setup/policies/branches`

After the real branches exist, use this page to seed one usable branch policy for each branch.

Create policy 1:

- `Branch` = `Downtown Branch`
- `Policy key` = `scheduling.window`
- `Display order` = `10`
- leave `Fallback to agency default` unchecked
- `Settings payload JSON` =

```json
{
  "minutes": 30
}
```

- `Effective from` = `2026-04-01T00:00:00-05:00`
- `Effective to` = `2026-12-31T23:59:59-06:00`

Click:

- `Create policy`

Create policy 2:

- `Branch` = `North Branch`
- `Policy key` = `scheduling.window`
- `Display order` = `20`
- leave `Fallback to agency default` unchecked
- `Settings payload JSON` =

```json
{
  "minutes": 45
}
```

- `Effective from` = `2026-04-01T00:00:00-05:00`
- `Effective to` = `2026-12-31T23:59:59-06:00`

Click:

- `Create policy`

Do not move on until both branch policy rows appear in the table

At `Alert Rules`:

Open:

- `http://localhost:5173/app/setup/policies/alerts`

You should see:

- `Create alert rule`
- fields `Scope`, `Rule type`, `Name`, `Display order`, `Configuration payload JSON`
- delivery checkboxes for `Email delivery`, `SMS delivery`, and `In-app delivery`

Create:

- `Scope` = `Agency-wide`
- `Rule type` = `SECURITY_EVENT`
- `Name` = `Repeated failed login escalation`
- `Display order` = `10`
- `Configuration payload JSON` =

```json
{
  "threshold": 5,
  "windowMinutes": 15,
  "severity": "HIGH"
}
```

- check `Email delivery`
- leave `SMS delivery` unchecked
- check `In-app delivery`

Click:

- `Create rule`

Do not move on until `Repeated failed login escalation` appears in the table

At `Mileage & Pay`:

Open:

- `http://localhost:5173/app/setup/compensation/mileage-pay`

In `Agency default`, enter:

- `Reimbursement strategy` = `STANDARD_RATE`
- `Mileage rate` = `0.6700`
- check `Travel pay enabled`
- `Visit type adjustments JSON` =

```json
[
  {
    "visitTypeCode": "RN-ROUTINE",
    "payPerVisit": 85.00
  }
]
```

- `Effective from` = `2026-04-01T00:00:00-05:00`
- `Effective to` = `2026-12-31T23:59:59-06:00`

Click:

- `Save agency default`

Then in `Branch override`, enter:

- `Branch` = `Downtown Branch`
- `Reimbursement strategy` = `CUSTOM_RATE`
- `Mileage rate` = `0.7200`
- check `Travel pay enabled`
- `Visit type adjustments JSON` =

```json
[
  {
    "visitTypeCode": "RN-ROUTINE",
    "payPerVisit": 90.00
  }
]
```

- `Effective from` = `2026-04-01T00:00:00-05:00`
- `Effective to` = `2026-12-31T23:59:59-06:00`

Click:

- `Save branch override`

Do not move on until the screen shows saved mileage settings without an error

At `Users`:

- create or invite every user listed in section `7.3`
- do not move on until each one appears in the user or invitation list

At `Patient Workspace`:

- create both patients listed in section `7.4`
- do not move on until both appear in the patient list

At `Workforce Workspace`:

- create both caregivers listed in section `7.5`
- link `Carla Caregiver` to `carla.caregiver@northstar.example` if the UI supports linking
- do not move on until both caregiver records appear in the list

At `Scheduling Workspace`:

- create the three visits listed in section `7.6`
- do not move on until all three visits appear in the scheduling view
- these visits are required for the mobile, EVV, documentation, review, and revenue tests later
Create exactly this:

### 7.1 Branches

- `Downtown Branch`
- `North Branch`

### 7.2 Setup Catalog Data

- Service line: `Skilled Nursing`
- Service line: `Therapy`
- Visit type: `RN Routine Visit`
- Visit type: `PT Follow-up Visit`
- Task template: `Daily Visit Checklist`
- Documentation template: `Skilled Nursing Visit Note`
- Alert rule: `Repeated failed login escalation`
- Mileage pay: any valid default

### 7.3 Users To Invite

- `brenda.branchadmin@northstar.example`
  - role: `BRANCH_ADMIN`
  - branch: `Downtown Branch`
- `sam.scheduler@northstar.example`
  - role: `SCHEDULER_COORDINATOR`
  - branch: `Downtown Branch`
- `carla.caregiver@northstar.example`
  - role: `CAREGIVER`
  - branch: `Downtown Branch`
- `riley.reviewer@northstar.example`
  - role: `QA_CLINICAL_REVIEWER`
  - branch: `Downtown Branch`
- `bill.billing@northstar.example`
  - role: `BILLING_BACK_OFFICE`
  - branch: `Downtown Branch`
- `audrey.audit@northstar.example`
  - role: `READ_ONLY_AUDITOR`
  - agency-wide read

Use password `StartPassword1!` for all invited users when they accept invitations.

### 7.4 Patients

- `Mary Johnson`
  - DOB: `1950-04-12`
  - branch: `Downtown Branch`
  - service line: `Skilled Nursing`
  - payer: `Medicare Advantage Demo`
  - authorization: `AUTH-MJ-001`
- `Robert Evans`
  - DOB: `1946-09-03`
  - branch: `North Branch`
  - service line: `Therapy`

### 7.5 Caregivers

- `Carla Caregiver`
  - linked to `carla.caregiver@northstar.example`
  - branch: `Downtown Branch`
- `Ramon Relief`
  - branch: `Downtown Branch`

### 7.6 Visits

- Visit A
  - patient: `Mary Johnson`
  - caregiver: `Carla Caregiver`
  - visit type: `RN Routine Visit`
  - branch: `Downtown Branch`
  - end state: fully completed with EVV and documentation
- Visit B
  - patient: `Mary Johnson`
  - caregiver: `Ramon Relief`
  - visit type: `RN Routine Visit`
  - branch: `Downtown Branch`
  - end state: rescheduled or cancelled
- Visit C
  - patient: `Mary Johnson`
  - branch: `Downtown Branch`
  - end state: open shift or blocker scenario if possible

## 8. Phase Order

Follow this exact order:

1. Owner self-service security
2. Setup and catalogs
3. Branches and users
4. Accept invitations
5. Patients
6. Workforce
7. Scheduling
8. Mobile and EVV
9. Documentation
10. Messaging
11. Review
12. Compliance
13. Patient events
14. Care progression
15. Revenue readiness
16. Analytics
17. Audit and permissions

How to interpret route instructions in the phases below:

- if a route is a workspace landing route such as `/app/patients` or `/app/review`, you can usually reach it from the sidebar or by direct URL
- if a route contains a real record id such as `:visitId` or `:patientId`, do not paste the placeholder path directly; open the parent workspace first, then open a real record from the UI
- if a route opens an access-denied panel for the current logged-in user, treat that as a permission test result, not as a navigation mistake

## 9. Phase 1: Owner Self-Service Security

User:

- `alicia.owner@northstar.example`

### 9.1 Profile

Open:

- `http://localhost:5173/app/settings/profile`

What to look for:

- profile form fields for your own account
- a save/update action

Do:

1. change first name
2. change last name
3. change phone
4. change preferred language
5. change time zone
6. save
7. reload page

Expected:

- success message appears
- saved values remain after reload

### 9.2 Password

Open:

- `http://localhost:5173/app/settings/password`

What to look for:

- password policy details
- current password field
- new password field
- confirm password field if present

Do:

1. try a weak password
2. confirm validation blocks it
3. enter wrong current password
4. confirm error appears
5. enter valid new password
6. save successfully
7. log out
8. log back in with the new password
9. change the password back to `StartPassword1!`

Expected:

- policy is enforced
- invalid current password is rejected
- valid password change works

### 9.3 MFA

Open:

- `http://localhost:5173/app/settings/mfa`

What to look for:

- current MFA status
- start enrollment action

Do:

1. start MFA enrollment
2. verify QR code and manual key appear
3. enter a valid TOTP code
4. confirm success
5. record recovery codes
6. sign out
7. sign back in

Expected:

- you are redirected to `/login/mfa`
- valid TOTP completes login
- MFA status shows enabled after login

### 9.4 Sessions

Open:

- `http://localhost:5173/app/settings/sessions`

Do:

1. create a second active browser session for the owner
2. refresh this page
3. revoke one non-current session
4. verify the revoked session loses access

### 9.5 Security Admin Screens

Open these direct URLs one by one:

- `/app/settings/security`
- `/app/settings/admin-mfa-policy`
- `/app/settings/admin-notifications`

Expected:

- `/app/settings/security` opens for the seeded owner
- `/app/settings/admin-mfa-policy` opens for the seeded owner
- `/app/settings/admin-notifications` opens for the seeded owner

If any one of these does not open:

- stop and record a defect
- clear local storage for `http://localhost:5173`
- sign in again
- retry once before continuing

## 10. Phase 2: Setup And Catalogs

User:

- owner

Go to:

- `/app/setup`

From here or via direct URL, test each setup screen in this exact order:

1. `/app/setup/profile`
2. `/app/setup/catalog/service-lines`
3. `/app/setup/catalog/visit-types`
4. `/app/setup/workforce/catalogs`
5. `/app/setup/templates/tasks`
6. `/app/setup/templates/documentation`
7. `/app/setup/policies/branches`
8. `/app/setup/policies/alerts`
9. `/app/setup/compensation/mileage-pay`

For every setup screen:

1. confirm the page opens without console or network errors
2. confirm empty state or list state is understandable
3. create the required record
4. save
5. edit the same record
6. save again
7. reload page and confirm the record still exists

Required outputs before moving on:

- both branches created
- both service lines created
- both visit types created
- task template created
- documentation template created
- alert rule created
- mileage pay saved

## 11. Phase 3: User Directory And Invites

User:

- owner

Open:

- `/app/admin/users`

What to look for:

- searchable user list
- filter controls
- paging controls
- invite user entry point

Do:

1. search by owner email
2. filter by status
3. filter by role
4. filter by branch
5. clear filters

Then open:

- `/app/admin/users/invite`

On the invite page, you should see:

- fields `First name`, `Last name`, `Email`, `Phone`, `Agency role`
- branch checkboxes under `Branch assignments`
- button `Send invite`

Create these invites one by one using these exact values.

Invite 1:

- `First name` = `Brenda`
- `Last name` = `Branchadmin`
- `Email` = `brenda.branchadmin@northstar.example`
- `Phone` = `+1-312-555-0201`
- `Agency role` = `Branch Admin`
- under `Branch assignments`, check `Downtown Branch`
- click `Send invite`

Invite 2:

- `First name` = `Sam`
- `Last name` = `Scheduler`
- `Email` = `sam.scheduler@northstar.example`
- `Phone` = `+1-312-555-0202`
- `Agency role` = `Scheduler Coordinator`
- check `Downtown Branch`
- click `Send invite`

Invite 3:

- `First name` = `Carla`
- `Last name` = `Caregiver`
- `Email` = `carla.caregiver@northstar.example`
- `Phone` = `+1-312-555-0203`
- `Agency role` = `Caregiver`
- check `Downtown Branch`
- click `Send invite`

Invite 4:

- `First name` = `Riley`
- `Last name` = `Reviewer`
- `Email` = `riley.reviewer@northstar.example`
- `Phone` = `+1-312-555-0204`
- `Agency role` = `QA Clinical Reviewer`
- check `Downtown Branch`
- click `Send invite`

Invite 5:

- `First name` = `Bill`
- `Last name` = `Billing`
- `Email` = `bill.billing@northstar.example`
- `Phone` = `+1-312-555-0205`
- `Agency role` = `Billing Back Office`
- leave all branch boxes unchecked if the role is treated as agency-wide
- click `Send invite`

Invite 6:

- `First name` = `Audrey`
- `Last name` = `Audit`
- `Email` = `audrey.audit@northstar.example`
- `Phone` = `+1-312-555-0206`
- `Agency role` = `Read Only Auditor`
- leave all branch boxes unchecked if the role is treated as agency-wide
- click `Send invite`

For every invite, confirm the success card shows:

- the invited email address
- the role you selected
- the invite expiration

Then return to directory and test:

1. open `Brenda Branchadmin` in the directory
2. edit `Phone` to `+1-312-555-1201`
3. keep role as `BRANCH_ADMIN`
4. keep `Downtown Branch` assigned
5. click `Save changes`
6. open `Sam Scheduler`
7. change branch assignment from `Downtown Branch` to `Downtown Branch` plus `North Branch` if multi-branch selection is allowed
8. click `Save changes`
9. open `Bill Billing`
10. change status to `LOCKED`
11. verify status change success message
12. change status back to `ACTIVE`
13. verify status change success message
14. verify the edited phone and branch names appear in the list view after refresh

Finally open:

- `/app/admin/audit`

Verify you can find invite and status-change audit activity.

## 12. Phase 4: Accept Invitations

You need a real invitation token for each user.

Do not continue to later role testing until these users have accepted invites:

- `sam.scheduler@northstar.example`
- `carla.caregiver@northstar.example`
- `riley.reviewer@northstar.example`
- `bill.billing@northstar.example`
- `audrey.audit@northstar.example`

For each invited user:

1. obtain the invitation link from your local invite-token workflow
2. open `/accept-invitation?token=<real-token>`
3. confirm invite details render
4. enter `First name` exactly as invited
5. enter `Last name` exactly as invited
6. enter `Phone` exactly as invited
7. enter `Password` = `StartPassword1!`
8. enter `Confirm password` = `StartPassword1!`
9. submit
10. verify user can log in

Also test one invalid or expired invitation if possible.

Expected:

- invalid tokens show controlled UI
- valid tokens complete and return user to login flow

## 13. Phase 5: Patient Workspace

User:

- owner

Open:

- `/app/patients`

What to look for:

- patient workspace
- list or empty state
- create patient entry point

Create `Mary Johnson` first.

Click:

- `New patient`

You should land on:

- `/app/patients/new/demographics`

In `Create patient record`, enter exactly:

- `External reference` = `MRN-MJ-1001`
- `First name` = `Mary`
- `Middle name` = `E`
- `Last name` = `Johnson`
- `Preferred name` = `Mary`
- `Date of birth` = `1950-04-12`
- `Sex marker` = `F`
- `Primary phone` = `+1-312-555-0301`
- `Secondary phone` = `+1-312-555-0302`
- `Email` = `mary.johnson@example.test`
- `Language` = `en`
- `Notes summary` = `Primary patient for Release 1 workflow validation.`

Click:

- `Create patient`

After create, complete these patient sections:

1. demographics
2. contacts
3. address
4. eligibility
5. diagnoses
6. payer link
7. authorization
8. attachment upload if available

For `contacts`, open:

- `/app/patients/<maryPatientId>/contacts`

In `Add patient contact`, enter:

- `Full name` = `Elaine Johnson`
- `Relationship` = `Daughter`
- `Phone` = `+1-312-555-0310`
- `Email` = `elaine.johnson@example.test`
- `Address` = `500 W Madison St, Chicago, IL 60661`
- `Notes` = `Primary family contact for scheduling and emergencies.`
- check `Primary contact`
- check `Emergency contact`
- check `Responsible party`

Click:

- `Add contact`

For `address`, open:

- `/app/patients/<maryPatientId>/address`

If the address form is present, enter:

- line 1 / street = `500 W Madison St`
- line 2 = `Apt 12B`
- city = `Chicago`
- state = `IL`
- postal / zip = `60661`
- notes = `Primary service address for home visits.`

Click the save button shown on that page.

For `eligibility`, open:

- `/app/patients/<maryPatientId>/eligibility`

In `Add eligibility`, enter:

- `Service line` = `Skilled Nursing`
- `Status` = `ACTIVE`
- `Eligibility start` = `2026-04-01`
- `Eligibility end` = `2026-12-31`
- `Notes` = `Medicare Advantage eligibility active for 2026 plan year.`

Click:

- `Add eligibility`

For `diagnoses`, open:

- `/app/patients/<maryPatientId>/diagnoses`

Create one primary diagnosis if the form is present:

- description / diagnosis text = `Hypertension`
- code if required = `I10`
- mark as primary
- notes if available = `Primary chronic condition used for test workflow.`

Save using the button shown on that page.

For `payer`, open:

- `/app/patients/<maryPatientId>/payer`

In `Add payer link`, enter:

- `Payer name` = `Medicare Advantage Demo`
- `Payer external ID` = `MEDADV-1001`
- `Member ID` = `MEM-MJ-001`
- `Group number` = `GRP-100`
- `Effective from` = `2026-04-01`
- `Effective to` = `2026-12-31`
- `Status` = `ACTIVE`
- check `Primary payer`
- `Notes` = `Primary payer for Mary Johnson.`

Click:

- `Add payer link`

For `authorization`, open:

- `/app/patients/<maryPatientId>/authorizations`

In `Add authorization`, enter:

- `Payer link` = `Medicare Advantage Demo`
- `Service line` = `Skilled Nursing`
- `Authorization number` = `AUTH-MJ-001`
- `Authorized units` = `12`
- `Used units` = `0`
- `Effective from` = `2026-04-01`
- `Effective to` = `2026-06-30`
- `Status` = `ACTIVE`
- `Notes` = `Initial skilled nursing authorization for Release 1 tests.`

Click:

- `Add authorization`

For `attachments`, open:

- `/app/patients/<maryPatientId>/attachments`

If upload is available, upload one small safe file and enter:

- `Category` = choose the closest available document category
- `Description` = `Sample patient attachment for audit and printable workflow tests.`

Click:

- `Upload attachment`

Then create `Robert Evans`.

Open:

- `/app/patients/new/demographics`

Enter:

- `External reference` = `MRN-RE-1002`
- `First name` = `Robert`
- `Middle name` = `L`
- `Last name` = `Evans`
- `Preferred name` = `Bob`
- `Date of birth` = `1946-09-03`
- `Sex marker` = `M`
- `Primary phone` = `+1-312-555-0401`
- `Secondary phone` = ``
- `Email` = `robert.evans@example.test`
- `Language` = `en`
- `Notes summary` = `Secondary patient used for alternate branch and therapy scenarios.`

Click:

- `Create patient`

Then at minimum add one eligibility record for Robert:

- `Service line` = `Therapy`
- `Status` = `ACTIVE`
- `Eligibility start` = `2026-04-01`
- `Eligibility end` = `2026-12-31`
- `Notes` = `Therapy eligibility active.`

Before moving on, verify:

- patient search finds both patients
- opening detail routes works from the workspace
- `Mary Johnson` has enough data for downstream modules

## 14. Phase 6: Workforce Workspace

User:

- owner

Open:

- `/app/workforce`

Create `Carla Caregiver` first and link that caregiver to the accepted caregiver user.

Click:

- `New caregiver`

You should land on:

- `/app/workforce/new/profile`

In `Create caregiver profile`, enter:

- `Agency membership` = select `Carla Caregiver · CAREGIVER · carla.caregiver@northstar.example`
- `Primary branch` = `Downtown Branch`
- `Caregiver code` = `CG-CARLA-01`
- `Display name` = `Carla Caregiver`
- `Employment type` = `Full Time`
- `Start date` = `2026-04-01`
- `End date` = leave blank
- `Notes` = `Primary caregiver used for assigned visit, mobile, EVV, and documentation tests.`

Click:

- `Create caregiver profile`

Then create `Ramon Relief`.

Open:

- `/app/workforce/new/profile`

Enter:

- `Agency membership` = leave blank if Ramon is not linked to a user account
- `Primary branch` = `Downtown Branch`
- `Caregiver code` = `CG-RAMON-02`
- `Display name` = `Ramon Relief`
- `Employment type` = `PRN`
- `Start date` = `2026-04-01`
- `End date` = leave blank
- `Notes` = `Relief caregiver used for reassignment, reschedule, and cancellation scenarios.`

Click:

- `Create caregiver profile`

For `Carla Caregiver`, complete:

1. profile
2. credentials
3. languages if available
4. skills
5. geography
6. shift preferences
7. availability
8. unavailability

Use these exact values for `Carla Caregiver`:

For `credentials`, open:

- `/app/workforce/<carlaCaregiverId>/credentials`

Enter:

- `Certification` = `Illinois RN License`
- `Credential type` = `RN_LICENSE`
- `License / credential number` = `RN-IL-445566`
- `Issued on` = `2025-01-01`
- `Expires on` = `2027-12-31`
- `Status` = `ACTIVE`
- `Verification status` = `VERIFIED`
- `Notes` = `Verified nursing credential for Release 1 caregiver tests.`

Click:

- `Add credential`

For `languages and skills`, add:

- language `en`
- mark it as primary
- one skill record tied to `Wound Care`
- proficiency / notes fields: use the strongest available positive value

For `geography`, add one preference:

- `Preference type` = `POSTAL_CODE`
- `Postal code` = `60661`
- `Radius miles` = `20`
- `Priority` / `rank` if present = `1`
- `Notes` = `Primary coverage area around downtown Chicago.`

For `shift preferences`, add:

- `Day of week` = `MONDAY`
- `Preferred start` = `08:00`
- `Preferred end` = `17:00`
- `Shift length` = `480`
- `Preference strength` = strongest positive option available
- `Notes` = `Standard weekday shift preference.`

For `availability`, add:

- `Availability type` = `RECURRING_WEEKLY`
- `Branch` = `Downtown Branch`
- `Day of week` = `MONDAY`
- `Start time` = `08:00`
- `End time` = `17:00`
- `Effective from` = `2026-04-01`
- `Effective to` = `2026-12-31`
- `Notes` = `Recurring Monday availability for visit assignment tests.`

For `unavailability`, add:

- `Reason type` = choose the closest PTO / personal-time option
- `Starts at` = `2026-04-15T13:00`
- `Ends at` = `2026-04-15T17:00`
- `Approval status` = approved / closest positive status
- `All day` = unchecked
- `Notes` = `Half-day blocked time used for schedulability testing.`

Verify:

- caregiver search/list works
- caregiver detail routes open properly
- credentials and availability data persist after reload

## 15. Phase 7: Scheduling

Users:

- owner first
- scheduler second

### 15.1 Owner Scheduling Run

Open:

- `/app/scheduling`

Create:

- Visit A
- Visit B
- Visit C

Use:

- patient: `Mary Johnson`
- branch: `Downtown Branch`
- visit type: `RN Routine Visit`

For Visit A, click `New visit` and enter:

- `Patient` = `Mary Johnson`
- `Branch` = `Downtown Branch`
- `Service line` = `Skilled Nursing`
- `Visit type` = `RN Routine Visit`
- `Planned start` = `2026-04-07T09:00`
- `Planned end` = `2026-04-07T10:00`
- `Timezone` = `America/Chicago`
- `Priority` = `Standard`
- `Visit mode` = `One-time visit`
- `Notes` = `Visit A primary nursing visit for full end-to-end workflow.`

Click:

- `Create visit`

For Visit B, click `New visit` and enter:

- `Patient` = `Mary Johnson`
- `Branch` = `Downtown Branch`
- `Service line` = `Skilled Nursing`
- `Visit type` = `RN Routine Visit`
- `Planned start` = `2026-04-07T13:00`
- `Planned end` = `2026-04-07T14:00`
- `Timezone` = `America/Chicago`
- `Priority` = `Standard`
- `Visit mode` = `One-time visit`
- `Notes` = `Visit B used for reschedule or cancellation workflow.`

Click:

- `Create visit`

For Visit C, click `New visit` and enter:

- `Patient` = `Mary Johnson`
- `Branch` = `Downtown Branch`
- `Service line` = `Skilled Nursing`
- `Visit type` = `RN Routine Visit`
- `Planned start` = `2026-04-07T16:00`
- `Planned end` = `2026-04-07T17:00`
- `Timezone` = `America/Chicago`
- `Priority` = `High`
- `Visit mode` = `One-time visit`
- `Notes` = `Visit C used for open shift or conflict scenario.`

Click:

- `Create visit`

Actions to test:

1. open Visit A from the board
2. click `Assign`
3. select `Carla Caregiver`
4. click `Commit assignment`
5. confirm assignment success
6. open Visit B
7. click `Assign`
8. select `Ramon Relief`
9. click `Commit assignment`
10. confirm assignment success
11. open Visit B again
12. click `Reschedule`
13. set `New planned start` = `2026-04-07T14:30`
14. set `New planned end` = `2026-04-07T15:30`
15. keep timezone as `America/Chicago`
16. keep caregiver as `Ramon Relief`
17. `Reason` = `Patient requested later afternoon appointment`
18. click `Commit reschedule`
19. confirm reschedule success
20. if you want the cancel path instead, open Visit B after reschedule
21. click `Cancel`
22. set `Cancellation party` = `AGENCY`
23. set `Reason` = `Coverage test cancellation`
24. check the confirm box
25. click `Cancel visit`
26. for Visit C, leave it unassigned if it already appears as an open shift
27. if not, open Visit C and use the open-shift / no-candidate path that the UI exposes
28. inspect any conflict preview or match guidance shown in assignment or reschedule flows

### 15.2 Scheduler Permission Run

Log in as:

- `sam.scheduler@northstar.example`

Open:

- `/app/home`
- `/app/scheduling`

Verify:

- scheduler can see scheduling tools
- scheduler cannot access owner security/admin routes

## 16. Phase 8: Mobile And EVV

User:

- `carla.caregiver@northstar.example`

### 16.1 Mobile Login

Open:

- `http://localhost:5173/mobile/login`

Confirm you see:

- `MavieHealth`
- `Epic 6 Mobile`
- `Caregiver field sign in`
- work email field
- password field

Log in as caregiver.

### 16.2 Mobile Workflow

Use Visit A.

Test:

1. `/mobile`
2. open Visit A from today work
3. `/mobile/visits/:visitId`
4. `/mobile/visits/:visitId/evv`
5. clock in
6. add mobile-safe notes/artifacts if available
7. create one missed-visit or exception scenario only if the UI supports it cleanly
8. clock out
9. `/mobile/visits/:visitId/documentation`
10. save draft documentation
11. `/mobile/messages`
12. `/mobile/account`

Verify:

- assigned caregiver can access own visit
- visit execution and EVV state persist
- caregiver can reach mobile documentation and messages

## 17. Phase 9: Documentation

Users:

- caregiver
- owner

As caregiver on Visit A:

1. create or open documentation record
2. save draft
3. validate required fields
4. submit documentation

Then as owner, test:

- `/app/documentation`
- `/app/documentation/templates`
- `/app/documentation/task-library`
- `/app/documentation/visits/:visitId`
- `/app/documentation/records/:documentationRecordId/printable`
- `/app/documentation/status`

Verify:

- Visit A documentation is visible in status workflow
- printable summary works
- templates and task library still work after real record creation

## 18. Phase 10: Messaging

Users:

- owner
- scheduler
- caregiver

Create at least these message scenarios:

1. owner to scheduler
2. scheduler to caregiver
3. one thread tied to patient or visit context
4. one unread or escalated scenario if available

Test:

- `/app/messaging`
- `/app/messaging/threads/:threadId`
- patient-linked discussion
- visit-linked discussion
- `/app/messaging/admin`
- `/app/messaging/command-center`
- `/mobile/messages`

Verify:

- thread creation works
- reply works
- unread state changes
- command center becomes richer after thread activity

## 19. Phase 11: Review

Users:

- reviewer
- owner

Log in as:

- `riley.reviewer@northstar.example`

Use submitted Visit A documentation.

Test:

- `/app/review`
- `/app/review/exceptions`
- `/app/review/items/:workItemId`
- `/app/review/items/:workItemId/resubmission`
- `/app/review/items/:workItemId/assignment`
- `/app/review/command-center`

Create these outcomes:

1. one approved review item
2. one returned-for-fix or exception-style item if the UI allows it

Verify:

- queue loads
- assignment works
- approve works
- return-for-fix or exception loop works

## 20. Phase 12: Compliance

Users:

- owner
- reviewer

Use `Mary Johnson` and Visit A.

Test:

- `/app/compliance`
- `/app/compliance/command-center`
- `/app/compliance/patients/:patientId`
- checklist detail route
- documentation-gap route
- acknowledgment route
- certification-period route
- risk-reminder route

Verify:

- documentation and EVV affect compliance posture
- patient-level compliance detail shows real data
- reviewer can access allowed compliance workflows

## 21. Phase 13: Patient Events

Users:

- owner
- reviewer

Use `Mary Johnson`.

Test:

- `/app/patient-events`
- `/app/patient-events/incidents`
- `/app/patient-events/infections`
- `/app/patient-events/wounds`
- follow-up route
- escalation route
- timeline route
- `/app/patient-events/command-center`

Create:

1. one incident
2. one infection
3. one wound record
4. one follow-up assignment
5. one escalation
6. one resolved patient-event item

Verify:

- lifecycle transitions work
- command center reflects open and escalated work

## 22. Phase 14: Care Progression

Users:

- owner
- caregiver

Use `Mary Johnson`.

Test:

- `/app/goals`
- `/app/goals/templates`
- `/app/goals/patients/:patientId`
- `/app/goals/patient-goals/:goalId`
- interventions route
- progress-notes route
- history route
- care-plan-sync route
- `/app/goals/command-center`

Create:

1. one goal template
2. one patient goal
3. one intervention
4. one progress note
5. one goal state transition
6. one care-plan sync update

Then as caregiver verify:

- caregiver can add progress-note style updates where permitted

## 23. Phase 15: Revenue Readiness

Users:

- billing
- owner

Log in as:

- `bill.billing@northstar.example`

Use:

- Visit A as ready or near-ready
- Visit B or Visit C as warning/blocker
- authorization `AUTH-MJ-001`

Test:

- `/app/revenue-readiness`
- `/app/revenue-readiness/visits/:visitId`
- `/app/revenue-readiness/exceptions`
- `/app/revenue-readiness/payroll-export`
- `/app/revenue-readiness/invoice-export`
- `/app/revenue-readiness/authorizations/:authorizationId`
- `/app/revenue-readiness/command-center`

Verify:

- readiness state reflects real upstream workflow state
- export preview distinguishes ready versus blocked
- billing user has finance access and not owner-only security access

## 24. Phase 16: Analytics

Users:

- owner
- scheduler
- reviewer
- billing
- auditor

Primary routes:

- `/app/analytics`
- `/app/analytics/operational`
- `/app/analytics/branch-performance`
- `/app/analytics/caregiver-utilization`
- `/app/analytics/readiness`
- `/app/analytics/command-center`

Verify:

- analytics are populated by the dataset you created
- branch comparison shows real branch differences if available
- caregiver utilization uses real visit data
- readiness analytics reflect documentation/review/revenue workflows

Then log in as:

- `audrey.audit@northstar.example`

Verify auditor can see allowed analytics routes and not perform restricted mutations.

## 25. Phase 17: Audit And Permission Cross-Checks

### 25.1 Audit

Log in as owner.

Open:

- `/app/admin/audit`

Confirm audit coverage exists for:

- login
- logout
- password or MFA changes
- user invites
- user status changes
- setup saves
- patient updates
- scheduling updates
- review decisions
- patient-event actions
- revenue readiness actions if surfaced

### 25.2 Role-Based Route Denials

Check at least one denied route per role.

Examples:

- scheduler should not access owner-only security admin routes
- caregiver should not access admin/setup/finance routes
- billing should not access caregiver mobile routes
- auditor should not access mutation-heavy routes

Expected:

- denied routes show controlled access-denied UI
- denied routes do not show broken blank pages

## 26. Final Release 1 Pass Criteria

Release 1 UI testing is complete only if all are true:

- backend started with the bootstrap owner line visible
- frontend started and `/login` rendered correctly
- owner login failure and success were both tested
- self-profile, password, MFA, sessions, and admin security screens were tested
- all required setup master data was created
- all required role users were invited and accepted
- `Mary Johnson` exists with payer and authorization data
- `Carla Caregiver` exists and can log into mobile
- Visit A went through scheduling, mobile, EVV, documentation, and review
- messaging was tested with real users
- compliance, patient events, and goals were tested using real patient data
- revenue readiness was tested using real visit and authorization data
- analytics displayed real downstream data
- audit log was reviewed
- role denials were verified

## 27. Fastest Serious End-To-End Chain

If you want the shortest full-chain Release 1 test, do exactly this:

1. start backend and confirm bootstrap line
2. start frontend and confirm login page
3. log in as owner
4. create branches, service lines, visit types, task template, documentation template
5. invite scheduler, caregiver, reviewer, billing, auditor
6. accept those invitations
7. create patient `Mary Johnson`
8. create caregivers `Carla Caregiver` and `Ramon Relief`
9. create Visit A, Visit B, Visit C
10. log in as caregiver and complete Visit A mobile + EVV + documentation
11. log in as reviewer and process Visit A review work
12. create one compliance action, one patient event, and one goal for `Mary Johnson`
13. log in as billing and verify revenue readiness
14. log in as owner and verify analytics and audit

That is the minimum serious Release 1 manual UI run.
