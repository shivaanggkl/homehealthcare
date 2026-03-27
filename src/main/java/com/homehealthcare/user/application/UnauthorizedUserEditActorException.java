package com.homehealthcare.user.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedUserEditActorException extends RuntimeException {

    public UnauthorizedUserEditActorException(UUID membershipId) {
        super("Membership cannot edit users: " + membershipId);
    }
}
