package com.homehealthcare.branch.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedBranchOperationException extends RuntimeException {

    public UnauthorizedBranchOperationException(UUID branchId) {
        super("Current user is not allowed to modify branch: " + branchId);
    }
}
