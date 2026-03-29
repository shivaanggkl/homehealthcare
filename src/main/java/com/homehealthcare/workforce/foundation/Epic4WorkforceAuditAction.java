package com.homehealthcare.workforce.foundation;

public enum Epic4WorkforceAuditAction {
    CREATED("WORKFORCE_RECORD_CREATED"),
    UPDATED("WORKFORCE_RECORD_UPDATED"),
    DEACTIVATED("WORKFORCE_RECORD_DEACTIVATED"),
    ARCHIVED("WORKFORCE_RECORD_ARCHIVED"),
    CONFLICT_FLAGGED("WORKFORCE_CONFLICT_FLAGGED"),
    PERFORMANCE_REFRESHED("WORKFORCE_PERFORMANCE_REFRESHED");

    private final String actionType;

    Epic4WorkforceAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
