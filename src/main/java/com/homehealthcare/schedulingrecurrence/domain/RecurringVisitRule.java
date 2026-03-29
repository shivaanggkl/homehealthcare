package com.homehealthcare.schedulingrecurrence.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "recurring_visit_rules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringVisitRule extends AgencyScopedEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "cadence", nullable = false, length = 32)
    private RecurringVisitCadence cadence;

    @Column(name = "weekday_pattern", length = 100)
    private String weekdayPattern;

    @Column(name = "effective_start", nullable = false)
    private LocalDate effectiveStart;

    @Column(name = "effective_end")
    private LocalDate effectiveEnd;

    @Column(name = "planned_start_time", nullable = false)
    private LocalTime plannedStartTime;

    @Column(name = "planned_end_time", nullable = false)
    private LocalTime plannedEndTime;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "priority", length = 32)
    private String priority;

    @Column(name = "creation_mode", length = 32)
    private String creationMode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RecurringVisitRuleStatus status;

    @Builder
    private RecurringVisitRule(
            UUID id,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            VisitType visitType,
            RecurringVisitCadence cadence,
            String weekdayPattern,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            LocalTime plannedStartTime,
            LocalTime plannedEndTime,
            String timezone,
            String priority,
            String creationMode,
            String notes,
            RecurringVisitRuleStatus status) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignVisitType(visitType);
        this.cadence = Objects.requireNonNull(cadence, "cadence must not be null");
        this.weekdayPattern = weekdayPattern;
        this.effectiveStart = Objects.requireNonNull(effectiveStart, "effectiveStart must not be null");
        this.effectiveEnd = effectiveEnd;
        this.plannedStartTime = Objects.requireNonNull(plannedStartTime, "plannedStartTime must not be null");
        this.plannedEndTime = Objects.requireNonNull(plannedEndTime, "plannedEndTime must not be null");
        this.timezone = timezone;
        this.priority = priority;
        this.creationMode = creationMode;
        this.notes = notes;
        this.status = Objects.requireNonNull(status, "status must not be null");
        validateState();
    }

    public static RecurringVisitRule create(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            VisitType visitType,
            RecurringVisitCadence cadence,
            Set<DayOfWeek> weekdays,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            LocalTime plannedStartTime,
            LocalTime plannedEndTime,
            String timezone,
            String priority,
            String creationMode,
            String notes) {
        return RecurringVisitRule.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .visitType(visitType)
                .cadence(cadence)
                .weekdayPattern(toWeekdayPattern(weekdays))
                .effectiveStart(effectiveStart)
                .effectiveEnd(effectiveEnd)
                .plannedStartTime(plannedStartTime)
                .plannedEndTime(plannedEndTime)
                .timezone(timezone)
                .priority(priority)
                .creationMode(creationMode)
                .notes(notes)
                .status(RecurringVisitRuleStatus.ACTIVE)
                .build();
    }

    public void updateDetails(
            Branch branch,
            ServiceLine serviceLine,
            VisitType visitType,
            RecurringVisitCadence cadence,
            Set<DayOfWeek> weekdays,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            LocalTime plannedStartTime,
            LocalTime plannedEndTime,
            String timezone,
            String priority,
            String creationMode,
            String notes) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignVisitType(visitType);
        this.cadence = Objects.requireNonNull(cadence, "cadence must not be null");
        this.weekdayPattern = toWeekdayPattern(weekdays);
        this.effectiveStart = Objects.requireNonNull(effectiveStart, "effectiveStart must not be null");
        this.effectiveEnd = effectiveEnd;
        this.plannedStartTime = Objects.requireNonNull(plannedStartTime, "plannedStartTime must not be null");
        this.plannedEndTime = Objects.requireNonNull(plannedEndTime, "plannedEndTime must not be null");
        this.timezone = timezone;
        this.priority = priority;
        this.creationMode = creationMode;
        this.notes = notes;
        validateState();
    }

    public void deactivate() {
        this.status = RecurringVisitRuleStatus.INACTIVE;
    }

    public Set<DayOfWeek> weekdays() {
        if (weekdayPattern == null) {
            return Set.of();
        }
        return Arrays.stream(weekdayPattern.split(","))
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the recurrence rule");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the recurrence rule");
        }
        this.serviceLine = serviceLine;
    }

    private void assignVisitType(VisitType visitType) {
        if (visitType != null && !Objects.equals(visitType.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitType must belong to the same agency as the recurrence rule");
        }
        this.visitType = visitType;
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
        weekdayPattern = normalizeOptional(weekdayPattern);
        priority = normalizeOptional(priority);
        if (priority != null) {
            priority = priority.toUpperCase(Locale.ROOT);
        }
        creationMode = normalizeOptional(creationMode);
        if (creationMode != null) {
            creationMode = creationMode.toUpperCase(Locale.ROOT);
        }
        notes = normalizeOptional(notes);
        timezone = normalizeRequired(timezone);
        ZoneId.of(timezone);
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignVisitType(visitType);
        validateState();
    }

    private void validateState() {
        if (effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            throw new IllegalArgumentException("effectiveEnd must be on or after effectiveStart");
        }
        if (!plannedEndTime.isAfter(plannedStartTime)) {
            throw new IllegalArgumentException("plannedEndTime must be after plannedStartTime");
        }
        Set<DayOfWeek> weekdays = weekdays();
        if (cadence == RecurringVisitCadence.SELECTED_WEEKDAYS && weekdays.isEmpty()) {
            throw new IllegalArgumentException("weekdayPattern must be provided for SELECTED_WEEKDAYS cadence");
        }
    }

    private static String toWeekdayPattern(Set<DayOfWeek> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return null;
        }
        return weekdays.stream()
                .sorted()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
