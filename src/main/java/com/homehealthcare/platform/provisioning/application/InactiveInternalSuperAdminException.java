package com.homehealthcare.platform.provisioning.application;

import java.util.UUID;

public class InactiveInternalSuperAdminException extends RuntimeException {

    public InactiveInternalSuperAdminException(UUID adminId) {
        super("Internal super admin is not active: " + adminId);
    }
}
