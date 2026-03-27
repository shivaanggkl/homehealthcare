package com.homehealthcare.invitation.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingInvitationEmailSender implements InvitationEmailSender {

    @Override
    public void send(InvitationEmail email) {
        log.info(
                "Dispatched agency invite email invitationId={} agencyId={} recipient={} subject={} expiresAt={}",
                email.invitationId(),
                email.agencyId(),
                email.recipientEmail(),
                email.subject(),
                email.expiresAt());
    }
}
