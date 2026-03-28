package com.homehealthcare.invitation.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInvitationBranchException extends RuntimeException {

    public InvalidInvitationBranchException(UUID branchId, UUID agencyId) {
        super("Branch " + branchId + " does not belong to agency " + agencyId);
    }
}
