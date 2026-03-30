package com.homehealthcare.compliance.application;

import java.util.UUID;

public class UnauthorizedComplianceActorException extends RuntimeException {

    public UnauthorizedComplianceActorException(UUID membershipId, String action) {
        super("Agency membership %s is not authorized to %s in the compliance workspace.".formatted(membershipId, action));
    }
}
