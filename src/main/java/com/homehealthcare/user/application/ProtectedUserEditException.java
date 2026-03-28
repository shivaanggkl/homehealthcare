package com.homehealthcare.user.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ProtectedUserEditException extends RuntimeException {

    public ProtectedUserEditException(String message) {
        super(message);
    }
}
