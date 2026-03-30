package com.homehealthcare.revenuereadiness.foundation;

public enum Epic14RevenueReadinessAuditAction {
    READINESS_RECALCULATED("REVENUE_READINESS_RECALCULATED"),
    EXCEPTION_FLAG_UPDATED("REVENUE_EXCEPTION_FLAG_UPDATED"),
    EXPORT_GENERATED("REVENUE_EXPORT_GENERATED"),
    AUTHORIZATION_USAGE_REFRESHED("REVENUE_AUTHORIZATION_USAGE_REFRESHED"),
    PAYER_SERVICE_SUMMARY_REFRESHED("REVENUE_PAYER_SERVICE_SUMMARY_REFRESHED");

    private final String actionType;

    Epic14RevenueReadinessAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
