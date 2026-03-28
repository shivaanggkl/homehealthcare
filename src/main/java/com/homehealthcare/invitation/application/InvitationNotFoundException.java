package com.homehealthcare.invitation.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation token is invalid");
    }
}
