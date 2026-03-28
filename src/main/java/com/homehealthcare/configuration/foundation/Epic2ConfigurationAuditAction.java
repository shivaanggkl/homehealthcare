package com.homehealthcare.configuration.foundation;

public enum Epic2ConfigurationAuditAction {
    CREATED("CONFIGURATION_CREATED"),
    UPDATED("CONFIGURATION_UPDATED"),
    DEACTIVATED("CONFIGURATION_DEACTIVATED"),
    PUBLISHED("CONFIGURATION_PUBLISHED");

    private final String actionType;

    Epic2ConfigurationAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
