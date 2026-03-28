package com.homehealthcare.platform.audit.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAuditLogActorException extends RuntimeException {

    public UnauthorizedAuditLogActorException(UUID membershipId) {
        super("Membership cannot view audit logs: " + membershipId);
    }
}
