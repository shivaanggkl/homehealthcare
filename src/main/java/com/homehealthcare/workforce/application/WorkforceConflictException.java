package com.homehealthcare.workforce.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WorkforceConflictException extends RuntimeException {

    public WorkforceConflictException(String message) {
        super(message);
    }
}
