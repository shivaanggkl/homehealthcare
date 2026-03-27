package com.homehealthcare.user.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedUserDirectoryActorException extends RuntimeException {

    public UnauthorizedUserDirectoryActorException(UUID membershipId) {
        super("Membership cannot access user directory: " + membershipId);
    }
}
