package com.homehealthcare.invitation.application;

public class InvitationAlreadyUsedException extends RuntimeException {

    public InvitationAlreadyUsedException() {
        super("Invitation token is no longer available");
    }
}
