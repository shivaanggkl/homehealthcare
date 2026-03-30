package com.homehealthcare.careprogression.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.careprogression.domain.CarePlanSyncLink;
import com.homehealthcare.careprogression.domain.CarePlanSyncLinkRepository;
import com.homehealthcare.careprogression.domain.GoalIntervention;
import com.homehealthcare.careprogression.domain.GoalInterventionRepository;
import com.homehealthcare.careprogression.domain.GoalProgressNote;
import com.homehealthcare.careprogression.domain.GoalProgressNoteRepository;
import com.homehealthcare.careprogression.domain.GoalTemplate;
import com.homehealthcare.careprogression.domain.GoalTemplateRepository;
import com.homehealthcare.careprogression.domain.GoalVersionRecord;
import com.homehealthcare.careprogression.domain.GoalVersionRecordRepository;
import com.homehealthcare.careprogression.domain.PatientGoal;
import com.homehealthcare.careprogression.domain.PatientGoalRepository;
import com.homehealthcare.careprogression.foundation.CarePlanSyncStatus;
import com.homehealthcare.careprogression.foundation.CareProgressionAuditService;
import com.homehealthcare.careprogression.foundation.GoalInterventionLifecycleStatus;
import com.homehealthcare.careprogression.foundation.GoalTargetDatePosture;
import com.homehealthcare.careprogression.foundation.GoalTemplateLifecycleStatus;
import com.homehealthcare.careprogression.foundation.PatientGoalLifecycleStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CareProgressionService {

    private final GoalTemplateRepository goalTemplateRepository;
    private final PatientGoalRepository patientGoalRepository;
    private final GoalInterventionRepository goalInterventionRepository;
    private final GoalProgressNoteRepository goalProgressNoteRepository;
    private final GoalVersionRecordRepository goalVersionRecordRepository;
    private final CarePlanSyncLinkRepository carePlanSyncLinkRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final CareProgressionAuditService careProgressionAuditService;

    @Transactional(readOnly = true)
    public List<GoalTemplate> listGoalTemplates(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            GoalTemplateLifecycleStatus status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view goal templates");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, branchId, "view goal templates");
        }
        return goalTemplateRepository.findAllByAgency_IdOrderByNameAsc(actorMembership.getAgencyId()).stream()
                .filter(template -> status == null || template.getStatus() == status)
                .filter(template -> branchId == null || Objects.equals(template.getBranchId(), branchId))
                .filter(template -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, template.getBranchId()))
                .toList();
    }

    @Transactional
    public GoalTemplate createGoalTemplate(@NotNull AgencyMembership actorMembership, @Valid ManageGoalTemplateCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_TEMPLATES, "manage goal templates");
        Branch branch = resolveManagedBranch(actorMembership, AgencyPermission.MANAGE_GOAL_TEMPLATES, command.branchId(), "manage goal templates");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        GoalTemplate saved = goalTemplateRepository.saveAndFlush(GoalTemplate.create(
                actorMembership.getAgency(),
                branch,
                serviceLine,
                command.name(),
                command.description(),
                command.targetOutcomeGuidance(),
                command.defaultInterventionScaffold(),
                firstNonNull(command.status(), GoalTemplateLifecycleStatus.ACTIVE)));
        careProgressionAuditService.recordGoalTemplateSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        return saved;
    }

    @Transactional(readOnly = true)
    public GoalTemplate getGoalTemplate(@NotNull AgencyMembership actorMembership, @NotNull UUID goalTemplateId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view goal templates");
        GoalTemplate template = resolveGoalTemplate(actorMembership.getAgencyId(), goalTemplateId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, template.getBranchId(), "view goal templates");
        return template;
    }

    @Transactional
    public GoalTemplate updateGoalTemplate(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID goalTemplateId,
            @Valid ManageGoalTemplateCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_TEMPLATES, "manage goal templates");
        GoalTemplate template = resolveGoalTemplate(actorMembership.getAgencyId(), goalTemplateId);
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_GOAL_TEMPLATES,
                firstNonNull(command.branchId(), template.getBranchId()),
                "manage goal templates");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), firstNonNull(command.serviceLineId(), template.getServiceLineId()));
        template.updateDetails(
                branch,
                serviceLine,
                command.name(),
                command.description(),
                command.targetOutcomeGuidance(),
                command.defaultInterventionScaffold(),
                firstNonNull(command.status(), template.getStatus()));
        GoalTemplate saved = goalTemplateRepository.saveAndFlush(template);
        careProgressionAuditService.recordGoalTemplateSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        return saved;
    }

    @Transactional
    public GoalTemplate deactivateGoalTemplate(@NotNull AgencyMembership actorMembership, @NotNull UUID goalTemplateId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_TEMPLATES, "manage goal templates");
        GoalTemplate template = resolveGoalTemplate(actorMembership.getAgencyId(), goalTemplateId);
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_GOAL_TEMPLATES, template.getBranchId(), "manage goal templates");
        template.deactivate();
        GoalTemplate saved = goalTemplateRepository.saveAndFlush(template);
        careProgressionAuditService.recordGoalTemplateSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PatientGoal> listPatientGoals(
            @NotNull AgencyMembership actorMembership,
            UUID patientId,
            UUID branchId,
            PatientGoalLifecycleStatus status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view patient goals");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, branchId, "view patient goals");
        }
        if (patientId != null) {
            resolvePatient(actorMembership.getAgencyId(), patientId);
        }
        return patientGoalRepository.findAllByAgency_IdOrderByCreatedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(goal -> patientId == null || Objects.equals(goal.getPatientId(), patientId))
                .filter(goal -> branchId == null || Objects.equals(goal.getBranchId(), branchId))
                .filter(goal -> status == null || goal.getStatus() == status)
                .filter(goal -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId()))
                .toList();
    }

    @Transactional
    public PatientGoal createPatientGoal(@NotNull AgencyMembership actorMembership, @Valid ManagePatientGoalCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_GOALS, "manage patient goals");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_GOALS,
                command.branchId(),
                "manage patient goals");
        GoalTemplate goalTemplate = resolveOptionalGoalTemplate(actorMembership.getAgencyId(), command.goalTemplateId());
        AgencyMembership ownerMembership = resolveMembership(actorMembership.getAgencyId(), command.ownerMembershipId());
        PatientGoal saved = patientGoalRepository.saveAndFlush(PatientGoal.create(
                patient,
                branch,
                goalTemplate,
                ownerMembership,
                command.title(),
                command.description(),
                command.targetDate()));
        careProgressionAuditService.recordPatientGoalCreated(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        recordGoalVersion(actorMembership, saved, "GOAL_CREATED", OffsetDateTime.now());
        return saved;
    }

    @Transactional(readOnly = true)
    public PatientGoal getPatientGoal(@NotNull AgencyMembership actorMembership, @NotNull UUID patientGoalId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view patient goals");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId(), "view patient goals");
        return goal;
    }

    @Transactional
    public PatientGoal updatePatientGoal(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientGoalId,
            @Valid ManagePatientGoalCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_GOALS, "manage patient goals");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_GOALS,
                firstNonNull(command.branchId(), goal.getBranchId()),
                "manage patient goals");
        GoalTemplate goalTemplate = resolveOptionalGoalTemplate(actorMembership.getAgencyId(), firstNonNull(command.goalTemplateId(), goal.getGoalTemplateId()));
        AgencyMembership ownerMembership = resolveMembership(actorMembership.getAgencyId(), firstNonNull(command.ownerMembershipId(), goal.getOwnerMembershipId()));
        LocalDate previousTargetDate = goal.getTargetDate();
        goal.updateDetails(branch, goalTemplate, ownerMembership, command.title(), command.description(), command.targetDate());
        PatientGoal saved = patientGoalRepository.saveAndFlush(goal);
        careProgressionAuditService.recordPatientGoalUpdated(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        if (!Objects.equals(previousTargetDate, saved.getTargetDate())) {
            careProgressionAuditService.recordTargetDateChanged(
                    actorMembership,
                    saved.getId(),
                    saved.getBranchId(),
                    "{\"targetDate\":\"" + Objects.toString(saved.getTargetDate(), "") + "\"}");
        }
        recordGoalVersion(actorMembership, saved, "GOAL_UPDATED", OffsetDateTime.now());
        return saved;
    }

    @Transactional
    public PatientGoal transitionPatientGoalState(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientGoalId,
            @NotNull PatientGoalLifecycleStatus status,
            OffsetDateTime changedAt) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_STATE_TRANSITIONS, "manage patient goal state transitions");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_GOAL_STATE_TRANSITIONS, goal.getBranchId(), "manage patient goal state transitions");
        OffsetDateTime effectiveAt = firstNonNull(changedAt, OffsetDateTime.now());
        goal.transitionState(status, effectiveAt);
        PatientGoal saved = patientGoalRepository.saveAndFlush(goal);
        careProgressionAuditService.recordGoalStateChanged(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        recordGoalVersion(actorMembership, saved, "GOAL_STATE_CHANGED", effectiveAt);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GoalIntervention> listInterventions(@NotNull AgencyMembership actorMembership, @NotNull UUID patientGoalId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view goal interventions");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId(), "view goal interventions");
        return goalInterventionRepository.findAllByPatientGoal_IdOrderByTargetDateAscIdAsc(patientGoalId);
    }

    @Transactional
    public GoalIntervention createIntervention(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientGoalId,
            @Valid ManageInterventionCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_INTERVENTIONS, "manage goal interventions");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_GOAL_INTERVENTIONS,
                firstNonNull(command.branchId(), goal.getBranchId()),
                "manage goal interventions");
        AgencyMembership ownerMembership = resolveMembership(actorMembership.getAgencyId(), command.ownerMembershipId());
        GoalIntervention saved = goalInterventionRepository.saveAndFlush(GoalIntervention.create(
                goal,
                branch,
                ownerMembership,
                command.title(),
                command.description(),
                command.targetDate(),
                command.derivedFromTemplate()));
        careProgressionAuditService.recordInterventionSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        recordGoalVersion(actorMembership, goal, "INTERVENTION_CREATED", OffsetDateTime.now());
        return saved;
    }

    @Transactional
    public GoalIntervention updateIntervention(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID interventionId,
            @Valid ManageInterventionCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_INTERVENTIONS, "manage goal interventions");
        GoalIntervention intervention = resolveIntervention(actorMembership.getAgencyId(), interventionId);
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_GOAL_INTERVENTIONS,
                firstNonNull(command.branchId(), intervention.getBranchId()),
                "manage goal interventions");
        AgencyMembership ownerMembership = resolveMembership(actorMembership.getAgencyId(), firstNonNull(command.ownerMembershipId(), intervention.getOwnerMembershipId()));
        intervention.updateDetails(
                branch,
                ownerMembership,
                command.title(),
                command.description(),
                command.targetDate(),
                firstNonNull(command.status(), intervention.getStatus()));
        GoalIntervention saved = goalInterventionRepository.saveAndFlush(intervention);
        careProgressionAuditService.recordInterventionSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        recordGoalVersion(actorMembership, saved.getPatientGoal(), "INTERVENTION_UPDATED", OffsetDateTime.now());
        return saved;
    }

    @Transactional
    public GoalIntervention deactivateIntervention(@NotNull AgencyMembership actorMembership, @NotNull UUID interventionId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_GOAL_INTERVENTIONS, "manage goal interventions");
        GoalIntervention intervention = resolveIntervention(actorMembership.getAgencyId(), interventionId);
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_GOAL_INTERVENTIONS, intervention.getBranchId(), "manage goal interventions");
        intervention.deactivate();
        GoalIntervention saved = goalInterventionRepository.saveAndFlush(intervention);
        careProgressionAuditService.recordInterventionSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        recordGoalVersion(actorMembership, saved.getPatientGoal(), "INTERVENTION_DEACTIVATED", OffsetDateTime.now());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GoalProgressNote> listProgressNotes(@NotNull AgencyMembership actorMembership, @NotNull UUID patientGoalId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view goal progress notes");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId(), "view goal progress notes");
        return goalProgressNoteRepository.findAllByPatientGoal_IdOrderByCapturedAtDesc(patientGoalId);
    }

    @Transactional
    public GoalProgressNote addProgressNote(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientGoalId,
            @Valid AddProgressNoteCommand command) {
        requirePermission(actorMembership, AgencyPermission.ADD_GOAL_PROGRESS_NOTES, "add goal progress notes");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.ADD_GOAL_PROGRESS_NOTES, goal.getBranchId(), "add goal progress notes");
        GoalIntervention intervention = resolveOptionalIntervention(actorMembership.getAgencyId(), command.goalInterventionId(), patientGoalId);
        AgencyMembership capturedByMembership = resolveMembership(actorMembership.getAgencyId(), firstNonNull(command.capturedByMembershipId(), actorMembership.getId()));
        GoalProgressNote saved = goalProgressNoteRepository.saveAndFlush(GoalProgressNote.create(
                goal,
                intervention,
                resolveBranch(actorMembership.getAgencyId(), firstNonNull(command.branchId(), goal.getBranchId())),
                capturedByMembership,
                command.noteText(),
                firstNonNull(command.capturedAt(), OffsetDateTime.now()),
                command.progressionSummary(),
                command.statusImpact()));
        careProgressionAuditService.recordProgressNoteAdded(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"goalId\":\"" + goal.getId() + "\"}");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GoalVersionRecord> listGoalVersions(@NotNull AgencyMembership actorMembership, @NotNull UUID patientGoalId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view goal version history");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId(), "view goal version history");
        return goalVersionRecordRepository.findAllByPatientGoal_IdOrderByVersionNumberDesc(patientGoalId);
    }

    @Transactional(readOnly = true)
    public List<CarePlanSyncLink> listCarePlanSyncLinks(@NotNull AgencyMembership actorMembership, @NotNull UUID patientGoalId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view care-plan sync state");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId(), "view care-plan sync state");
        return carePlanSyncLinkRepository.findAllByPatientGoal_IdOrderByCareplanIdentifierAsc(patientGoalId);
    }

    @Transactional
    public CarePlanSyncLink createCarePlanSyncLink(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientGoalId,
            @Valid ManageCarePlanSyncCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREPLAN_SYNC, "manage care-plan sync");
        PatientGoal goal = resolvePatientGoal(actorMembership.getAgencyId(), patientGoalId);
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_CAREPLAN_SYNC,
                firstNonNull(command.branchId(), goal.getBranchId()),
                "manage care-plan sync");
        CarePlanSyncLink saved = carePlanSyncLinkRepository.saveAndFlush(CarePlanSyncLink.create(
                goal,
                branch,
                command.careplanIdentifier(),
                command.syncStatus(),
                command.lastSyncedAt(),
                command.syncSource()));
        careProgressionAuditService.recordCarePlanSyncUpdated(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"syncStatus\":\"" + saved.getSyncStatus().name() + "\"}");
        if (saved.getSyncStatus() == CarePlanSyncStatus.STALE || saved.getSyncStatus() == CarePlanSyncStatus.FAILED) {
            careProgressionAuditService.recordProgressionEventPublished(
                    actorMembership,
                    saved.getId(),
                    saved.getBranchId(),
                    "{\"event\":\"CAREPLAN_SYNC_DRIFT_DETECTED\"}");
        }
        return saved;
    }

    @Transactional
    public CarePlanSyncLink updateCarePlanSyncLink(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID carePlanSyncLinkId,
            @Valid ManageCarePlanSyncCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREPLAN_SYNC, "manage care-plan sync");
        CarePlanSyncLink link = resolveCarePlanSyncLink(actorMembership.getAgencyId(), carePlanSyncLinkId);
        Branch branch = resolveManagedBranch(
                actorMembership,
                AgencyPermission.MANAGE_CAREPLAN_SYNC,
                firstNonNull(command.branchId(), link.getBranchId()),
                "manage care-plan sync");
        link.updateState(
                branch,
                command.careplanIdentifier(),
                command.syncStatus(),
                command.lastSyncedAt(),
                command.syncSource());
        CarePlanSyncLink saved = carePlanSyncLinkRepository.saveAndFlush(link);
        careProgressionAuditService.recordCarePlanSyncUpdated(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"syncStatus\":\"" + saved.getSyncStatus().name() + "\"}");
        return saved;
    }

    @Transactional(readOnly = true)
    public PatientProgressionSummaryView getPatientProgressionSummary(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientId,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, "view progression summary");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, branchId, "view progression summary");
        }
        List<GoalSummaryView> goalSummaries = patientGoalRepository.findAllByAgency_IdAndPatient_IdOrderByCreatedAtDesc(actorMembership.getAgencyId(), patientId).stream()
                .filter(goal -> branchId == null || Objects.equals(goal.getBranchId(), branchId))
                .filter(goal -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_GOAL_WORKSPACE, goal.getBranchId()))
                .map(this::toGoalSummaryView)
                .toList();
        long overdueCount = goalSummaries.stream()
                .filter(summary -> summary.targetDatePosture() == GoalTargetDatePosture.OVERDUE)
                .count();
        long atRiskCount = goalSummaries.stream()
                .filter(summary -> summary.targetDatePosture() == GoalTargetDatePosture.AT_RISK)
                .count();
        return new PatientProgressionSummaryView(
                patient.getId(),
                branchId,
                goalSummaries.size(),
                overdueCount,
                atRiskCount,
                goalSummaries);
    }

    private GoalSummaryView toGoalSummaryView(PatientGoal goal) {
        List<GoalIntervention> interventions = goalInterventionRepository.findAllByPatientGoal_IdOrderByTargetDateAscIdAsc(goal.getId());
        List<CarePlanSyncLink> syncLinks = carePlanSyncLinkRepository.findAllByPatientGoal_IdOrderByCareplanIdentifierAsc(goal.getId());
        GoalProgressNote latestNote = goalProgressNoteRepository.findAllByPatientGoal_IdOrderByCapturedAtDesc(goal.getId()).stream()
                .findFirst()
                .orElse(null);
        long completedInterventionCount = interventions.stream()
                .filter(intervention -> intervention.getStatus() == GoalInterventionLifecycleStatus.COMPLETED)
                .count();
        return new GoalSummaryView(
                goal.getId(),
                goal.getBranchId(),
                goal.getTitle(),
                goal.getStatus(),
                goal.getTargetDate(),
                determinePosture(goal.getTargetDate()),
                latestNote == null ? null : latestNote.getProgressionSummary(),
                completedInterventionCount,
                interventions.size(),
                syncLinks.stream().map(CarePlanSyncLink::getSyncStatus).findFirst().orElse(null));
    }

    private GoalTargetDatePosture determinePosture(LocalDate targetDate) {
        if (targetDate == null) {
            return GoalTargetDatePosture.NOT_APPLICABLE;
        }
        LocalDate today = LocalDate.now();
        if (targetDate.isBefore(today)) {
            return GoalTargetDatePosture.OVERDUE;
        }
        if (!targetDate.isAfter(today.plusDays(7))) {
            return GoalTargetDatePosture.AT_RISK;
        }
        return GoalTargetDatePosture.ON_TRACK;
    }

    private void recordGoalVersion(
            AgencyMembership actorMembership,
            PatientGoal goal,
            String changeType,
            OffsetDateTime changedAt) {
        int nextVersionNumber = goalVersionRecordRepository.findTopByPatientGoal_IdOrderByVersionNumberDesc(goal.getId())
                .map(GoalVersionRecord::getVersionNumber)
                .orElse(0) + 1;
        GoalVersionRecord version = goalVersionRecordRepository.saveAndFlush(GoalVersionRecord.create(
                goal,
                resolveBranch(goal.getAgencyId(), goal.getBranchId()),
                actorMembership,
                nextVersionNumber,
                changeType,
                changedAt,
                goalSnapshotJson(goal)));
        careProgressionAuditService.recordGoalVersionRecorded(
                actorMembership,
                version.getId(),
                version.getBranchId(),
                "{\"versionNumber\":" + version.getVersionNumber() + "}");
    }

    private String goalSnapshotJson(PatientGoal goal) {
        return "{"
                + "\"goalId\":\"" + goal.getId() + "\","
                + "\"patientId\":\"" + goal.getPatientId() + "\","
                + "\"branchId\":\"" + Objects.toString(goal.getBranchId(), "") + "\","
                + "\"title\":\"" + json(goal.getTitle()) + "\","
                + "\"status\":\"" + goal.getStatus().name() + "\","
                + "\"targetDate\":\"" + Objects.toString(goal.getTargetDate(), "") + "\""
                + "}";
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private GoalTemplate resolveGoalTemplate(UUID agencyId, UUID goalTemplateId) {
        return goalTemplateRepository.findById(goalTemplateId)
                .filter(template -> Objects.equals(template.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("GoalTemplate %s was not found.".formatted(goalTemplateId)));
    }

    private GoalTemplate resolveOptionalGoalTemplate(UUID agencyId, UUID goalTemplateId) {
        if (goalTemplateId == null) {
            return null;
        }
        return resolveGoalTemplate(agencyId, goalTemplateId);
    }

    private PatientGoal resolvePatientGoal(UUID agencyId, UUID patientGoalId) {
        return patientGoalRepository.findById(patientGoalId)
                .filter(goal -> Objects.equals(goal.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("PatientGoal %s was not found.".formatted(patientGoalId)));
    }

    private GoalIntervention resolveIntervention(UUID agencyId, UUID interventionId) {
        return goalInterventionRepository.findById(interventionId)
                .filter(intervention -> Objects.equals(intervention.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("GoalIntervention %s was not found.".formatted(interventionId)));
    }

    private GoalIntervention resolveOptionalIntervention(UUID agencyId, UUID interventionId, UUID patientGoalId) {
        if (interventionId == null) {
            return null;
        }
        GoalIntervention intervention = resolveIntervention(agencyId, interventionId);
        if (!Objects.equals(intervention.getPatientGoalId(), patientGoalId)) {
            throw new CareProgressionConflictException("The requested intervention does not belong to the selected patient goal.");
        }
        return intervention;
    }

    private CarePlanSyncLink resolveCarePlanSyncLink(UUID agencyId, UUID carePlanSyncLinkId) {
        return carePlanSyncLinkRepository.findById(carePlanSyncLinkId)
                .filter(link -> Objects.equals(link.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("CarePlanSyncLink %s was not found.".formatted(carePlanSyncLinkId)));
    }

    private Patient resolvePatient(UUID agencyId, UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(patient -> Objects.equals(patient.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("Patient %s was not found.".formatted(patientId)));
    }

    private Branch resolveManagedBranch(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (branchId == null) {
            if (actorMembership.getRole() != AgencyRole.AGENCY_OWNER) {
                throw new UnauthorizedCareProgressionActorException(actorMembership.getId(), action);
            }
            return null;
        }
        Branch branch = resolveBranch(actorMembership.getAgencyId(), branchId);
        requireBranchAccess(actorMembership, permission, branchId, action);
        return branch;
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("Branch %s was not found.".formatted(branchId)));
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> Objects.equals(serviceLine.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("ServiceLine %s was not found.".formatted(serviceLineId)));
    }

    private AgencyMembership resolveMembership(UUID agencyId, UUID membershipId) {
        if (membershipId == null) {
            return null;
        }
        return agencyMembershipRepository.findById(membershipId)
                .filter(membership -> Objects.equals(membership.getAgencyId(), agencyId))
                .orElseThrow(() -> new CareProgressionEntityNotFoundException("AgencyMembership %s was not found.".formatted(membershipId)));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission, String action) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                permission,
                membershipId -> new UnauthorizedCareProgressionActorException(membershipId, action));
    }

    private void requireBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (!hasBranchAccess(actorMembership, permission, branchId)) {
            throw new UnauthorizedCareProgressionActorException(actorMembership.getId(), action);
        }
    }

    private boolean hasBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId) {
        if (!agencyAuthorizationGuard.hasPermission(actorMembership, permission)) {
            return false;
        }
        if (branchId == null) {
            return actorMembership.getRole() == AgencyRole.AGENCY_OWNER || actorMembership.getRole() == AgencyRole.READ_ONLY_AUDITOR;
        }
        if (actorMembership.getRole() == AgencyRole.AGENCY_OWNER || actorMembership.getRole() == AgencyRole.READ_ONLY_AUDITOR) {
            return true;
        }
        return branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                actorMembership.getId(),
                branchId,
                BranchAssignmentStatus.ACTIVE);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    public record ManageGoalTemplateCommand(
            UUID branchId,
            UUID serviceLineId,
            @NotBlank String name,
            String description,
            String targetOutcomeGuidance,
            String defaultInterventionScaffold,
            GoalTemplateLifecycleStatus status) {}

    public record ManagePatientGoalCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID goalTemplateId,
            UUID ownerMembershipId,
            @NotBlank String title,
            String description,
            LocalDate targetDate,
            OffsetDateTime createdAt) {}

    public record ManageInterventionCommand(
            UUID branchId,
            UUID ownerMembershipId,
            @NotBlank String title,
            String description,
            LocalDate targetDate,
            GoalInterventionLifecycleStatus status,
            boolean derivedFromTemplate) {}

    public record AddProgressNoteCommand(
            UUID goalInterventionId,
            UUID branchId,
            UUID capturedByMembershipId,
            @NotBlank String noteText,
            OffsetDateTime capturedAt,
            String progressionSummary,
            String statusImpact) {}

    public record ManageCarePlanSyncCommand(
            UUID branchId,
            @NotBlank String careplanIdentifier,
            @NotNull CarePlanSyncStatus syncStatus,
            OffsetDateTime lastSyncedAt,
            String syncSource) {}

    public record GoalSummaryView(
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

    public record PatientProgressionSummaryView(
            UUID patientId,
            UUID branchId,
            int totalGoalCount,
            long overdueGoalCount,
            long atRiskGoalCount,
            List<GoalSummaryView> goals) {}
}
