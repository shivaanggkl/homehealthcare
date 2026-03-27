package com.homehealthcare.platform.email;

import java.time.OffsetDateTime;

public record TemplatedEmail(
        String subject,
        String textBody,
        String htmlBody,
        String actionUrl,
        OffsetDateTime expiresAt,
        EmailBranding branding) {
}
