package com.homehealthcare.careprogression.api;

import com.homehealthcare.careprogression.application.CareProgressionService;
import com.homehealthcare.careprogression.application.CareProgressionService.AddProgressNoteCommand;
import com.homehealthcare.careprogression.application.CareProgressionService.GoalSummaryView;
import com.homehealthcare.careprogression.application.CareProgressionService.ManageCarePlanSyncCommand;
import com.homehealthcare.careprogression.application.CareProgressionService.ManageGoalTemplateCommand;
import com.homehealthcare.careprogression.application.CareProgressionService.ManageInterventionCommand;
import com.homehealthcare.careprogression.application.CareProgressionService.ManagePatientGoalCommand;
import com.homehealthcare.careprogression.application.CareProgressionService.PatientProgressionSummaryView;
import com.homehealthcare.careprogression.domain.CarePlanSyncLink;
import com.homehealthcare.careprogression.domain.GoalIntervention;
import com.homehealthcare.careprogression.domain.GoalProgressNote;
import com.homehealthcare.careprogression.domain.GoalTemplate;
import com.homehealthcare.careprogression.domain.GoalVersionRecord;
import com.homehealthcare.careprogression.domain.PatientGoal;
import com.homehealthcare.careprogression.foundation.CarePlanSyncStatus;
import com.homehealthcare.careprogression.foundation.GoalInterventionLifecycleStatus;
import com.homehealthcare.careprogression.foundation.GoalTargetDatePosture;
import com.homehealthcare.careprogression.foundation.GoalTemplateLifecycleStatus;
import com.homehealthcare.careprogression.foundation.PatientGoalLifecycleStatus;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/care-progression")
class CareProgressionController {

    private final ConfigurationActorResolver actorResolver;
    private final CareProgressionService careProgressionService;

    CareProgressionController(ConfigurationActorResolver actorResolver, CareProgressionService careProgressionService) {
        this.actorResolver = actorResolver;
        this.careProgressionService = careProgressionService;
    }

    @GetMapping("/goal-templates")
    List<GoalTemplateResponse> listGoalTemplates(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) GoalTemplateLifecycleStatus status) {
        return careProgressionService.listGoalTemplates(actorResolver.requireActorMembership(), branchId, status).stream()
                .map(CareProgressionController::toGoalTemplateResponse)
                .toList();
    }

    @PostMapping("/goal-templates")
    GoalTemplateResponse createGoalTemplate(@Valid @RequestBody SaveGoalTemplateRequest request) {
        return toGoalTemplateResponse(careProgressionService.createGoalTemplate(
                actorResolver.requireActorMembership(),
                new ManageGoalTemplateCommand(
                        request.branchId(),
                        request.serviceLineId(),
                        request.name(),
                        request.description(),
                        request.targetOutcomeGuidance(),
                        request.defaultInterventionScaffold(),
                        request.status())));
    }

    @GetMapping("/goal-templates/{goalTemplateId}")
    GoalTemplateResponse getGoalTemplate(@PathVariable UUID goalTemplateId) {
        return toGoalTemplateResponse(careProgressionService.getGoalTemplate(actorResolver.requireActorMembership(), goalTemplateId));
    }

    @PutMapping("/goal-templates/{goalTemplateId}")
    GoalTemplateResponse updateGoalTemplate(@PathVariable UUID goalTemplateId, @Valid @RequestBody SaveGoalTemplateRequest request) {
        return toGoalTemplateResponse(careProgressionService.updateGoalTemplate(
                actorResolver.requireActorMembership(),
                goalTemplateId,
                new ManageGoalTemplateCommand(
                        request.branchId(),
                        request.serviceLineId(),
                        request.name(),
                        request.description(),
                        request.targetOutcomeGuidance(),
                        request.defaultInterventionScaffold(),
                        request.status())));
    }

    @DeleteMapping("/goal-templates/{goalTemplateId}")
    GoalTemplateResponse deactivateGoalTemplate(@PathVariable UUID goalTemplateId) {
        return toGoalTemplateResponse(careProgressionService.deactivateGoalTemplate(actorResolver.requireActorMembership(), goalTemplateId));
    }

    @GetMapping("/patient-goals")
    List<PatientGoalResponse> listPatientGoals(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) PatientGoalLifecycleStatus status) {
        return careProgressionService.listPatientGoals(actorResolver.requireActorMembership(), patientId, branchId, status).stream()
                .map(CareProgressionController::toPatientGoalResponse)
                .toList();
    }

    @PostMapping("/patient-goals")
    PatientGoalResponse createPatientGoal(@Valid @RequestBody SavePatientGoalRequest request) {
        return toPatientGoalResponse(careProgressionService.createPatientGoal(
                actorResolver.requireActorMembership(),
                new ManagePatientGoalCommand(
                        request.patientId(),
                        request.branchId(),
                        request.goalTemplateId(),
                        request.ownerMembershipId(),
                        request.title(),
                        request.description(),
                        request.targetDate(),
                        request.createdAt())));
    }

    @GetMapping("/patient-goals/{patientGoalId}")
    PatientGoalResponse getPatientGoal(@PathVariable UUID patientGoalId) {
        return toPatientGoalResponse(careProgressionService.getPatientGoal(actorResolver.requireActorMembership(), patientGoalId));
    }

    @PutMapping("/patient-goals/{patientGoalId}")
    PatientGoalResponse updatePatientGoal(@PathVariable UUID patientGoalId, @Valid @RequestBody SavePatientGoalRequest request) {
        return toPatientGoalResponse(careProgressionService.updatePatientGoal(
                actorResolver.requireActorMembership(),
                patientGoalId,
                new ManagePatientGoalCommand(
                        request.patientId(),
                        request.branchId(),
                        request.goalTemplateId(),
                        request.ownerMembershipId(),
                        request.title(),
                        request.description(),
                        request.targetDate(),
                        request.createdAt())));
    }

    @PostMapping("/patient-goals/{patientGoalId}/state-transitions")
    PatientGoalResponse transitionPatientGoalState(@PathVariable UUID patientGoalId, @Valid @RequestBody GoalStateTransitionRequest request) {
        return toPatientGoalResponse(careProgressionService.transitionPatientGoalState(
                actorResolver.requireActorMembership(),
                patientGoalId,
                request.status(),
                request.changedAt()));
    }

    @GetMapping("/patient-goals/{patientGoalId}/interventions")
    List<GoalInterventionResponse> listInterventions(@PathVariable UUID patientGoalId) {
        return careProgressionService.listInterventions(actorResolver.requireActorMembership(), patientGoalId).stream()
                .map(CareProgressionController::toGoalInterventionResponse)
                .toList();
    }

    @PostMapping("/patient-goals/{patientGoalId}/interventions")
    GoalInterventionResponse createIntervention(@PathVariable UUID patientGoalId, @Valid @RequestBody SaveInterventionRequest request) {
        return toGoalInterventionResponse(careProgressionService.createIntervention(
                actorResolver.requireActorMembership(),
                patientGoalId,
                new ManageInterventionCommand(
                        request.branchId(),
                        request.ownerMembershipId(),
                        request.title(),
                        request.description(),
                        request.targetDate(),
                        request.status(),
                        request.derivedFromTemplate())));
    }

    @PutMapping("/interventions/{interventionId}")
    GoalInterventionResponse updateIntervention(@PathVariable UUID interventionId, @Valid @RequestBody SaveInterventionRequest request) {
        return toGoalInterventionResponse(careProgressionService.updateIntervention(
                actorResolver.requireActorMembership(),
                interventionId,
                new ManageInterventionCommand(
                        request.branchId(),
                        request.ownerMembershipId(),
                        request.title(),
                        request.description(),
                        request.targetDate(),
                        request.status(),
                        request.derivedFromTemplate())));
    }

    @DeleteMapping("/interventions/{interventionId}")
    GoalInterventionResponse deactivateIntervention(@PathVariable UUID interventionId) {
        return toGoalInterventionResponse(careProgressionService.deactivateIntervention(actorResolver.requireActorMembership(), interventionId));
    }

    @GetMapping("/patient-goals/{patientGoalId}/progress-notes")
    List<GoalProgressNoteResponse> listProgressNotes(@PathVariable UUID patientGoalId) {
        return careProgressionService.listProgressNotes(actorResolver.requireActorMembership(), patientGoalId).stream()
                .map(CareProgressionController::toGoalProgressNoteResponse)
                .toList();
    }

    @PostMapping("/patient-goals/{patientGoalId}/progress-notes")
    GoalProgressNoteResponse addProgressNote(@PathVariable UUID patientGoalId, @Valid @RequestBody AddProgressNoteRequest request) {
        return toGoalProgressNoteResponse(careProgressionService.addProgressNote(
                actorResolver.requireActorMembership(),
                patientGoalId,
                new AddProgressNoteCommand(
                        request.goalInterventionId(),
                        request.branchId(),
                        request.capturedByMembershipId(),
                        request.noteText(),
                        request.capturedAt(),
                        request.progressionSummary(),
                        request.statusImpact())));
    }

    @GetMapping("/patient-goals/{patientGoalId}/versions")
    List<GoalVersionResponse> listVersions(@PathVariable UUID patientGoalId) {
        return careProgressionService.listGoalVersions(actorResolver.requireActorMembership(), patientGoalId).stream()
                .map(CareProgressionController::toGoalVersionResponse)
                .toList();
    }

    @GetMapping("/patient-goals/{patientGoalId}/careplan-sync")
    List<CarePlanSyncResponse> listCarePlanSyncLinks(@PathVariable UUID patientGoalId) {
        return careProgressionService.listCarePlanSyncLinks(actorResolver.requireActorMembership(), patientGoalId).stream()
                .map(CareProgressionController::toCarePlanSyncResponse)
                .toList();
    }

    @PostMapping("/patient-goals/{patientGoalId}/careplan-sync")
    CarePlanSyncResponse createCarePlanSyncLink(@PathVariable UUID patientGoalId, @Valid @RequestBody SaveCarePlanSyncRequest request) {
        return toCarePlanSyncResponse(careProgressionService.createCarePlanSyncLink(
                actorResolver.requireActorMembership(),
                patientGoalId,
                new ManageCarePlanSyncCommand(
                        request.branchId(),
                        request.careplanIdentifier(),
                        request.syncStatus(),
                        request.lastSyncedAt(),
                        request.syncSource())));
    }

    @PutMapping("/careplan-sync/{carePlanSyncLinkId}")
    CarePlanSyncResponse updateCarePlanSyncLink(@PathVariable UUID carePlanSyncLinkId, @Valid @RequestBody SaveCarePlanSyncRequest request) {
        return toCarePlanSyncResponse(careProgressionService.updateCarePlanSyncLink(
                actorResolver.requireActorMembership(),
                carePlanSyncLinkId,
                new ManageCarePlanSyncCommand(
                        request.branchId(),
                        request.careplanIdentifier(),
                        request.syncStatus(),
                        request.lastSyncedAt(),
                        request.syncSource())));
    }

    @GetMapping("/patients/{patientId}/summary")
    PatientProgressionSummaryResponse getPatientProgressionSummary(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return toPatientProgressionSummaryResponse(careProgressionService.getPatientProgressionSummary(
                actorResolver.requireActorMembership(),
                patientId,
                branchId));
    }

    private static GoalTemplateResponse toGoalTemplateResponse(GoalTemplate template) {
        return new GoalTemplateResponse(
                template.getId(),
                template.getBranchId(),
                template.getServiceLineId(),
                template.getName(),
                template.getDescription(),
                template.getTargetOutcomeGuidance(),
                template.getDefaultInterventionScaffold(),
                template.getStatus());
    }

    private static PatientGoalResponse toPatientGoalResponse(PatientGoal goal) {
        return new PatientGoalResponse(
                goal.getId(),
                goal.getPatientId(),
                goal.getBranchId(),
                goal.getGoalTemplateId(),
                goal.getOwnerMembershipId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getResolvedAt());
    }

    private static GoalInterventionResponse toGoalInterventionResponse(GoalIntervention intervention) {
        return new GoalInterventionResponse(
                intervention.getId(),
                intervention.getPatientGoalId(),
                intervention.getBranchId(),
                intervention.getOwnerMembershipId(),
                intervention.getTitle(),
                intervention.getDescription(),
                intervention.getTargetDate(),
                intervention.getStatus(),
                intervention.isDerivedFromTemplate());
    }

    private static GoalProgressNoteResponse toGoalProgressNoteResponse(GoalProgressNote note) {
        return new GoalProgressNoteResponse(
                note.getId(),
                note.getPatientGoalId(),
                note.getGoalInterventionId(),
                note.getBranchId(),
                note.getCapturedByMembership().getId(),
                note.getNoteText(),
                note.getCapturedAt(),
                note.getProgressionSummary(),
                note.getStatusImpact(),
                note.getLifecycleStatus().name());
    }

    private static GoalVersionResponse toGoalVersionResponse(GoalVersionRecord version) {
        return new GoalVersionResponse(
                version.getId(),
                version.getPatientGoalId(),
                version.getBranchId(),
                version.getVersionNumber(),
                version.getChangeType(),
                version.getChangedAt(),
                version.getSnapshotJson());
    }

    private static CarePlanSyncResponse toCarePlanSyncResponse(CarePlanSyncLink link) {
        return new CarePlanSyncResponse(
                link.getId(),
                link.getPatientGoalId(),
                link.getBranchId(),
                link.getCareplanIdentifier(),
                link.getSyncStatus(),
                link.getLastSyncedAt(),
                link.getSyncSource());
    }

    private static PatientProgressionSummaryResponse toPatientProgressionSummaryResponse(PatientProgressionSummaryView summary) {
        return new PatientProgressionSummaryResponse(
                summary.patientId(),
                summary.branchId(),
                summary.totalGoalCount(),
                summary.overdueGoalCount(),
                summary.atRiskGoalCount(),
                summary.goals().stream().map(CareProgressionController::toGoalSummaryResponse).toList());
    }

    private static GoalSummaryResponse toGoalSummaryResponse(GoalSummaryView summary) {
        return new GoalSummaryResponse(
                summary.goalId(),
                summary.branchId(),
                summary.title(),
                summary.status(),
                summary.targetDate(),
                summary.targetDatePosture(),
                summary.latestProgressSummary(),
                summary.completedInterventionCount(),
                summary.totalInterventionCount(),
                summary.carePlanSyncStatus());
    }

    record SaveGoalTemplateRequest(
            UUID branchId,
            UUID serviceLineId,
            @NotBlank String name,
            String description,
            String targetOutcomeGuidance,
            String defaultInterventionScaffold,
            GoalTemplateLifecycleStatus status) {}

    record SavePatientGoalRequest(
            @NotNull UUID patientId,
            UUID branchId,
            UUID goalTemplateId,
            UUID ownerMembershipId,
            @NotBlank String title,
            String description,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdAt) {}

    record GoalStateTransitionRequest(
            @NotNull PatientGoalLifecycleStatus status,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime changedAt) {}

    record SaveInterventionRequest(
            UUID branchId,
            UUID ownerMembershipId,
            @NotBlank String title,
            String description,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            GoalInterventionLifecycleStatus status,
            boolean derivedFromTemplate) {}

    record AddProgressNoteRequest(
            UUID goalInterventionId,
            UUID branchId,
            UUID capturedByMembershipId,
            @NotBlank String noteText,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime capturedAt,
            String progressionSummary,
            String statusImpact) {}

    record SaveCarePlanSyncRequest(
            UUID branchId,
            @NotBlank String careplanIdentifier,
            @NotNull CarePlanSyncStatus syncStatus,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime lastSyncedAt,
            String syncSource) {}

    record GoalTemplateResponse(
            UUID id,
            UUID branchId,
            UUID serviceLineId,
            String name,
            String description,
            String targetOutcomeGuidance,
            String defaultInterventionScaffold,
            GoalTemplateLifecycleStatus status) {}

    record PatientGoalResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID goalTemplateId,
            UUID ownerMembershipId,
            String title,
            String description,
            LocalDate targetDate,
            PatientGoalLifecycleStatus status,
            Instant createdAt,
            OffsetDateTime resolvedAt) {}

    record GoalInterventionResponse(
            UUID id,
            UUID patientGoalId,
            UUID branchId,
            UUID ownerMembershipId,
            String title,
            String description,
            LocalDate targetDate,
            GoalInterventionLifecycleStatus status,
            boolean derivedFromTemplate) {}

    record GoalProgressNoteResponse(
            UUID id,
            UUID patientGoalId,
            UUID goalInterventionId,
            UUID branchId,
            UUID capturedByMembershipId,
            String noteText,
            OffsetDateTime capturedAt,
            String progressionSummary,
            String statusImpact,
            String lifecycleStatus) {}

    record GoalVersionResponse(
            UUID id,
            UUID patientGoalId,
            UUID branchId,
            int versionNumber,
            String changeType,
            OffsetDateTime changedAt,
            String snapshotJson) {}

    record CarePlanSyncResponse(
            UUID id,
            UUID patientGoalId,
            UUID branchId,
            String careplanIdentifier,
            CarePlanSyncStatus syncStatus,
            OffsetDateTime lastSyncedAt,
            String syncSource) {}

    record PatientProgressionSummaryResponse(
            UUID patientId,
            UUID branchId,
            int totalGoalCount,
            long overdueGoalCount,
            long atRiskGoalCount,
            List<GoalSummaryResponse> goals) {}

    record GoalSummaryResponse(
            UUID goalId,
            UUID branchId,
            String title,
            PatientGoalLifecycleStatus status,
            LocalDate targetDate,
            GoalTargetDatePosture targetDatePosture,
            String latestProgressSummary,
            long completedInterventionCount,
            int totalInterventionCount,
            CarePlanSyncStatus carePlanSyncStatus) {}
}
