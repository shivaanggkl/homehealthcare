package com.homehealthcare.security.tenant;

import org.springframework.security.access.AccessDeniedException;

public class TenantContextException extends AccessDeniedException {

    public TenantContextException(String message) {
        super(message);
    }
}
