# Password Policy Standard

## Baseline Rules

- All password setup and mutation flows use the same server-side policy.
- Passwords must meet the configured minimum length and complexity rules.
- The default policy requires at least 12 characters plus uppercase, lowercase, digit, and symbol characters.
- The policy is published at `GET /api/auth/password-policy` so invite, reset, and account-settings clients can render the same rules before submission.

## Common Password Blocking

- `security.password-policy.common-password-check-enabled` controls baseline common-password blocking.
- The implementation uses the bundled `security/common-passwords.txt` blocklist.
- The blocklist is a baseline safeguard, not a substitute for a full external breach-screening service.

## Reuse Prevention

- `security.password-policy.prevent-reuse-count` defines how many recent passwords cannot be reused.
- The default policy blocks reuse of the current password and the previous 4 stored password hashes, for a total of 5.
- Password history is enforced for invitation acceptance, password reset, and authenticated password change.

## Engineering Guardrails

- Do not hash or persist a new password before policy validation succeeds.
- Do not expose password hashes, history rows, or blocklist contents in API responses.
- New password entry points must use the shared `PasswordPolicy` component instead of duplicating validation logic.
