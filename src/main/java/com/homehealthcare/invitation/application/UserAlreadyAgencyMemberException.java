package com.homehealthcare.invitation.application;

import java.util.UUID;

public class UserAlreadyAgencyMemberException extends RuntimeException {

    public UserAlreadyAgencyMemberException(UUID userId, UUID agencyId) {
        super("User " + userId + " is already an active member of agency " + agencyId);
    }
}
