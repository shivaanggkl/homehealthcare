package com.homehealthcare.invitation.application;

import java.util.UUID;

public class InvalidInvitationBranchException extends RuntimeException {

    public InvalidInvitationBranchException(UUID branchId, UUID agencyId) {
        super("Branch " + branchId + " does not belong to agency " + agencyId);
    }
}
