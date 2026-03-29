package com.homehealthcare.documentation.foundation;

public enum Epic8DocumentationAuditAction {
    TEMPLATE_CREATED("DOC_TEMPLATE_CREATED"),
    TEMPLATE_UPDATED("DOC_TEMPLATE_UPDATED"),
    TASK_LIBRARY_UPDATED("DOC_TASK_LIBRARY_UPDATED"),
    DOCUMENTATION_DRAFT_SAVED("DOC_DRAFT_SAVED"),
    DOCUMENTATION_SUBMITTED("DOC_SUBMITTED"),
    DOCUMENTATION_AMENDED("DOC_AMENDED"),
    ATTACHMENT_LINKED("DOC_ATTACHMENT_LINKED"),
    PRINTABLE_SUMMARY_GENERATED("DOC_PRINTABLE_SUMMARY_GENERATED");

    private final String actionType;

    Epic8DocumentationAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
