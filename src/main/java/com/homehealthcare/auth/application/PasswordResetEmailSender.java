package com.homehealthcare.auth.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface PasswordResetEmailSender {

    void send(PasswordResetEmail email);

    record PasswordResetEmail(
            UUID passwordResetTokenId,
            UUID userId,
            String recipientEmail,
            String subject,
            String textBody,
            String htmlBody,
            String actionUrl,
            OffsetDateTime expiresAt) {
    }
}
