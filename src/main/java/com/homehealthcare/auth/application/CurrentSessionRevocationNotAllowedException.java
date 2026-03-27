package com.homehealthcare.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CurrentSessionRevocationNotAllowedException extends RuntimeException {

    public CurrentSessionRevocationNotAllowedException() {
        super("Current session must be ended through logout");
    }
}
