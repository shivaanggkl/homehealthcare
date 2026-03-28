package com.homehealthcare.branch.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBranchCodeException extends RuntimeException {

    public DuplicateBranchCodeException(String code) {
        super("Branch code already exists within agency: " + code);
    }
}
