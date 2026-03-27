package com.homehealthcare.platform.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.email", name = "security-alert-enabled", havingValue = "true")
public class LoggingSecurityAlertEmailSender implements SecurityAlertEmailSender {

    @Override
    public void send(SecurityAlertEmail email) {
        log.info(
                "Dispatched security alert email recipient={} subject={} expiresAt={}",
                email.recipientEmail(),
                email.subject(),
                email.expiresAt());
    }
}
