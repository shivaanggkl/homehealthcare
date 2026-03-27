package com.homehealthcare.invitation.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedInvitationActorException extends RuntimeException {

    public UnauthorizedInvitationActorException(UUID membershipId) {
        super("Membership cannot send user invites: " + membershipId);
    }
}
