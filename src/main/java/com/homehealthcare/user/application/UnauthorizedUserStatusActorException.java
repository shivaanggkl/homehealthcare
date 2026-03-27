package com.homehealthcare.user.application;

import java.util.UUID;

public class UnauthorizedUserStatusActorException extends RuntimeException {

    public UnauthorizedUserStatusActorException(UUID membershipId) {
        super("Membership cannot change user status: " + membershipId);
    }
}
