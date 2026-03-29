package com.homehealthcare.scheduling.foundation;

import java.util.Objects;

public record SchedulingTravelAwareness(
        int estimatedTravelMinutes,
        int gapMinutes,
        TravelAwarenessLevel level,
        String rationaleCode) {

    public SchedulingTravelAwareness {
        if (estimatedTravelMinutes < 0) {
            throw new IllegalArgumentException("estimatedTravelMinutes must not be negative");
        }
        if (gapMinutes < 0) {
            throw new IllegalArgumentException("gapMinutes must not be negative");
        }
        Objects.requireNonNull(level, "level must not be null");
        rationaleCode = rationaleCode == null || rationaleCode.isBlank() ? "UNSPECIFIED" : rationaleCode;
    }

    public static SchedulingTravelAwareness feasible(int estimatedTravelMinutes, int gapMinutes) {
        return new SchedulingTravelAwareness(estimatedTravelMinutes, gapMinutes, TravelAwarenessLevel.FEASIBLE, "FEASIBLE");
    }

    public static SchedulingTravelAwareness tightConnection(int estimatedTravelMinutes, int gapMinutes) {
        return new SchedulingTravelAwareness(estimatedTravelMinutes, gapMinutes, TravelAwarenessLevel.TIGHT_CONNECTION, "TIGHT_CONNECTION");
    }

    public static SchedulingTravelAwareness infeasible(int estimatedTravelMinutes, int gapMinutes) {
        return new SchedulingTravelAwareness(estimatedTravelMinutes, gapMinutes, TravelAwarenessLevel.INFEASIBLE, "INFEASIBLE");
    }

    public static SchedulingTravelAwareness unknown() {
        return new SchedulingTravelAwareness(0, 0, TravelAwarenessLevel.UNKNOWN, "UNKNOWN");
    }
}
