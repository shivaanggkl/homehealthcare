package com.homehealthcare.invitation.application;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation token is invalid");
    }
}
