package com.homehealthcare.scheduling.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
import com.homehealthcare.scheduling.foundation.SchedulingAuditService;
import com.homehealthcare.scheduling.foundation.SchedulingVisitStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingopenshift.domain.OpenShift;
import com.homehealthcare.schedulingopenshift.domain.OpenShiftRepository;
import com.homehealthcare.schedulingopenshift.domain.OpenShiftStatus;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitCadence;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRule;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRuleRepository;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRuleStatus;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationEvent;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationEventRepository;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationParty;
import com.homehealthcare.schedulingworkflow.domain.VisitRescheduleEvent;
import com.homehealthcare.schedulingworkflow.domain.VisitRescheduleEventRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class SchedulingRecordService {

    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final RecurringVisitRuleRepository recurringVisitRuleRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final OpenShiftRepository openShiftRepository;
    private final VisitRescheduleEventRepository visitRescheduleEventRepository;
    private final VisitCancellationEventRepository visitCancellationEventRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final SchedulingAuditService schedulingAuditService;
    private final SchedulingRulesService schedulingRulesService;

    @Transactional
    public VisitOccurrence createVisit(@NotNull AgencyMembership actorMembership, @Valid ManageVisitCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_SCHEDULE_VISITS);
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        VisitOccurrence saved = visitOccurrenceRepository.saveAndFlush(VisitOccurrence.create(
                patient,
                resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                null,
                command.plannedStartAt(),
                command.plannedEndAt(),
                command.timezone(),
                command.priority(),
                command.creationMode(),
                command.notes()));
        schedulingAuditService.recordVisitCreated(actorMembership, saved.getId(), saved.getBranchId(), visitMetadata(saved));
        return saved;
    }

    @Transactional
    public VisitOccurrence updateVisit(@NotNull AgencyMembership actorMembership, @NotNull UUID visitOccurrenceId, @Valid ManageVisitCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_SCHEDULE_VISITS);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        if (!Objects.equals(visit.getPatient().getId(), command.patientId())) {
            throw new SchedulingConflictException("Visit patient cannot be changed.");
        }
        CaregiverVisitAssignment activeAssignment = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visit.getId(), CaregiverAssignmentStatus.ACTIVE)
                .orElse(null);
        if (activeAssignment != null) {
            VisitOccurrence proposal = VisitOccurrence.create(
                    visit.getPatient(),
                    resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                    resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                    resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                    visit.getRecurringVisitRule(),
                    command.plannedStartAt(),
                    command.plannedEndAt(),
                    command.timezone(),
                    command.priority(),
                    command.creationMode(),
                    command.notes());
            schedulingRulesService.assertAssignmentAllowed(proposal, activeAssignment.getCaregiverProfile());
        }
        visit.updateDetails(
                resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                command.plannedStartAt(),
                command.plannedEndAt(),
                command.timezone(),
                command.priority(),
                command.creationMode(),
                command.notes());
        VisitOccurrence saved = visitOccurrenceRepository.saveAndFlush(visit);
        schedulingAuditService.recordVisitUpdated(actorMembership, saved.getId(), saved.getBranchId(), visitMetadata(saved));
        return saved;
    }

    @Transactional
    public RecurringVisitRule createRecurringRule(@NotNull AgencyMembership actorMembership, @Valid ManageRecurringVisitRuleCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_SCHEDULE_VISITS);
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        RecurringVisitRule saved = recurringVisitRuleRepository.saveAndFlush(RecurringVisitRule.create(
                patient,
                resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                command.cadence(),
                command.weekdays(),
                command.effectiveStart(),
                command.effectiveEnd(),
                command.plannedStartTime(),
                command.plannedEndTime(),
                command.timezone(),
                command.priority(),
                command.creationMode(),
                command.notes()));
        schedulingAuditService.recordRecurringRuleCreated(actorMembership, saved.getId(), saved.getBranch() == null ? null : saved.getBranch().getId(), recurringMetadata(saved));
        return saved;
    }

    @Transactional
    public RecurringVisitRule updateRecurringRule(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID recurringVisitRuleId,
            @Valid ManageRecurringVisitRuleCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_SCHEDULE_VISITS);
        RecurringVisitRule rule = recurringVisitRuleRepository.findByIdAndAgency_Id(recurringVisitRuleId, actorMembership.getAgencyId())
                .orElseThrow(() -> new SchedulingEntityNotFoundException("RecurringVisitRule", recurringVisitRuleId));
        if (!Objects.equals(rule.getPatient().getId(), command.patientId())) {
            throw new SchedulingConflictException("Recurring visit rule patient cannot be changed.");
        }
        rule.updateDetails(
                resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                command.cadence(),
                command.weekdays(),
                command.effectiveStart(),
                command.effectiveEnd(),
                command.plannedStartTime(),
                command.plannedEndTime(),
                command.timezone(),
                command.priority(),
                command.creationMode(),
                command.notes());
        RecurringVisitRule saved = recurringVisitRuleRepository.saveAndFlush(rule);
        schedulingAuditService.recordRecurringRuleUpdated(actorMembership, saved.getId(), saved.getBranch() == null ? null : saved.getBranch().getId(), recurringMetadata(saved));
        return saved;
    }

    @Transactional
    public RecurringVisitRule deactivateRecurringRule(@NotNull AgencyMembership actorMembership, @NotNull UUID recurringVisitRuleId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_SCHEDULE_VISITS);
        RecurringVisitRule rule = recurringVisitRuleRepository.findByIdAndAgency_Id(recurringVisitRuleId, actorMembership.getAgencyId())
                .orElseThrow(() -> new SchedulingEntityNotFoundException("RecurringVisitRule", recurringVisitRuleId));
        rule.deactivate();
        RecurringVisitRule saved = recurringVisitRuleRepository.saveAndFlush(rule);
        schedulingAuditService.recordRecurringRuleUpdated(actorMembership, saved.getId(), saved.getBranch() == null ? null : saved.getBranch().getId(), recurringMetadata(saved));
        return saved;
    }

    @Transactional
    public List<VisitOccurrence> expandRecurringRule(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID recurringVisitRuleId,
            @NotNull LocalDate windowStart,
            @NotNull LocalDate windowEnd) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_SCHEDULE_VISITS);
        RecurringVisitRule rule = recurringVisitRuleRepository.findByIdAndAgency_Id(recurringVisitRuleId, actorMembership.getAgencyId())
                .orElseThrow(() -> new SchedulingEntityNotFoundException("RecurringVisitRule", recurringVisitRuleId));
        if (windowEnd.isBefore(windowStart)) {
            throw new IllegalArgumentException("windowEnd must be on or after windowStart");
        }
        List<VisitOccurrence> created = new ArrayList<>();
        for (LocalDate date : datesFor(rule, windowStart, windowEnd)) {
            OffsetDateTime plannedStartAt = toOffsetDateTime(date, rule.getPlannedStartTime(), rule.getTimezone());
            if (visitOccurrenceRepository.existsByRecurringVisitRule_IdAndPlannedStartAt(rule.getId(), plannedStartAt)) {
                continue;
            }
            VisitOccurrence occurrence = VisitOccurrence.create(
                    rule.getPatient(),
                    rule.getBranch(),
                    rule.getServiceLine(),
                    rule.getVisitType(),
                    rule,
                    plannedStartAt,
                    toOffsetDateTime(date, rule.getPlannedEndTime(), rule.getTimezone()),
                    rule.getTimezone(),
                    rule.getPriority(),
                    rule.getCreationMode(),
                    rule.getNotes());
            VisitOccurrence saved = visitOccurrenceRepository.saveAndFlush(occurrence);
            schedulingAuditService.recordVisitCreated(actorMembership, saved.getId(), saved.getBranchId(), visitMetadata(saved));
            created.add(saved);
        }
        return created;
    }

    @Transactional
    public CaregiverVisitAssignment assignCaregiver(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @Valid AssignCaregiverCommand command) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_CAREGIVERS);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        CaregiverProfile caregiverProfile = resolveCaregiverProfile(actorMembership.getAgencyId(), command.caregiverProfileId());
        schedulingRulesService.assertAssignmentAllowed(visit, caregiverProfile);

        CaregiverVisitAssignment existingActive = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visit.getId(), CaregiverAssignmentStatus.ACTIVE)
                .orElse(null);
        if (existingActive != null) {
            if (Objects.equals(existingActive.getCaregiverProfileId(), caregiverProfile.getId())) {
                throw new SchedulingConflictException("This visit is already assigned to that caregiver.");
            }
            existingActive.reassign();
            caregiverVisitAssignmentRepository.saveAndFlush(existingActive);
            schedulingAuditService.recordAssignmentRemoved(actorMembership, existingActive.getId(), visit.getBranchId(), assignmentMetadata(existingActive));
        }

        CaregiverVisitAssignment saved = caregiverVisitAssignmentRepository.saveAndFlush(CaregiverVisitAssignment.create(
                visit,
                caregiverProfile,
                resolveBranch(actorMembership.getAgencyId(), command.branchId() == null ? visit.getBranchId() : command.branchId()),
                actorMembership,
                command.assignmentSource(),
                command.notes()));
        visit.markAssigned();
        visitOccurrenceRepository.saveAndFlush(visit);
        closeOpenShiftIfActive(actorMembership, visit, OpenShiftStatus.CLAIMED_OR_ASSIGNED);
        schedulingAuditService.recordCaregiverAssigned(actorMembership, saved.getId(), visit.getBranchId(), assignmentMetadata(saved));
        return saved;
    }

    @Transactional
    public OpenShift openShift(@NotNull AgencyMembership actorMembership, @NotNull UUID visitOccurrenceId, @Valid ManageOpenShiftCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_OPEN_SHIFTS);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        if (caregiverVisitAssignmentRepository.findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visit.getId(), CaregiverAssignmentStatus.ACTIVE).isPresent()) {
            throw new SchedulingConflictException("Assigned visits cannot be converted to an open shift without first removing the active assignment.");
        }
        if (openShiftRepository.findFirstByVisitOccurrence_IdAndStatusOrderByOpenedAtDesc(visit.getId(), OpenShiftStatus.OPEN).isPresent()) {
            throw new SchedulingConflictException("An active open shift already exists for this visit.");
        }
        OpenShift saved = openShiftRepository.saveAndFlush(OpenShift.create(
                visit,
                resolveBranch(actorMembership.getAgencyId(), command.branchId() == null ? visit.getBranchId() : command.branchId()),
                actorMembership,
                command.priority(),
                command.notes()));
        visit.markOpenShift();
        visitOccurrenceRepository.saveAndFlush(visit);
        schedulingAuditService.recordOpenShiftCreated(actorMembership, saved.getId(), visit.getBranchId(), openShiftMetadata(saved));
        return saved;
    }

    @Transactional
    public CaregiverVisitAssignment removeAssignment(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @NotNull UUID assignmentId,
            boolean convertToOpenShift,
            @Valid ManageOpenShiftCommand openShiftCommand) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_CAREGIVERS);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        CaregiverVisitAssignment assignment = caregiverVisitAssignmentRepository.findByIdAndAgency_Id(assignmentId, actorMembership.getAgencyId())
                .orElseThrow(() -> new SchedulingEntityNotFoundException("CaregiverVisitAssignment", assignmentId));
        if (!Objects.equals(assignment.getVisitOccurrenceId(), visitOccurrenceId)) {
            throw new SchedulingConflictException("Assignment does not belong to the requested visit.");
        }
        if (assignment.getAssignmentStatus() != CaregiverAssignmentStatus.ACTIVE) {
            throw new SchedulingConflictException("Only active assignments can be removed.");
        }
        assignment.remove();
        CaregiverVisitAssignment saved = caregiverVisitAssignmentRepository.saveAndFlush(assignment);
        schedulingAuditService.recordAssignmentRemoved(actorMembership, saved.getId(), visit.getBranchId(), assignmentMetadata(saved));
        if (convertToOpenShift) {
            openShift(actorMembership, visitOccurrenceId, openShiftCommand == null ? new ManageOpenShiftCommand(visit.getBranchId(), visit.getPriority(), null) : openShiftCommand);
        } else {
            visit.markPlanned();
            visitOccurrenceRepository.saveAndFlush(visit);
        }
        return saved;
    }

    @Transactional
    public VisitRescheduleEvent rescheduleVisit(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @Valid RescheduleVisitCommand command) {
        requirePermission(actorMembership, AgencyPermission.RESCHEDULE_VISITS);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        CaregiverVisitAssignment activeAssignment = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visit.getId(), CaregiverAssignmentStatus.ACTIVE)
                .orElse(null);
        CaregiverProfile previousCaregiver = activeAssignment == null ? null : activeAssignment.getCaregiverProfile();
        CaregiverProfile nextCaregiver = command.newCaregiverProfileId() == null
                ? previousCaregiver
                : resolveCaregiverProfile(actorMembership.getAgencyId(), command.newCaregiverProfileId());
        if (nextCaregiver != null) {
            VisitOccurrence proposal = VisitOccurrence.create(
                    visit.getPatient(),
                    resolveBranch(actorMembership.getAgencyId(), command.branchId() == null ? visit.getBranchId() : command.branchId()),
                    visit.getServiceLine(),
                    visit.getVisitType(),
                    visit.getRecurringVisitRule(),
                    command.newPlannedStartAt(),
                    command.newPlannedEndAt(),
                    command.timezone(),
                    visit.getPriority(),
                    visit.getCreationMode(),
                    visit.getNotes());
            schedulingRulesService.assertAssignmentAllowed(proposal, nextCaregiver);
        }

        OffsetDateTime previousStart = visit.getPlannedStartAt();
        OffsetDateTime previousEnd = visit.getPlannedEndAt();

        if (activeAssignment != null && command.newCaregiverProfileId() != null && !Objects.equals(activeAssignment.getCaregiverProfileId(), nextCaregiver.getId())) {
            activeAssignment.reassign();
            caregiverVisitAssignmentRepository.saveAndFlush(activeAssignment);
            schedulingAuditService.recordAssignmentRemoved(actorMembership, activeAssignment.getId(), visit.getBranchId(), assignmentMetadata(activeAssignment));
            CaregiverVisitAssignment newAssignment = caregiverVisitAssignmentRepository.saveAndFlush(CaregiverVisitAssignment.create(
                    visit,
                    nextCaregiver,
                    resolveBranch(actorMembership.getAgencyId(), command.branchId() == null ? visit.getBranchId() : command.branchId()),
                    actorMembership,
                    "RESCHEDULE",
                    command.reason()));
            schedulingAuditService.recordCaregiverAssigned(actorMembership, newAssignment.getId(), visit.getBranchId(), assignmentMetadata(newAssignment));
        } else if (activeAssignment == null && nextCaregiver != null) {
            CaregiverVisitAssignment newAssignment = caregiverVisitAssignmentRepository.saveAndFlush(CaregiverVisitAssignment.create(
                    visit,
                    nextCaregiver,
                    resolveBranch(actorMembership.getAgencyId(), command.branchId() == null ? visit.getBranchId() : command.branchId()),
                    actorMembership,
                    "RESCHEDULE",
                    command.reason()));
            schedulingAuditService.recordCaregiverAssigned(actorMembership, newAssignment.getId(), visit.getBranchId(), assignmentMetadata(newAssignment));
        }

        visit.markRescheduled(command.newPlannedStartAt(), command.newPlannedEndAt(), command.timezone());
        VisitOccurrence savedVisit = visitOccurrenceRepository.saveAndFlush(visit);
        if (nextCaregiver != null) {
            savedVisit.markAssigned();
            visitOccurrenceRepository.saveAndFlush(savedVisit);
            closeOpenShiftIfActive(actorMembership, savedVisit, OpenShiftStatus.CLAIMED_OR_ASSIGNED);
        }

        VisitRescheduleEvent saved = visitRescheduleEventRepository.saveAndFlush(VisitRescheduleEvent.create(
                savedVisit,
                previousCaregiver,
                nextCaregiver,
                actorMembership,
                previousStart,
                previousEnd,
                command.newPlannedStartAt(),
                command.newPlannedEndAt(),
                command.reason()));
        schedulingAuditService.recordVisitRescheduled(actorMembership, saved.getId(), savedVisit.getBranchId(), rescheduleMetadata(saved));
        return saved;
    }

    @Transactional
    public VisitCancellationEvent cancelVisit(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @Valid CancelVisitCommand command) {
        requirePermission(actorMembership, AgencyPermission.CANCEL_VISITS);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);

        caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visit.getId(), CaregiverAssignmentStatus.ACTIVE)
                .ifPresent(assignment -> {
                    assignment.cancel();
                    caregiverVisitAssignmentRepository.saveAndFlush(assignment);
                    schedulingAuditService.recordAssignmentRemoved(actorMembership, assignment.getId(), visit.getBranchId(), assignmentMetadata(assignment));
                });
        closeOpenShiftIfActive(actorMembership, visit, OpenShiftStatus.CANCELLED);

        visit.markCancelled();
        visitOccurrenceRepository.saveAndFlush(visit);

        VisitCancellationEvent saved = visitCancellationEventRepository.saveAndFlush(VisitCancellationEvent.create(
                visit,
                actorMembership,
                command.cancellationParty(),
                command.reason()));
        schedulingAuditService.recordVisitCancelled(actorMembership, saved.getId(), visit.getBranchId(), cancellationMetadata(saved));
        return saved;
    }

    private void closeOpenShiftIfActive(AgencyMembership actorMembership, VisitOccurrence visit, OpenShiftStatus closureStatus) {
        openShiftRepository.findFirstByVisitOccurrence_IdAndStatusOrderByOpenedAtDesc(visit.getId(), OpenShiftStatus.OPEN)
                .ifPresent(openShift -> {
                    if (closureStatus == OpenShiftStatus.CLAIMED_OR_ASSIGNED) {
                        openShift.claimOrAssign(actorMembership);
                    } else if (closureStatus == OpenShiftStatus.CANCELLED) {
                        openShift.cancel(actorMembership);
                    }
                    openShiftRepository.saveAndFlush(openShift);
                    schedulingAuditService.recordOpenShiftClosed(actorMembership, openShift.getId(), visit.getBranchId(), openShiftMetadata(openShift));
                });
    }

    private List<LocalDate> datesFor(RecurringVisitRule rule, LocalDate windowStart, LocalDate windowEnd) {
        if (rule.getStatus() != com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRuleStatus.ACTIVE) {
            return List.of();
        }
        LocalDate effectiveEnd = rule.getEffectiveEnd() == null ? windowEnd : rule.getEffectiveEnd().isBefore(windowEnd) ? rule.getEffectiveEnd() : windowEnd;
        LocalDate current = rule.getEffectiveStart().isAfter(windowStart) ? rule.getEffectiveStart() : windowStart;
        if (effectiveEnd.isBefore(current)) {
            return List.of();
        }
        Set<DayOfWeek> weekdays = rule.weekdays();
        if (weekdays.isEmpty() && rule.getCadence() != RecurringVisitCadence.DAILY) {
            weekdays = new LinkedHashSet<>();
            weekdays.add(rule.getEffectiveStart().getDayOfWeek());
        }
        List<LocalDate> dates = new ArrayList<>();
        while (!current.isAfter(effectiveEnd)) {
            if (matchesCadence(rule, current, weekdays)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private boolean matchesCadence(RecurringVisitRule rule, LocalDate date, Set<DayOfWeek> weekdays) {
        return switch (rule.getCadence()) {
            case DAILY -> true;
            case SELECTED_WEEKDAYS -> weekdays.contains(date.getDayOfWeek());
            case WEEKLY -> weekdays.contains(date.getDayOfWeek());
            case BIWEEKLY -> weekdays.contains(date.getDayOfWeek())
                    && ChronoUnit.WEEKS.between(rule.getEffectiveStart(), date) % 2 == 0;
        };
    }

    private static OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time, String timezone) {
        return ZonedDateTime.of(date, time, ZoneId.of(timezone)).toOffsetDateTime();
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedSchedulingActorException::new);
    }

    private Patient resolvePatient(UUID agencyId, UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new SchedulingEntityNotFoundException("Patient", patientId));
        if (!Objects.equals(patient.getAgencyId(), agencyId)) {
            throw new SchedulingEntityNotFoundException("Patient", patientId);
        }
        if (patient.getStatus() != PatientLifecycleStatus.ACTIVE) {
            throw new SchedulingConflictException("Only active patients can receive scheduled visits.");
        }
        return patient;
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .filter(branch -> Objects.equals(branch.getAgencyId(), agencyId))
                .orElseThrow(() -> new SchedulingEntityNotFoundException("Branch", branchId));
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> Objects.equals(serviceLine.getAgencyId(), agencyId))
                .orElseThrow(() -> new SchedulingEntityNotFoundException("ServiceLine", serviceLineId));
    }

    private VisitType resolveVisitType(UUID agencyId, UUID visitTypeId) {
        if (visitTypeId == null) {
            return null;
        }
        return visitTypeRepository.findById(visitTypeId)
                .filter(visitType -> Objects.equals(visitType.getAgencyId(), agencyId))
                .orElseThrow(() -> new SchedulingEntityNotFoundException("VisitType", visitTypeId));
    }

    private VisitOccurrence resolveVisit(UUID agencyId, UUID visitOccurrenceId) {
        return visitOccurrenceRepository.findByIdAndAgency_Id(visitOccurrenceId, agencyId)
                .orElseThrow(() -> new SchedulingEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
    }

    private CaregiverProfile resolveCaregiverProfile(UUID agencyId, UUID caregiverProfileId) {
        return caregiverProfileRepository.findByIdAndAgency_Id(caregiverProfileId, agencyId)
                .orElseThrow(() -> new SchedulingEntityNotFoundException("CaregiverProfile", caregiverProfileId));
    }

    private static String visitMetadata(VisitOccurrence visit) {
        return "{\"status\":\"" + visit.getStatus().name() + "\",\"plannedStartAt\":\"" + visit.getPlannedStartAt() + "\"}";
    }

    private static String recurringMetadata(RecurringVisitRule rule) {
        return "{\"status\":\"" + rule.getStatus().name() + "\",\"cadence\":\"" + rule.getCadence().name() + "\"}";
    }

    private static String assignmentMetadata(CaregiverVisitAssignment assignment) {
        return "{\"status\":\"" + assignment.getAssignmentStatus().name() + "\",\"visitOccurrenceId\":\"" + assignment.getVisitOccurrenceId() + "\"}";
    }

    private static String openShiftMetadata(OpenShift openShift) {
        return "{\"status\":\"" + openShift.getStatus().name() + "\",\"visitOccurrenceId\":\"" + openShift.getVisitOccurrenceId() + "\"}";
    }

    private static String rescheduleMetadata(VisitRescheduleEvent event) {
        return "{\"visitOccurrenceId\":\"" + event.getVisitOccurrence().getId() + "\",\"newPlannedStartAt\":\"" + event.getNewPlannedStartAt() + "\"}";
    }

    private static String cancellationMetadata(VisitCancellationEvent event) {
        return "{\"visitOccurrenceId\":\"" + event.getVisitOccurrence().getId() + "\",\"cancellationParty\":\""
                + (event.getCancellationParty() == null ? "" : event.getCancellationParty().name()) + "\"}";
    }

    public record ManageVisitCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            UUID visitTypeId,
            @NotNull OffsetDateTime plannedStartAt,
            @NotNull OffsetDateTime plannedEndAt,
            @NotNull String timezone,
            String priority,
            String creationMode,
            String notes) {
    }

    public record ManageRecurringVisitRuleCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            UUID visitTypeId,
            @NotNull RecurringVisitCadence cadence,
            Set<DayOfWeek> weekdays,
            @NotNull LocalDate effectiveStart,
            LocalDate effectiveEnd,
            @NotNull LocalTime plannedStartTime,
            @NotNull LocalTime plannedEndTime,
            @NotNull String timezone,
            String priority,
            String creationMode,
            String notes) {
    }

    public record AssignCaregiverCommand(
            @NotNull UUID caregiverProfileId,
            UUID branchId,
            String assignmentSource,
            String notes) {
    }

    public record ManageOpenShiftCommand(
            UUID branchId,
            String priority,
            String notes) {
    }

    public record RescheduleVisitCommand(
            @NotNull OffsetDateTime newPlannedStartAt,
            @NotNull OffsetDateTime newPlannedEndAt,
            @NotNull String timezone,
            UUID branchId,
            UUID newCaregiverProfileId,
            String reason) {
    }

    public record CancelVisitCommand(
            VisitCancellationParty cancellationParty,
            String reason) {
    }
}
