package com.homehealthcare.invitation.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvitationAlreadyUsedException extends RuntimeException {

    public InvitationAlreadyUsedException() {
        super("Invitation token is no longer available");
    }
}
