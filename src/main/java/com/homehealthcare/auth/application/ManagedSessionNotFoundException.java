package com.homehealthcare.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ManagedSessionNotFoundException extends RuntimeException {

    public ManagedSessionNotFoundException() {
        super("Session not found");
    }
}
