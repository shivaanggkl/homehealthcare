package com.homehealthcare.notification.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAdminNotificationPreferenceException extends RuntimeException {

    public UnauthorizedAdminNotificationPreferenceException(UUID membershipId) {
        super("Agency membership %s is not allowed to manage admin notification preferences".formatted(membershipId));
    }
}
