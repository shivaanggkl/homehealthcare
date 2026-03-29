package com.homehealthcare.patient.foundation;

public enum Epic3PatientAuditAction {
    CREATED("PATIENT_RECORD_CREATED"),
    UPDATED("PATIENT_RECORD_UPDATED"),
    DEACTIVATED("PATIENT_RECORD_DEACTIVATED"),
    ARCHIVED("PATIENT_RECORD_ARCHIVED"),
    DUPLICATE_FLAGGED("PATIENT_DUPLICATE_FLAGGED"),
    ATTACHMENT_UPLOADED("PATIENT_ATTACHMENT_UPLOADED"),
    ATTACHMENT_DOWNLOADED("PATIENT_ATTACHMENT_DOWNLOADED");

    private final String actionType;

    Epic3PatientAuditAction(String actionType) {
        this.actionType = actionType;
    }

    public String actionType() {
        return actionType;
    }
}
