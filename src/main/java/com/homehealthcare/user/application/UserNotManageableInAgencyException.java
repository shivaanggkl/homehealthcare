package com.homehealthcare.user.application;

import java.util.UUID;

public class UserNotManageableInAgencyException extends RuntimeException {

    public UserNotManageableInAgencyException(UUID userId, UUID agencyId) {
        super("User " + userId + " is not a member of agency " + agencyId);
    }
}
