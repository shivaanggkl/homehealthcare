package com.homehealthcare.schedulingvisit.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.scheduling.foundation.SchedulingVisitStatus;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRule;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.visittype.domain.VisitType;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "visit_occurrences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitOccurrence extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_type_id")
    private VisitType visitType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_visit_rule_id")
    private RecurringVisitRule recurringVisitRule;

    @Column(name = "planned_start_at", nullable = false)
    private OffsetDateTime plannedStartAt;

    @Column(name = "planned_end_at", nullable = false)
    private OffsetDateTime plannedEndAt;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SchedulingVisitStatus status;

    @Column(name = "priority", length = 32)
    private String priority;

    @Column(name = "creation_mode", length = 32)
    private String creationMode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private VisitOccurrence(
            UUID id,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            VisitType visitType,
            RecurringVisitRule recurringVisitRule,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            SchedulingVisitStatus status,
            String priority,
            String creationMode,
            String notes) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignVisitType(visitType);
        assignRecurringVisitRule(recurringVisitRule);
        this.plannedStartAt = Objects.requireNonNull(plannedStartAt, "plannedStartAt must not be null");
        this.plannedEndAt = Objects.requireNonNull(plannedEndAt, "plannedEndAt must not be null");
        this.timezone = timezone;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.priority = priority;
        this.creationMode = creationMode;
        this.notes = notes;
        validateState();
    }

    public static VisitOccurrence create(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            VisitType visitType,
            RecurringVisitRule recurringVisitRule,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            String priority,
            String creationMode,
            String notes) {
        return VisitOccurrence.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .visitType(visitType)
                .recurringVisitRule(recurringVisitRule)
                .plannedStartAt(plannedStartAt)
                .plannedEndAt(plannedEndAt)
                .timezone(timezone)
                .status(SchedulingVisitStatus.PLANNED)
                .priority(priority)
                .creationMode(creationMode)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            Branch branch,
            ServiceLine serviceLine,
            VisitType visitType,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            String priority,
            String creationMode,
            String notes) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignVisitType(visitType);
        this.plannedStartAt = Objects.requireNonNull(plannedStartAt, "plannedStartAt must not be null");
        this.plannedEndAt = Objects.requireNonNull(plannedEndAt, "plannedEndAt must not be null");
        this.timezone = timezone;
        this.priority = priority;
        this.creationMode = creationMode;
        this.notes = notes;
        validateState();
    }

    public void markAssigned() {
        this.status = SchedulingVisitStatus.ASSIGNED;
    }

    public void markOpenShift() {
        this.status = SchedulingVisitStatus.OPEN_SHIFT;
    }

    public void markRescheduled(OffsetDateTime plannedStartAt, OffsetDateTime plannedEndAt, String timezone) {
        this.plannedStartAt = Objects.requireNonNull(plannedStartAt, "plannedStartAt must not be null");
        this.plannedEndAt = Objects.requireNonNull(plannedEndAt, "plannedEndAt must not be null");
        this.timezone = timezone;
        this.status = SchedulingVisitStatus.RESCHEDULED;
        validateState();
    }

    public void markCancelled() {
        this.status = SchedulingVisitStatus.CANCELLED;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the visit occurrence");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the visit occurrence");
        }
        this.serviceLine = serviceLine;
    }

    private void assignVisitType(VisitType visitType) {
        if (visitType != null && !Objects.equals(visitType.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitType must belong to the same agency as the visit occurrence");
        }
        this.visitType = visitType;
    }

    private void assignRecurringVisitRule(RecurringVisitRule recurringVisitRule) {
        if (recurringVisitRule != null && !Objects.equals(recurringVisitRule.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("recurringVisitRule must belong to the same agency as the visit occurrence");
        }
        if (recurringVisitRule != null && patient != null && !Objects.equals(recurringVisitRule.getPatient().getId(), patient.getId())) {
            throw new IllegalArgumentException("recurringVisitRule must belong to the same patient as the visit occurrence");
        }
        this.recurringVisitRule = recurringVisitRule;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (patient == null) {
            throw new IllegalArgumentException("patient must not be null");
        }
        timezone = Objects.requireNonNull(timezone, "timezone must not be null").trim();
        ZoneId.of(timezone);
        priority = normalizeOptional(priority);
        if (priority != null) {
            priority = priority.toUpperCase(Locale.ROOT);
        }
        creationMode = normalizeOptional(creationMode);
        if (creationMode != null) {
            creationMode = creationMode.toUpperCase(Locale.ROOT);
        }
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignVisitType(visitType);
        assignRecurringVisitRule(recurringVisitRule);
        validateState();
    }

    private void validateState() {
        if (!plannedEndAt.isAfter(plannedStartAt)) {
            throw new IllegalArgumentException("plannedEndAt must be after plannedStartAt");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
