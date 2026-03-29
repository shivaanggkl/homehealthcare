package com.homehealthcare.evv.foundation;

import java.util.Objects;

public record GeofenceEvaluationResult(
        GeofenceEvaluationOutcome outcome,
        int distanceFromExpectedMeters,
        int toleranceMetersUsed,
        boolean blocking,
        String reasonCode) {

    public GeofenceEvaluationResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (distanceFromExpectedMeters < 0) {
            throw new IllegalArgumentException("distanceFromExpectedMeters must not be negative");
        }
        if (toleranceMetersUsed < 0) {
            throw new IllegalArgumentException("toleranceMetersUsed must not be negative");
        }
        reasonCode = reasonCode == null || reasonCode.isBlank() ? "UNSPECIFIED" : reasonCode;
    }

    public static GeofenceEvaluationResult withinTolerance(int distanceFromExpectedMeters, int toleranceMetersUsed) {
        return new GeofenceEvaluationResult(
                GeofenceEvaluationOutcome.WITHIN_TOLERANCE,
                distanceFromExpectedMeters,
                toleranceMetersUsed,
                false,
                "WITHIN_TOLERANCE");
    }

    public static GeofenceEvaluationResult warning(int distanceFromExpectedMeters, int toleranceMetersUsed, String reasonCode) {
        return new GeofenceEvaluationResult(
                GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_WARNING,
                distanceFromExpectedMeters,
                toleranceMetersUsed,
                false,
                reasonCode);
    }

    public static GeofenceEvaluationResult blocked(int distanceFromExpectedMeters, int toleranceMetersUsed, String reasonCode) {
        return new GeofenceEvaluationResult(
                GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_BLOCKED,
                distanceFromExpectedMeters,
                toleranceMetersUsed,
                true,
                reasonCode);
    }

    public static GeofenceEvaluationResult notEvaluable(String reasonCode) {
        return new GeofenceEvaluationResult(
                GeofenceEvaluationOutcome.NOT_EVALUABLE,
                0,
                0,
                false,
                reasonCode);
    }
}
