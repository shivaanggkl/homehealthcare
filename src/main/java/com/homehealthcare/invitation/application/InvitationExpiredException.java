package com.homehealthcare.invitation.application;

public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException() {
        super("Invitation token has expired");
    }
}
