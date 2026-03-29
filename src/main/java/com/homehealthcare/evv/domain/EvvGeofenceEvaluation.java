package com.homehealthcare.evv.domain;

import com.homehealthcare.evv.foundation.GeofenceEvaluationOutcome;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "evv_geofence_evaluations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvvGeofenceEvaluation extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_session_id", nullable = false)
    private EvvVerificationSession verificationSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clock_event_id", nullable = false)
    private EvvClockEvent clockEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geofence_rule_id")
    private GeofenceToleranceRule geofenceRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 40)
    private GeofenceEvaluationOutcome outcome;

    @Column(name = "distance_from_expected_meters", nullable = false)
    private int distanceFromExpectedMeters;

    @Column(name = "tolerance_meters_used", nullable = false)
    private int toleranceMetersUsed;

    @Column(name = "blocking", nullable = false)
    private boolean blocking;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Builder
    private EvvGeofenceEvaluation(
            UUID id,
            EvvVerificationSession verificationSession,
            EvvClockEvent clockEvent,
            GeofenceToleranceRule geofenceRule,
            GeofenceEvaluationOutcome outcome,
            int distanceFromExpectedMeters,
            int toleranceMetersUsed,
            boolean blocking,
            String reasonCode) {
        this.id = id;
        assignVerificationSession(verificationSession);
        assignClockEvent(clockEvent);
        assignGeofenceRule(geofenceRule);
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.distanceFromExpectedMeters = distanceFromExpectedMeters;
        this.toleranceMetersUsed = toleranceMetersUsed;
        this.blocking = blocking;
        this.reasonCode = reasonCode;
        validate();
    }

    public static EvvGeofenceEvaluation record(
            EvvVerificationSession verificationSession,
            EvvClockEvent clockEvent,
            GeofenceToleranceRule geofenceRule,
            GeofenceEvaluationOutcome outcome,
            int distanceFromExpectedMeters,
            int toleranceMetersUsed,
            boolean blocking,
            String reasonCode) {
        return EvvGeofenceEvaluation.builder()
                .id(UUID.randomUUID())
                .verificationSession(verificationSession)
                .clockEvent(clockEvent)
                .geofenceRule(geofenceRule)
                .outcome(outcome)
                .distanceFromExpectedMeters(distanceFromExpectedMeters)
                .toleranceMetersUsed(toleranceMetersUsed)
                .blocking(blocking)
                .reasonCode(reasonCode)
                .build();
    }

    private void assignVerificationSession(EvvVerificationSession verificationSession) {
        this.verificationSession = Objects.requireNonNull(verificationSession, "verificationSession must not be null");
        assignAgency(verificationSession.getAgency());
    }

    private void assignClockEvent(EvvClockEvent clockEvent) {
        this.clockEvent = Objects.requireNonNull(clockEvent, "clockEvent must not be null");
        if (!Objects.equals(clockEvent.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("clockEvent must belong to the same agency as the geofence evaluation");
        }
    }

    private void assignGeofenceRule(GeofenceToleranceRule geofenceRule) {
        if (geofenceRule != null && !Objects.equals(geofenceRule.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("geofenceRule must belong to the same agency as the geofence evaluation");
        }
        this.geofenceRule = geofenceRule;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null").trim();
        validate();
    }

    private void validate() {
        if (distanceFromExpectedMeters < 0) {
            throw new IllegalArgumentException("distanceFromExpectedMeters must not be negative");
        }
        if (toleranceMetersUsed < 0) {
            throw new IllegalArgumentException("toleranceMetersUsed must not be negative");
        }
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode must not be blank");
        }
    }
}
