package com.homehealthcare.evv.foundation;

public enum Epic7EvvAuditAction {
    CLOCK_IN_RECORDED("EVV_CLOCK_IN_RECORDED"),
    CLOCK_OUT_RECORDED("EVV_CLOCK_OUT_RECORDED"),
    GEOFENCE_EVALUATED("EVV_GEOFENCE_EVALUATED"),
    SIGNATURE_STATUS_RECORDED("EVV_SIGNATURE_STATUS_RECORDED"),
    MISSED_VISIT_REPORTED("EVV_MISSED_VISIT_REPORTED"),
    EXCEPTION_LOGGED("EVV_EXCEPTION_LOGGED"),
    SUPERVISOR_NOTIFIED("EVV_SUPERVISOR_NOTIFIED"),
    ESCALATION_CREATED("EVV_ESCALATION_CREATED");

    private final String actionType;

    Epic7EvvAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
