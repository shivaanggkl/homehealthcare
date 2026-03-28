package com.homehealthcare.invitation.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyAgencyMemberException extends RuntimeException {

    public UserAlreadyAgencyMemberException(UUID userId, UUID agencyId) {
        super("User " + userId + " is already an active member of agency " + agencyId);
    }
}
