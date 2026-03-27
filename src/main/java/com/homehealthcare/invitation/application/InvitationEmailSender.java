package com.homehealthcare.invitation.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface InvitationEmailSender {

    void send(InvitationEmail email);

    record InvitationEmail(
            UUID invitationId,
            UUID agencyId,
            String recipientEmail,
            String token,
            OffsetDateTime expiresAt) {
    }
}
