package com.homehealthcare.invitation.application;

import java.util.UUID;

public class UnauthorizedInvitationActorException extends RuntimeException {

    public UnauthorizedInvitationActorException(UUID membershipId) {
        super("Membership cannot send user invites: " + membershipId);
    }
}
