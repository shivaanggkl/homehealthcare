package com.homehealthcare.evv.foundation;

import java.util.Objects;

public record EvvComplianceProjection(
        boolean startEventPresent,
        boolean endEventPresent,
        GeofenceEvaluationOutcome geofenceOutcome,
        boolean signatureComplete,
        int openExceptionCount,
        boolean missedVisitReported,
        EvvComplianceOutcome overallOutcome) {

    public EvvComplianceProjection {
        Objects.requireNonNull(geofenceOutcome, "geofenceOutcome must not be null");
        Objects.requireNonNull(overallOutcome, "overallOutcome must not be null");
        if (openExceptionCount < 0) {
            throw new IllegalArgumentException("openExceptionCount must not be negative");
        }
    }

    public static EvvComplianceProjection ready(boolean startEventPresent, boolean endEventPresent, boolean signatureComplete) {
        return new EvvComplianceProjection(
                startEventPresent,
                endEventPresent,
                GeofenceEvaluationOutcome.WITHIN_TOLERANCE,
                signatureComplete,
                0,
                false,
                EvvComplianceOutcome.READY);
    }

    public static EvvComplianceProjection readyWithWarning(
            boolean startEventPresent,
            boolean endEventPresent,
            GeofenceEvaluationOutcome geofenceOutcome,
            boolean signatureComplete,
            int openExceptionCount) {
        return new EvvComplianceProjection(
                startEventPresent,
                endEventPresent,
                geofenceOutcome,
                signatureComplete,
                openExceptionCount,
                false,
                EvvComplianceOutcome.READY_WITH_WARNING);
    }

    public static EvvComplianceProjection blocked(
            boolean startEventPresent,
            boolean endEventPresent,
            GeofenceEvaluationOutcome geofenceOutcome,
            boolean signatureComplete,
            int openExceptionCount) {
        return new EvvComplianceProjection(
                startEventPresent,
                endEventPresent,
                geofenceOutcome,
                signatureComplete,
                openExceptionCount,
                false,
                EvvComplianceOutcome.BLOCKED);
    }

    public static EvvComplianceProjection missedVisit(int openExceptionCount) {
        return new EvvComplianceProjection(
                false,
                false,
                GeofenceEvaluationOutcome.NOT_EVALUABLE,
                false,
                openExceptionCount,
                true,
                EvvComplianceOutcome.MISSED_VISIT);
    }
}
