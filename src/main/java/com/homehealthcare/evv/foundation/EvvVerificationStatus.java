package com.homehealthcare.evv.foundation;

public enum EvvVerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    VERIFIED_WITH_WARNING,
    EXCEPTION_OPEN,
    EXCEPTION_ACKNOWLEDGED,
    MISSED_VISIT_REPORTED,
    ESCALATED,
    RESOLVED
}
