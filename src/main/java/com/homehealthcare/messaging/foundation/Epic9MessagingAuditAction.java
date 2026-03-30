package com.homehealthcare.messaging.foundation;

public enum Epic9MessagingAuditAction {
    THREAD_CREATED("MSG_THREAD_CREATED"),
    MESSAGE_SENT("MSG_MESSAGE_SENT"),
    PARTICIPANT_ADDED("MSG_PARTICIPANT_ADDED"),
    PARTICIPANT_REMOVED("MSG_PARTICIPANT_REMOVED"),
    STAFF_GROUP_UPDATED("MSG_STAFF_GROUP_UPDATED"),
    BRANCH_BROADCAST_SENT("MSG_BRANCH_BROADCAST_SENT"),
    MESSAGE_READ("MSG_MESSAGE_READ"),
    ESCALATION_TAGGED("MSG_ESCALATION_TAGGED"),
    ESCALATION_RESOLVED("MSG_ESCALATION_RESOLVED");

    private final String actionType;

    Epic9MessagingAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
