package com.homehealthcare.patientevent.foundation;

import java.util.UUID;

public record PatientEventAlertContract(
        UUID patientId,
        UUID branchId,
        PatientEventAlertType alertType,
        UUID targetId,
        String targetType,
        String severity,
        String summary) {
}
