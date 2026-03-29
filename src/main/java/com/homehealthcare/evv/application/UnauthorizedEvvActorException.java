package com.homehealthcare.evv.application;

import java.util.UUID;

public class UnauthorizedEvvActorException extends RuntimeException {

    public UnauthorizedEvvActorException(UUID actorMembershipId) {
        super("Actor membership is not allowed to execute this EVV action: " + actorMembershipId);
    }
}
