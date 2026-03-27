package com.homehealthcare.user.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedUserStatusActorException extends RuntimeException {

    public UnauthorizedUserStatusActorException(UUID membershipId) {
        super("Membership cannot change user status: " + membershipId);
    }
}
