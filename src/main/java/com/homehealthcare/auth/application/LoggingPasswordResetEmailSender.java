package com.homehealthcare.auth.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

    @Override
    public void send(PasswordResetEmail email) {
        log.info(
                "Dispatched password reset email passwordResetTokenId={} userId={} recipient={} subject={} expiresAt={}",
                email.passwordResetTokenId(),
                email.userId(),
                email.recipientEmail(),
                email.subject(),
                email.expiresAt());
    }
}
