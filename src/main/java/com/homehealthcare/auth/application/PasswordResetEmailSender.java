package com.homehealthcare.auth.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface PasswordResetEmailSender {

    void send(PasswordResetEmail email);

    record PasswordResetEmail(
            UUID passwordResetTokenId,
            UUID userId,
            String recipientEmail,
            String token,
            OffsetDateTime expiresAt) {
    }
}
