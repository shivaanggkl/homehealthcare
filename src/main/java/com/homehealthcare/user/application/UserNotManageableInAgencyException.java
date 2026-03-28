package com.homehealthcare.user.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotManageableInAgencyException extends RuntimeException {

    public UserNotManageableInAgencyException(UUID userId, UUID agencyId) {
        super("User " + userId + " is not a member of agency " + agencyId);
    }
}
