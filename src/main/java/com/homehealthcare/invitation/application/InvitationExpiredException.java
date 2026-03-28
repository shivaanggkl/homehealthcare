package com.homehealthcare.invitation.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException() {
        super("Invitation token has expired");
    }
}
