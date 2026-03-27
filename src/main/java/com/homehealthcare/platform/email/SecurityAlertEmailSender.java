package com.homehealthcare.platform.email;

import java.time.OffsetDateTime;

public interface SecurityAlertEmailSender {

    void send(SecurityAlertEmail email);

    record SecurityAlertEmail(
            String recipientEmail,
            String subject,
            String textBody,
            String htmlBody,
            String actionUrl,
            OffsetDateTime expiresAt) {
    }
}
