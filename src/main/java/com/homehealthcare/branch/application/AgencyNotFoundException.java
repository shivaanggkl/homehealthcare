package com.homehealthcare.branch.application;

import java.util.UUID;

public class AgencyNotFoundException extends RuntimeException {

    public AgencyNotFoundException(UUID agencyId) {
        super("Agency not found: " + agencyId);
    }
}
