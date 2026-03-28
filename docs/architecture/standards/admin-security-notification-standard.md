# Admin Security Notification Standard

## Story

`E1-S45 — Admin notification on critical account events`

## Supported events

- Repeated failed login that results in temporary lockout
- Locked account
- New admin created through invitation or role promotion

## Delivery model

- Delivery is optional and preference-driven.
- Preferences are stored per agency admin membership.
- Current delivery channel is email only.
- Only active `AGENCY_OWNER` and `BRANCH_ADMIN` users can receive notifications.

## Preference rules

- Default preferences enable all supported notifications.
- Preferences are managed through `GET` and `PUT /api/security/admin-notifications`.
- Non-admin users must not be allowed to manage these preferences.

## Audit rules

- Preference updates must create `ADMIN_NOTIFICATION_PREFERENCES_UPDATED`.
- Each delivered notification must create `ADMIN_SECURITY_NOTIFICATION_SENT`.
