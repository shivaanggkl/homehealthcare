package com.homehealthcare.review.foundation;

public enum Epic10ReviewAuditAction {
    REVIEW_ITEM_CREATED("REVIEW_ITEM_CREATED"),
    REVIEW_ASSIGNED("REVIEW_ASSIGNED"),
    REVIEW_REASSIGNED("REVIEW_REASSIGNED"),
    REVIEW_DECISION_RECORDED("REVIEW_DECISION_RECORDED"),
    REVIEW_RETURNED_FOR_FIX("REVIEW_RETURNED_FOR_FIX"),
    SIGNOFF_REQUESTED("REVIEW_SIGNOFF_REQUESTED"),
    COMPLETENESS_RECALCULATED("REVIEW_COMPLETENESS_RECALCULATED");

    private final String actionType;

    Epic10ReviewAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
