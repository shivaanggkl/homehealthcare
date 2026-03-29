package com.homehealthcare.mobile.application;

import java.util.UUID;

public class UnauthorizedMobileActorException extends RuntimeException {

    public UnauthorizedMobileActorException(UUID actorMembershipId) {
        super("Actor membership is not allowed to execute this mobile action: " + actorMembershipId);
    }
}
