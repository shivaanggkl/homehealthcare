package com.homehealthcare.branch.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBranchNameException extends RuntimeException {

    public DuplicateBranchNameException(String name) {
        super("Branch name already exists within agency: " + name);
    }
}
