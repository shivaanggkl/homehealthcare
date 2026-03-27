package com.homehealthcare.agency.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAgencyMfaPolicyConfigurationException extends RuntimeException {

    public InvalidAgencyMfaPolicyConfigurationException(String message) {
        super(message);
    }
}
