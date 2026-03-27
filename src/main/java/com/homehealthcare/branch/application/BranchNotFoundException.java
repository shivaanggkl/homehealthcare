package com.homehealthcare.branch.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(UUID branchId) {
        super("Branch not found for current agency: " + branchId);
    }
}
