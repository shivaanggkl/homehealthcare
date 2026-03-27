# System Email Standard

## Story

`E1-S44 — Invite and security email templates`

## Required templates

- Invite email template
- Password reset email template
- Security alert email template when security alerts are enabled

## Template rules

- Emails must render both text and HTML bodies.
- Template content must be driven by reusable variables so agency branding can be customized later.
- Current branding variables include platform name, agency name, support email, primary color, and logo URL.

## Link rules

- Invite and password reset links must include a signature and an expiration timestamp.
- The signature must be generated server-side with `HmacSHA256`.
- Email transports must not log raw tokens or full signed URLs.

## Configuration

- `app.email.base-url`
- `app.email.invite-path`
- `app.email.password-reset-path`
- `app.email.security-alert-path`
- `app.email.platform-name`
- `app.email.support-email`
- `app.email.primary-color`
- `app.email.link-signing-secret`
- `app.email.security-alert-enabled`
