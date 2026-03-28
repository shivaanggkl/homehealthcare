package com.homehealthcare.agency.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAgencySettingsActorException extends RuntimeException {

    public UnauthorizedAgencySettingsActorException(UUID membershipId) {
        super("Membership %s is not allowed to manage agency settings".formatted(membershipId));
    }
}
