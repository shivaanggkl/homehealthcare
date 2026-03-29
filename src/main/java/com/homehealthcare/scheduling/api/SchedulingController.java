package com.homehealthcare.scheduling.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.CancelVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageOpenShiftCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageRecurringVisitRuleCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.RescheduleVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRulesService;
import com.homehealthcare.scheduling.application.SchedulingRulesService.CaregiverMatchResult;
import com.homehealthcare.scheduling.application.SchedulingRulesService.ConflictCheckCommand;
import com.homehealthcare.scheduling.application.SchedulingRulesService.SchedulingConflictEvaluation;
import com.homehealthcare.scheduling.foundation.SchedulingConflictOutcome;
import com.homehealthcare.scheduling.foundation.SchedulingTravelAwareness;
import com.homehealthcare.scheduling.foundation.SchedulingVisitStatus;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class SchedulingController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final SchedulingRecordService schedulingRecordService;
    private final SchedulingRulesService schedulingRulesService;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final OpenShiftRepository openShiftRepository;
    private final RecurringVisitRuleRepository recurringVisitRuleRepository;
    private final VisitRescheduleEventRepository visitRescheduleEventRepository;
    private final VisitCancellationEventRepository visitCancellationEventRepository;

    SchedulingController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            SchedulingRecordService schedulingRecordService,
            SchedulingRulesService schedulingRulesService,
            VisitOccurrenceRepository visitOccurrenceRepository,
            CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository,
            OpenShiftRepository openShiftRepository,
            RecurringVisitRuleRepository recurringVisitRuleRepository,
            VisitRescheduleEventRepository visitRescheduleEventRepository,
            VisitCancellationEventRepository visitCancellationEventRepository) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.schedulingRecordService = schedulingRecordService;
        this.schedulingRulesService = schedulingRulesService;
        this.visitOccurrenceRepository = visitOccurrenceRepository;
        this.caregiverVisitAssignmentRepository = caregiverVisitAssignmentRepository;
        this.openShiftRepository = openShiftRepository;
        this.recurringVisitRuleRepository = recurringVisitRuleRepository;
        this.visitRescheduleEventRepository = visitRescheduleEventRepository;
        this.visitCancellationEventRepository = visitCancellationEventRepository;
    }

    @GetMapping("/schedule-board")
    ScheduleBoardResponse board(
            @RequestParam(name = "view", defaultValue = "WEEK") ScheduleBoardView view,
            @RequestParam(name = "date") LocalDate date,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "caregiverId", required = false) UUID caregiverId,
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "status", required = false) SchedulingVisitStatus status,
            @RequestParam(name = "openShiftsOnly", defaultValue = "false") boolean openShiftsOnly) {
        var actorMembership = requirePermission(AgencyPermission.VIEW_SCHEDULING_WORKSPACE);
        Window window = windowFor(view, date);

        List<VisitOccurrence> visits = visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                .filter(visit -> !visit.getPlannedEndAt().isBefore(window.start()) && !visit.getPlannedStartAt().isAfter(window.end()))
                .filter(visit -> branchId == null || branchId.equals(visit.getBranchId()))
                .filter(visit -> patientId == null || patientId.equals(visit.getPatient().getId()))
                .filter(visit -> status == null || status == visit.getStatus())
                .toList();

        Map<UUID, CaregiverVisitAssignment> activeAssignmentsByVisitId = caregiverVisitAssignmentRepository.findAllByAgency_IdOrderByAssignedAtAsc(actorMembership.getAgencyId()).stream()
                .filter(assignment -> assignment.getAssignmentStatus() == com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus.ACTIVE)
                .collect(Collectors.toMap(CaregiverVisitAssignment::getVisitOccurrenceId, Function.identity(), (left, right) -> right));

        Map<UUID, OpenShift> activeOpenShiftsByVisitId = openShiftRepository.findAllByAgency_IdOrderByOpenedAtAsc(actorMembership.getAgencyId()).stream()
                .filter(openShift -> openShift.getStatus() == OpenShiftStatus.OPEN)
                .collect(Collectors.toMap(OpenShift::getVisitOccurrenceId, Function.identity(), (left, right) -> right));

        List<ScheduleBoardItemResponse> items = visits.stream()
                .filter(visit -> caregiverId == null || hasCaregiver(activeAssignmentsByVisitId.get(visit.getId()), caregiverId))
                .filter(visit -> !openShiftsOnly || activeOpenShiftsByVisitId.containsKey(visit.getId()))
                .map(visit -> toBoardItem(visit, activeAssignmentsByVisitId.get(visit.getId()), activeOpenShiftsByVisitId.get(visit.getId())))
                .toList();

        return new ScheduleBoardResponse(view, window.start().toLocalDate(), window.end().toLocalDate(), items);
    }

    @GetMapping("/schedule-visits")
    PagedResponse<VisitResponse> listVisits(
            @RequestParam(name = "status", required = false) SchedulingVisitStatus status,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "caregiverId", required = false) UUID caregiverId,
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        var actorMembership = requirePermission(AgencyPermission.VIEW_SCHEDULING_WORKSPACE);
        Map<UUID, CaregiverVisitAssignment> activeAssignmentsByVisitId = caregiverVisitAssignmentRepository.findAllByAgency_IdOrderByAssignedAtAsc(actorMembership.getAgencyId()).stream()
                .filter(assignment -> assignment.getAssignmentStatus() == com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus.ACTIVE)
                .collect(Collectors.toMap(CaregiverVisitAssignment::getVisitOccurrenceId, Function.identity(), (left, right) -> right));
        Map<UUID, OpenShift> activeOpenShiftsByVisitId = openShiftRepository.findAllByAgency_IdOrderByOpenedAtAsc(actorMembership.getAgencyId()).stream()
                .filter(openShift -> openShift.getStatus() == OpenShiftStatus.OPEN)
                .collect(Collectors.toMap(OpenShift::getVisitOccurrenceId, Function.identity(), (left, right) -> right));

        List<VisitResponse> filtered = visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                .filter(visit -> status == null || visit.getStatus() == status)
                .filter(visit -> branchId == null || branchId.equals(visit.getBranchId()))
                .filter(visit -> patientId == null || patientId.equals(visit.getPatient().getId()))
                .filter(visit -> caregiverId == null || hasCaregiver(activeAssignmentsByVisitId.get(visit.getId()), caregiverId))
                .map(visit -> toVisitResponse(visit, activeAssignmentsByVisitId.get(visit.getId()), activeOpenShiftsByVisitId.get(visit.getId())))
                .toList();
        return page(filtered, page, size);
    }

    @GetMapping("/schedule-visits/{visitId}")
    VisitResponse getVisit(@PathVariable UUID visitId) {
        var actorMembership = requirePermission(AgencyPermission.VIEW_SCHEDULING_WORKSPACE);
        VisitOccurrence visit = visitOccurrenceRepository.findByIdAndAgency_Id(visitId, actorMembership.getAgencyId())
                .orElseThrow(() -> new com.homehealthcare.scheduling.application.SchedulingEntityNotFoundException("VisitOccurrence", visitId));
        CaregiverVisitAssignment activeAssignment = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visitId, com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus.ACTIVE)
                .orElse(null);
        OpenShift activeOpenShift = openShiftRepository.findFirstByVisitOccurrence_IdAndStatusOrderByOpenedAtDesc(visitId, OpenShiftStatus.OPEN).orElse(null);
        return toVisitResponse(visit, activeAssignment, activeOpenShift);
    }

    @PostMapping("/schedule-visits")
    VisitResponse createVisit(@Valid @RequestBody ManageVisitRequest request) {
        VisitOccurrence saved = schedulingRecordService.createVisit(configurationActorResolver.requireActorMembership(), toVisitCommand(request));
        return toVisitResponse(saved, null, null);
    }

    @PutMapping("/schedule-visits/{visitId}")
    VisitResponse updateVisit(@PathVariable UUID visitId, @Valid @RequestBody ManageVisitRequest request) {
        VisitOccurrence saved = schedulingRecordService.updateVisit(configurationActorResolver.requireActorMembership(), visitId, toVisitCommand(request));
        CaregiverVisitAssignment activeAssignment = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visitId, com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus.ACTIVE)
                .orElse(null);
        OpenShift activeOpenShift = openShiftRepository.findFirstByVisitOccurrence_IdAndStatusOrderByOpenedAtDesc(visitId, OpenShiftStatus.OPEN).orElse(null);
        return toVisitResponse(saved, activeAssignment, activeOpenShift);
    }

    @PostMapping("/schedule-visits/{visitId}/assignments")
    AssignmentResponse assignCaregiver(@PathVariable UUID visitId, @Valid @RequestBody AssignCaregiverRequest request) {
        CaregiverVisitAssignment saved = schedulingRecordService.assignCaregiver(
                configurationActorResolver.requireActorMembership(),
                visitId,
                new AssignCaregiverCommand(request.caregiverProfileId(), request.branchId(), request.assignmentSource(), request.notes()));
        return toAssignmentResponse(saved);
    }

    @DeleteMapping("/schedule-visits/{visitId}/assignments/{assignmentId}")
    AssignmentResponse removeAssignment(
            @PathVariable UUID visitId,
            @PathVariable UUID assignmentId,
            @RequestParam(name = "convertToOpenShift", defaultValue = "false") boolean convertToOpenShift,
            @Valid @RequestBody(required = false) ManageOpenShiftRequest request) {
        CaregiverVisitAssignment saved = schedulingRecordService.removeAssignment(
                configurationActorResolver.requireActorMembership(),
                visitId,
                assignmentId,
                convertToOpenShift,
                request == null ? null : new ManageOpenShiftCommand(request.branchId(), request.priority(), request.notes()));
        return toAssignmentResponse(saved);
    }

    @PostMapping("/open-shifts/{openShiftId}/assign")
    AssignmentResponse assignFromOpenShift(@PathVariable UUID openShiftId, @Valid @RequestBody AssignCaregiverRequest request) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        OpenShift openShift = openShiftRepository.findByIdAndAgency_Id(openShiftId, actorMembership.getAgencyId())
                .orElseThrow(() -> new com.homehealthcare.scheduling.application.SchedulingEntityNotFoundException("OpenShift", openShiftId));
        if (openShift.getStatus() != OpenShiftStatus.OPEN) {
            throw new com.homehealthcare.scheduling.application.SchedulingConflictException("Only open shifts can be assigned from the open-shift pool.");
        }
        CaregiverVisitAssignment saved = schedulingRecordService.assignCaregiver(
                actorMembership,
                openShift.getVisitOccurrenceId(),
                new AssignCaregiverCommand(request.caregiverProfileId(), request.branchId(), request.assignmentSource(), request.notes()));
        return toAssignmentResponse(saved);
    }

    @PostMapping("/schedule-visits/{visitId}/open-shift")
    OpenShiftResponse openShift(@PathVariable UUID visitId, @Valid @RequestBody ManageOpenShiftRequest request) {
        OpenShift saved = schedulingRecordService.openShift(
                configurationActorResolver.requireActorMembership(),
                visitId,
                new ManageOpenShiftCommand(request.branchId(), request.priority(), request.notes()));
        return toOpenShiftResponse(saved);
    }

    @GetMapping("/schedule-visits/{visitId}/matches")
    List<CaregiverMatchResponse> matchCaregivers(
            @PathVariable UUID visitId,
            @RequestParam(name = "preferredLanguage", required = false) String preferredLanguage,
            @RequestParam(name = "enforcePatientOverlapCheck", defaultValue = "false") boolean enforcePatientOverlapCheck,
            @RequestParam(name = "requireAvailabilityFit", defaultValue = "true") boolean requireAvailabilityFit,
            @RequestParam(name = "requiredSkillIds", required = false) Set<UUID> requiredSkillIds,
            @RequestParam(name = "requiredCredentialTypes", required = false) Set<String> requiredCredentialTypes) {
        List<CaregiverMatchResult> matches = schedulingRulesService.matchCaregivers(
                configurationActorResolver.requireActorMembership(),
                visitId,
                new SchedulingRulesService.MatchCaregiversCommand(
                        requiredSkillIds == null ? Set.of() : requiredSkillIds,
                        requiredCredentialTypes == null ? Set.of() : requiredCredentialTypes,
                        preferredLanguage,
                        enforcePatientOverlapCheck,
                        requireAvailabilityFit,
                        null,
                        null));
        return matches.stream().map(SchedulingController::toCaregiverMatchResponse).toList();
    }

    @PostMapping("/schedule-visits/{visitId}/conflict-preview")
    ConflictPreviewResponse conflictPreview(@PathVariable UUID visitId, @Valid @RequestBody ConflictPreviewRequest request) {
        SchedulingConflictEvaluation evaluation = schedulingRulesService.previewAssignment(
                configurationActorResolver.requireActorMembership(),
                visitId,
                request.caregiverProfileId(),
                new ConflictCheckCommand(
                        request.requiredSkillIds() == null ? Set.of() : request.requiredSkillIds(),
                        request.requiredCredentialTypes() == null ? Set.of() : request.requiredCredentialTypes(),
                        request.preferredLanguage(),
                        request.enforcePatientOverlapCheck(),
                        request.requireAvailabilityFit(),
                        null,
                        null));
        return toConflictPreviewResponse(evaluation);
    }

    @GetMapping("/recurring-visits")
    List<RecurringVisitRuleResponse> listRecurringRules(@RequestParam(name = "patientId", required = false) UUID patientId) {
        var actorMembership = requirePermission(AgencyPermission.VIEW_SCHEDULING_WORKSPACE);
        return recurringVisitRuleRepository.findAll().stream()
                .filter(rule -> actorMembership.getAgencyId().equals(rule.getAgencyId()))
                .filter(rule -> patientId == null || patientId.equals(rule.getPatient().getId()))
                .sorted(Comparator.comparing(RecurringVisitRule::getEffectiveStart))
                .map(SchedulingController::toRecurringRuleResponse)
                .toList();
    }

    @PostMapping("/recurring-visits")
    RecurringVisitRuleResponse createRecurringRule(@Valid @RequestBody ManageRecurringVisitRuleRequest request) {
        RecurringVisitRule saved = schedulingRecordService.createRecurringRule(configurationActorResolver.requireActorMembership(), toRecurringCommand(request));
        return toRecurringRuleResponse(saved);
    }

    @PutMapping("/recurring-visits/{recurringRuleId}")
    RecurringVisitRuleResponse updateRecurringRule(@PathVariable UUID recurringRuleId, @Valid @RequestBody ManageRecurringVisitRuleRequest request) {
        RecurringVisitRule saved = schedulingRecordService.updateRecurringRule(configurationActorResolver.requireActorMembership(), recurringRuleId, toRecurringCommand(request));
        return toRecurringRuleResponse(saved);
    }

    @DeleteMapping("/recurring-visits/{recurringRuleId}")
    RecurringVisitRuleResponse deactivateRecurringRule(@PathVariable UUID recurringRuleId) {
        RecurringVisitRule saved = schedulingRecordService.deactivateRecurringRule(configurationActorResolver.requireActorMembership(), recurringRuleId);
        return toRecurringRuleResponse(saved);
    }

    @PostMapping("/recurring-visits/{recurringRuleId}/expand")
    List<VisitResponse> expandRecurringRule(
            @PathVariable UUID recurringRuleId,
            @Valid @RequestBody ExpandRecurringRuleRequest request) {
        return schedulingRecordService.expandRecurringRule(
                        configurationActorResolver.requireActorMembership(),
                        recurringRuleId,
                        request.windowStart(),
                        request.windowEnd()).stream()
                .map(visit -> toVisitResponse(visit, null, null))
                .toList();
    }

    @PostMapping("/schedule-visits/{visitId}/reschedule")
    RescheduleResponse reschedule(@PathVariable UUID visitId, @Valid @RequestBody RescheduleRequest request) {
        VisitRescheduleEvent saved = schedulingRecordService.rescheduleVisit(
                configurationActorResolver.requireActorMembership(),
                visitId,
                new RescheduleVisitCommand(
                        request.newPlannedStartAt(),
                        request.newPlannedEndAt(),
                        request.timezone(),
                        request.branchId(),
                        request.newCaregiverProfileId(),
                        request.reason()));
        return toRescheduleResponse(saved);
    }

    @PostMapping("/schedule-visits/{visitId}/cancel")
    CancellationResponse cancel(@PathVariable UUID visitId, @Valid @RequestBody CancelRequest request) {
        VisitCancellationEvent saved = schedulingRecordService.cancelVisit(
                configurationActorResolver.requireActorMembership(),
                visitId,
                new CancelVisitCommand(request.cancellationParty(), request.reason()));
        return toCancellationResponse(saved);
    }

    private AgencyMembership requirePermission(AgencyPermission permission) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, com.homehealthcare.scheduling.application.UnauthorizedSchedulingActorException::new);
        return actorMembership;
    }

    private static Window windowFor(ScheduleBoardView view, LocalDate date) {
        return switch (view) {
            case DAY -> new Window(date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()), date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
            case WEEK -> {
                LocalDate start = date.minusDays(date.getDayOfWeek().getValue() - 1L);
                yield new Window(start.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()), start.plusDays(7).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
            }
            case MONTH -> {
                YearMonth month = YearMonth.from(date);
                LocalDate start = month.atDay(1);
                yield new Window(start.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()), month.plusMonths(1).atDay(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
            }
        };
    }

    private static boolean hasCaregiver(CaregiverVisitAssignment assignment, UUID caregiverId) {
        return assignment != null && caregiverId.equals(assignment.getCaregiverProfileId());
    }

    private static PagedResponse<VisitResponse> page(List<VisitResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static ManageVisitCommand toVisitCommand(ManageVisitRequest request) {
        return new ManageVisitCommand(
                request.patientId(),
                request.branchId(),
                request.serviceLineId(),
                request.visitTypeId(),
                request.plannedStartAt(),
                request.plannedEndAt(),
                request.timezone(),
                request.priority(),
                request.creationMode(),
                request.notes());
    }

    private static ManageRecurringVisitRuleCommand toRecurringCommand(ManageRecurringVisitRuleRequest request) {
        return new ManageRecurringVisitRuleCommand(
                request.patientId(),
                request.branchId(),
                request.serviceLineId(),
                request.visitTypeId(),
                request.cadence(),
                request.weekdays() == null ? Set.of() : request.weekdays(),
                request.effectiveStart(),
                request.effectiveEnd(),
                request.plannedStartTime(),
                request.plannedEndTime(),
                request.timezone(),
                request.priority(),
                request.creationMode(),
                request.notes());
    }

    private static ScheduleBoardItemResponse toBoardItem(VisitOccurrence visit, CaregiverVisitAssignment activeAssignment, OpenShift activeOpenShift) {
        return new ScheduleBoardItemResponse(
                visit.getId(),
                visit.getPatient().getId(),
                visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName(),
                visit.getBranchId(),
                visit.getServiceLine() == null ? null : visit.getServiceLine().getId(),
                visit.getVisitType() == null ? null : visit.getVisitType().getId(),
                visit.getPlannedStartAt(),
                visit.getPlannedEndAt(),
                visit.getTimezone(),
                visit.getStatus(),
                visit.getPriority(),
                activeAssignment == null ? null : activeAssignment.getId(),
                activeAssignment == null ? null : activeAssignment.getCaregiverProfileId(),
                activeOpenShift != null);
    }

    private static VisitResponse toVisitResponse(VisitOccurrence visit, CaregiverVisitAssignment activeAssignment, OpenShift activeOpenShift) {
        return new VisitResponse(
                visit.getId(),
                visit.getAgencyId(),
                visit.getPatient().getId(),
                visit.getBranchId(),
                visit.getServiceLine() == null ? null : visit.getServiceLine().getId(),
                visit.getVisitType() == null ? null : visit.getVisitType().getId(),
                visit.getRecurringVisitRule() == null ? null : visit.getRecurringVisitRule().getId(),
                visit.getPlannedStartAt(),
                visit.getPlannedEndAt(),
                visit.getTimezone(),
                visit.getStatus(),
                visit.getPriority(),
                visit.getCreationMode(),
                visit.getNotes(),
                activeAssignment == null ? null : activeAssignment.getId(),
                activeAssignment == null ? null : activeAssignment.getCaregiverProfileId(),
                activeOpenShift == null ? null : activeOpenShift.getId());
    }

    private static AssignmentResponse toAssignmentResponse(CaregiverVisitAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getAgencyId(),
                assignment.getVisitOccurrenceId(),
                assignment.getCaregiverProfileId(),
                assignment.getBranch() == null ? null : assignment.getBranch().getId(),
                assignment.getAssignedAt(),
                assignment.getAssignmentStatus(),
                assignment.getAssignmentSource(),
                assignment.getNotes());
    }

    private static OpenShiftResponse toOpenShiftResponse(OpenShift openShift) {
        return new OpenShiftResponse(
                openShift.getId(),
                openShift.getAgencyId(),
                openShift.getVisitOccurrenceId(),
                openShift.getBranch() == null ? null : openShift.getBranch().getId(),
                openShift.getOpenedAt(),
                openShift.getClosedAt(),
                openShift.getStatus(),
                openShift.getPriority(),
                openShift.getNotes());
    }

    private static CaregiverMatchResponse toCaregiverMatchResponse(CaregiverMatchResult result) {
        return new CaregiverMatchResponse(
                result.caregiverProfileId(),
                result.caregiverDisplayName(),
                result.score(),
                result.outcome(),
                result.factors().stream().map(item -> new ConflictItemResponse(item.code(), item.outcome(), item.message())).toList(),
                toTravelResponse(result.travelAwareness()),
                toOvertimeResponse(result.overtimeEvaluation()));
    }

    private static ConflictPreviewResponse toConflictPreviewResponse(SchedulingConflictEvaluation evaluation) {
        return new ConflictPreviewResponse(
                evaluation.visitOccurrenceId(),
                evaluation.caregiverProfileId(),
                evaluation.outcome(),
                evaluation.items().stream().map(item -> new ConflictItemResponse(item.code(), item.outcome(), item.message())).toList(),
                toTravelResponse(evaluation.travelAwareness()),
                toOvertimeResponse(evaluation.overtimeEvaluation()));
    }

    private static TravelResponse toTravelResponse(SchedulingTravelAwareness travelAwareness) {
        return new TravelResponse(
                travelAwareness.estimatedTravelMinutes(),
                travelAwareness.gapMinutes(),
                travelAwareness.level(),
                travelAwareness.rationaleCode());
    }

    private static OvertimeResponse toOvertimeResponse(SchedulingRulesService.OvertimeEvaluation overtimeEvaluation) {
        return new OvertimeResponse(
                overtimeEvaluation.projectedScheduledMinutes(),
                overtimeEvaluation.proposedMinutes(),
                overtimeEvaluation.warningThresholdMinutes(),
                overtimeEvaluation.blockingThresholdMinutes(),
                overtimeEvaluation.outcome(),
                overtimeEvaluation.message());
    }

    private static RecurringVisitRuleResponse toRecurringRuleResponse(RecurringVisitRule rule) {
        return new RecurringVisitRuleResponse(
                rule.getId(),
                rule.getAgencyId(),
                rule.getPatient().getId(),
                rule.getBranch() == null ? null : rule.getBranch().getId(),
                rule.getServiceLine() == null ? null : rule.getServiceLine().getId(),
                rule.getVisitType() == null ? null : rule.getVisitType().getId(),
                rule.getCadence(),
                rule.weekdays(),
                rule.getEffectiveStart(),
                rule.getEffectiveEnd(),
                rule.getPlannedStartTime(),
                rule.getPlannedEndTime(),
                rule.getTimezone(),
                rule.getPriority(),
                rule.getCreationMode(),
                rule.getNotes(),
                rule.getStatus());
    }

    private static RescheduleResponse toRescheduleResponse(VisitRescheduleEvent event) {
        return new RescheduleResponse(
                event.getId(),
                event.getAgencyId(),
                event.getVisitOccurrence().getId(),
                event.getPreviousCaregiverProfile() == null ? null : event.getPreviousCaregiverProfile().getId(),
                event.getNewCaregiverProfile() == null ? null : event.getNewCaregiverProfile().getId(),
                event.getPreviousPlannedStartAt(),
                event.getPreviousPlannedEndAt(),
                event.getNewPlannedStartAt(),
                event.getNewPlannedEndAt(),
                event.getReason(),
                event.getRescheduledAt());
    }

    private static CancellationResponse toCancellationResponse(VisitCancellationEvent event) {
        return new CancellationResponse(
                event.getId(),
                event.getAgencyId(),
                event.getVisitOccurrence().getId(),
                event.getCancellationParty(),
                event.getReason(),
                event.getCancelledAt());
    }

    enum ScheduleBoardView {
        DAY,
        WEEK,
        MONTH
    }

    private record Window(OffsetDateTime start, OffsetDateTime end) {
    }

    record ScheduleBoardResponse(
            ScheduleBoardView view,
            LocalDate windowStart,
            LocalDate windowEnd,
            List<ScheduleBoardItemResponse> items) {
    }

    record ScheduleBoardItemResponse(
            UUID visitId,
            UUID patientId,
            String patientDisplayName,
            UUID branchId,
            UUID serviceLineId,
            UUID visitTypeId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            SchedulingVisitStatus status,
            String priority,
            UUID activeAssignmentId,
            UUID activeCaregiverProfileId,
            boolean openShift) {
    }

    record ManageVisitRequest(
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

    record VisitResponse(
            UUID id,
            UUID agencyId,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            UUID visitTypeId,
            UUID recurringVisitRuleId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            SchedulingVisitStatus status,
            String priority,
            String creationMode,
            String notes,
            UUID activeAssignmentId,
            UUID activeCaregiverProfileId,
            UUID openShiftId) {
    }

    record AssignCaregiverRequest(
            @NotNull UUID caregiverProfileId,
            UUID branchId,
            String assignmentSource,
            String notes) {
    }

    record AssignmentResponse(
            UUID id,
            UUID agencyId,
            UUID visitOccurrenceId,
            UUID caregiverProfileId,
            UUID branchId,
            OffsetDateTime assignedAt,
            com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus assignmentStatus,
            String assignmentSource,
            String notes) {
    }

    record ManageOpenShiftRequest(UUID branchId, String priority, String notes) {
    }

    record OpenShiftResponse(
            UUID id,
            UUID agencyId,
            UUID visitOccurrenceId,
            UUID branchId,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt,
            OpenShiftStatus status,
            String priority,
            String notes) {
    }

    record ConflictPreviewRequest(
            @NotNull UUID caregiverProfileId,
            Set<UUID> requiredSkillIds,
            Set<String> requiredCredentialTypes,
            String preferredLanguage,
            boolean enforcePatientOverlapCheck,
            boolean requireAvailabilityFit) {
    }

    record ConflictPreviewResponse(
            UUID visitOccurrenceId,
            UUID caregiverProfileId,
            SchedulingConflictOutcome outcome,
            List<ConflictItemResponse> items,
            TravelResponse travelAwareness,
            OvertimeResponse overtimeEvaluation) {
    }

    record CaregiverMatchResponse(
            UUID caregiverProfileId,
            String caregiverDisplayName,
            int score,
            SchedulingConflictOutcome outcome,
            List<ConflictItemResponse> factors,
            TravelResponse travelAwareness,
            OvertimeResponse overtimeEvaluation) {
    }

    record ConflictItemResponse(
            String code,
            SchedulingConflictOutcome outcome,
            String message) {
    }

    record TravelResponse(
            int estimatedTravelMinutes,
            int gapMinutes,
            com.homehealthcare.scheduling.foundation.TravelAwarenessLevel level,
            String rationaleCode) {
    }

    record OvertimeResponse(
            int projectedScheduledMinutes,
            int proposedMinutes,
            int warningThresholdMinutes,
            int blockingThresholdMinutes,
            SchedulingConflictOutcome outcome,
            String message) {
    }

    record ManageRecurringVisitRuleRequest(
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

    record RecurringVisitRuleResponse(
            UUID id,
            UUID agencyId,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            UUID visitTypeId,
            RecurringVisitCadence cadence,
            Set<DayOfWeek> weekdays,
            LocalDate effectiveStart,
            LocalDate effectiveEnd,
            LocalTime plannedStartTime,
            LocalTime plannedEndTime,
            String timezone,
            String priority,
            String creationMode,
            String notes,
            RecurringVisitRuleStatus status) {
    }

    record ExpandRecurringRuleRequest(@NotNull LocalDate windowStart, @NotNull LocalDate windowEnd) {
    }

    record RescheduleRequest(
            @NotNull OffsetDateTime newPlannedStartAt,
            @NotNull OffsetDateTime newPlannedEndAt,
            @NotNull String timezone,
            UUID branchId,
            UUID newCaregiverProfileId,
            String reason) {
    }

    record RescheduleResponse(
            UUID id,
            UUID agencyId,
            UUID visitOccurrenceId,
            UUID previousCaregiverProfileId,
            UUID newCaregiverProfileId,
            OffsetDateTime previousPlannedStartAt,
            OffsetDateTime previousPlannedEndAt,
            OffsetDateTime newPlannedStartAt,
            OffsetDateTime newPlannedEndAt,
            String reason,
            OffsetDateTime rescheduledAt) {
    }

    record CancelRequest(VisitCancellationParty cancellationParty, String reason) {
    }

    record CancellationResponse(
            UUID id,
            UUID agencyId,
            UUID visitOccurrenceId,
            VisitCancellationParty cancellationParty,
            String reason,
            OffsetDateTime cancelledAt) {
    }
}
