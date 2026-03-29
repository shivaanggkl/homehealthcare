package com.homehealthcare.mobile.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileQuickNoteEntry;
import com.homehealthcare.mobile.domain.MobileQuickNoteEntryRepository;
import com.homehealthcare.mobile.domain.MobileQuickNoteStatus;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSessionRepository;
import com.homehealthcare.mobile.domain.MobileVisitTaskChecklistEntry;
import com.homehealthcare.mobile.domain.MobileVisitTaskChecklistEntryRepository;
import com.homehealthcare.mobile.foundation.MobileAuditService;
import com.homehealthcare.mobile.foundation.MobileExecutionSessionStatus;
import com.homehealthcare.mobile.foundation.MobileSyncDisposition;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.patientcontact.domain.PatientContact;
import com.homehealthcare.patientcontact.domain.PatientContactRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisCondition;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisConditionRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class MobileExecutionService {

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final PatientAddressRepository patientAddressRepository;
    private final PatientContactRepository patientContactRepository;
    private final PatientDiagnosisConditionRepository patientDiagnosisConditionRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final MobileVisitExecutionSessionRepository mobileVisitExecutionSessionRepository;
    private final MobileVisitTaskChecklistEntryRepository mobileVisitTaskChecklistEntryRepository;
    private final MobileQuickNoteEntryRepository mobileQuickNoteEntryRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final MobileAuditService mobileAuditService;

    @Transactional(readOnly = true)
    public List<CaregiverTodayWorkItem> getTodayWork(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate day,
            @NotBlank String timezone) {
        requirePermission(actorMembership, AgencyPermission.VIEW_OWN_MOBILE_VISITS);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        List<CaregiverVisitAssignment> assignments = findAssignmentsForDay(caregiverProfile, day, timezone);
        List<CaregiverTodayWorkItem> items = new ArrayList<>();
        int sortOrder = 1;
        for (CaregiverVisitAssignment assignment : assignments) {
            VisitOccurrence visit = assignment.getVisitOccurrence();
            MobileVisitExecutionSession latestSession = mobileVisitExecutionSessionRepository
                    .findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByStartedAtDesc(visit.getId(), caregiverProfile.getId())
                    .orElse(null);
            items.add(new CaregiverTodayWorkItem(
                    visit.getId(),
                    patientDisplaySummary(visit.getPatient()),
                    visit.getBranch() == null ? null : visit.getBranch().getName(),
                    visit.getPlannedStartAt(),
                    visit.getPlannedEndAt(),
                    visit.getTimezone(),
                    visit.getStatus().name(),
                    sortOrder++,
                    latestSession == null ? MobileExecutionSessionStatus.NOT_STARTED : latestSession.getExecutionStatus()));
        }
        return items;
    }

    @Transactional
    public MobileVisitExecutionSession startVisitExecution(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @Valid StartVisitExecutionCommand command) {
        requirePermission(actorMembership, AgencyPermission.EXECUTE_OWN_VISITS);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        CaregiverVisitAssignment assignment = resolveOwnedActiveAssignment(actorMembership, caregiverProfile, visitOccurrenceId);
        mobileVisitExecutionSessionRepository
                .findFirstByVisitOccurrence_IdAndCaregiverProfile_IdAndExecutionStatusOrderByStartedAtDesc(
                        visitOccurrenceId,
                        caregiverProfile.getId(),
                        MobileExecutionSessionStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new MobileConflictException("An in-progress mobile execution session already exists for this visit.");
                });

        VisitOccurrence visit = assignment.getVisitOccurrence();
        MobileVisitExecutionSession session = MobileVisitExecutionSession.start(
                visit,
                caregiverProfile,
                visit.getPatient(),
                visit.getBranch(),
                command.startedAt(),
                command.startedLatitude(),
                command.startedLongitude(),
                command.startSource(),
                command.syncStatus());
        MobileVisitExecutionSession saved = mobileVisitExecutionSessionRepository.saveAndFlush(session);
        mobileAuditService.recordVisitExecutionStarted(actorMembership, saved.getId(), saved.getBranchId(), executionMetadata(saved));
        return saved;
    }

    @Transactional
    public MobileVisitExecutionSession endVisitExecution(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID executionSessionId,
            @Valid EndVisitExecutionCommand command) {
        requirePermission(actorMembership, AgencyPermission.EXECUTE_OWN_VISITS);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        MobileVisitExecutionSession session = mobileVisitExecutionSessionRepository.findByIdAndAgency_Id(executionSessionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MobileEntityNotFoundException("MobileVisitExecutionSession", executionSessionId));
        ensureActorOwnsSession(caregiverProfile, session);
        session.complete(
                command.endedAt(),
                command.endedLatitude(),
                command.endedLongitude(),
                command.endSource(),
                command.syncStatus());
        MobileVisitExecutionSession saved = mobileVisitExecutionSessionRepository.saveAndFlush(session);
        mobileAuditService.recordVisitExecutionEnded(actorMembership, saved.getId(), saved.getBranchId(), executionMetadata(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public CaregiverRouteProjection getRouteProjection(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate day,
            @NotBlank String timezone) {
        requirePermission(actorMembership, AgencyPermission.VIEW_OWN_MOBILE_VISITS);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        List<CaregiverVisitAssignment> assignments = findAssignmentsForDay(caregiverProfile, day, timezone);
        List<RouteStop> stops = new ArrayList<>();
        int sortOrder = 1;
        for (CaregiverVisitAssignment assignment : assignments) {
            VisitOccurrence visit = assignment.getVisitOccurrence();
            Patient patient = visit.getPatient();
            PatientAddress address = patientAddressRepository.findByPatient_Id(patient.getId()).orElse(null);
            MobileVisitExecutionSession latestSession = mobileVisitExecutionSessionRepository
                    .findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByStartedAtDesc(visit.getId(), caregiverProfile.getId())
                    .orElse(null);
            stops.add(new RouteStop(
                    visit.getId(),
                    patientDisplaySummary(patient),
                    addressSummary(address),
                    visit.getPlannedStartAt(),
                    visit.getPlannedEndAt(),
                    sortOrder++,
                    latestSession == null ? MobileExecutionSessionStatus.NOT_STARTED : latestSession.getExecutionStatus()));
        }
        return new CaregiverRouteProjection(caregiverProfile.getId(), day, timezone.trim(), stops);
    }

    @Transactional(readOnly = true)
    public MobilePatientSummary getPatientSummary(@NotNull AgencyMembership actorMembership, @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_OWN_MOBILE_VISITS);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        VisitOccurrence visit = resolveOwnedVisit(actorMembership, caregiverProfile, visitOccurrenceId);
        Patient patient = visit.getPatient();
        PatientAddress address = patientAddressRepository.findByPatient_Id(patient.getId()).orElse(null);
        PatientContact contact = patientContactRepository.findAllByPatient_IdOrderByPrimaryContactDescEmergencyContactDescFullNameAsc(patient.getId())
                .stream()
                .filter(candidate -> candidate.isPrimaryContact() || candidate.isEmergencyContact())
                .findFirst()
                .orElse(null);
        List<String> diagnoses = patientDiagnosisConditionRepository.findAllByPatient_IdOrderByPrimaryConditionDescDescriptionAsc(patient.getId()).stream()
                .filter(condition -> condition.getStatus() != PatientDiagnosisStatus.INACTIVE)
                .sorted(Comparator.comparing(PatientDiagnosisCondition::isPrimaryCondition).reversed())
                .map(PatientDiagnosisCondition::getDescription)
                .limit(3)
                .toList();
        return new MobilePatientSummary(
                patient.getId(),
                patientDisplaySummary(patient),
                patient.getDateOfBirth(),
                addressSummary(address),
                contact == null ? null : new ContactSummary(contact.getFullName(), contact.getRelationshipType(), contact.getPhone(), contact.getEmail()),
                diagnoses,
                visit.getServiceLine() == null ? null : visit.getServiceLine().getName(),
                visit.getVisitType() == null ? null : visit.getVisitType().getName(),
                null);
    }

    @Transactional(readOnly = true)
    public MobileCareInstructionSummary getCareInstructionSummary(@NotNull AgencyMembership actorMembership, @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_OWN_MOBILE_VISITS);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        VisitOccurrence visit = resolveOwnedVisit(actorMembership, caregiverProfile, visitOccurrenceId);
        String visitTypeInstructions = visit.getVisitType() == null ? null : visit.getVisitType().getDescription();
        String serviceLineInstructions = visit.getServiceLine() == null ? null : visit.getServiceLine().getDescription();
        String branchInstructions = visit.getBranch() == null ? null : "Follow branch workflow for " + visit.getBranch().getName();
        String patientSpecificCareNotes = normalizeOptional(visit.getNotes());
        return new MobileCareInstructionSummary(visit.getId(), visitTypeInstructions, serviceLineInstructions, branchInstructions, patientSpecificCareNotes);
    }

    @Transactional
    public List<MobileVisitTaskChecklistEntry> saveTaskChecklist(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID executionSessionId,
            @NotEmpty List<@Valid SaveTaskChecklistItemCommand> items) {
        requirePermission(actorMembership, AgencyPermission.SUBMIT_MOBILE_VISIT_DOCUMENTATION);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        MobileVisitExecutionSession session = resolveOwnedSession(actorMembership, caregiverProfile, executionSessionId);

        List<MobileVisitTaskChecklistEntry> existingEntries = mobileVisitTaskChecklistEntryRepository
                .findAllByExecutionSession_IdOrderBySortOrderAscCreatedAtAsc(session.getId());
        List<MobileVisitTaskChecklistEntry> savedEntries = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            SaveTaskChecklistItemCommand item = items.get(index);
            TaskTemplate taskTemplate = resolveTaskTemplate(session.getAgencyId(), item.taskTemplateId());
            OffsetDateTime completedAt = item.completed() ? coalesce(item.completedAt(), OffsetDateTime.now()) : null;
            MobileVisitTaskChecklistEntry entry = index < existingEntries.size()
                    ? existingEntries.get(index)
                    : MobileVisitTaskChecklistEntry.create(
                            session,
                            taskTemplate,
                            item.title(),
                            item.description(),
                            item.category(),
                            item.sortOrder(),
                            item.completed(),
                            completedAt,
                            item.completionNotes());
            if (index < existingEntries.size()) {
                entry.update(
                        taskTemplate,
                        item.title(),
                        item.description(),
                        item.category(),
                        item.sortOrder(),
                        item.completed(),
                        completedAt,
                        item.completionNotes());
            }
            savedEntries.add(mobileVisitTaskChecklistEntryRepository.save(entry));
        }
        for (int index = items.size(); index < existingEntries.size(); index++) {
            mobileVisitTaskChecklistEntryRepository.delete(existingEntries.get(index));
        }
        mobileVisitTaskChecklistEntryRepository.flush();
        for (MobileVisitTaskChecklistEntry entry : savedEntries) {
            mobileAuditService.recordTaskChecklistSaved(actorMembership, entry.getId(), session.getBranchId(), taskMetadata(entry));
        }
        return savedEntries;
    }

    @Transactional
    public MobileQuickNoteEntry saveQuickNote(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID executionSessionId,
            @Valid SaveQuickNoteCommand command) {
        requirePermission(actorMembership, AgencyPermission.SUBMIT_MOBILE_VISIT_DOCUMENTATION);
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        MobileVisitExecutionSession session = resolveOwnedSession(actorMembership, caregiverProfile, executionSessionId);
        MobileQuickNoteEntry saved = mobileQuickNoteEntryRepository.saveAndFlush(MobileQuickNoteEntry.create(
                session,
                caregiverProfile,
                coalesce(command.authoredAt(), OffsetDateTime.now()),
                command.noteText(),
                command.status()));
        mobileAuditService.recordQuickNoteSaved(actorMembership, saved.getId(), session.getBranchId(), quickNoteMetadata(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    MobileVisitExecutionSession resolveOwnedSessionContext(@NotNull AgencyMembership actorMembership, @NotNull UUID executionSessionId) {
        CaregiverProfile caregiverProfile = resolveActorCaregiverProfile(actorMembership);
        return resolveOwnedSession(actorMembership, caregiverProfile, executionSessionId);
    }

    @Transactional(readOnly = true)
    public CaregiverProfile resolveActorCaregiverProfile(AgencyMembership actorMembership) {
        return caregiverProfileRepository.findFirstByAgency_IdAndAgencyMembership_Id(actorMembership.getAgencyId(), actorMembership.getId())
                .orElseThrow(() -> new UnauthorizedMobileActorException(actorMembership.getId()));
    }

    private List<CaregiverVisitAssignment> findAssignmentsForDay(CaregiverProfile caregiverProfile, LocalDate day, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone.trim());
        OffsetDateTime windowStart = day.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime windowEnd = day.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        return caregiverVisitAssignmentRepository.findAllActiveForCaregiverWithin(
                caregiverProfile.getId(),
                windowStart,
                windowEnd,
                CaregiverAssignmentStatus.ACTIVE);
    }

    private CaregiverVisitAssignment resolveOwnedActiveAssignment(
            AgencyMembership actorMembership,
            CaregiverProfile caregiverProfile,
            UUID visitOccurrenceId) {
        CaregiverVisitAssignment assignment = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visitOccurrenceId, CaregiverAssignmentStatus.ACTIVE)
                .orElseThrow(() -> new MobileEntityNotFoundException("CaregiverVisitAssignment", visitOccurrenceId));
        if (!Objects.equals(assignment.getAgencyId(), actorMembership.getAgencyId())
                || !Objects.equals(assignment.getCaregiverProfileId(), caregiverProfile.getId())) {
            throw new UnauthorizedMobileActorException(actorMembership.getId());
        }
        return assignment;
    }

    private VisitOccurrence resolveOwnedVisit(AgencyMembership actorMembership, CaregiverProfile caregiverProfile, UUID visitOccurrenceId) {
        CaregiverVisitAssignment assignment = resolveOwnedActiveAssignment(actorMembership, caregiverProfile, visitOccurrenceId);
        return visitOccurrenceRepository.findByIdAndAgency_Id(assignment.getVisitOccurrenceId(), actorMembership.getAgencyId())
                .orElseThrow(() -> new MobileEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
    }

    private MobileVisitExecutionSession resolveOwnedSession(
            AgencyMembership actorMembership,
            CaregiverProfile caregiverProfile,
            UUID executionSessionId) {
        MobileVisitExecutionSession session = mobileVisitExecutionSessionRepository.findByIdAndAgency_Id(executionSessionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MobileEntityNotFoundException("MobileVisitExecutionSession", executionSessionId));
        ensureActorOwnsSession(caregiverProfile, session);
        return session;
    }

    private void ensureActorOwnsSession(CaregiverProfile caregiverProfile, MobileVisitExecutionSession session) {
        if (!Objects.equals(session.getCaregiverProfileId(), caregiverProfile.getId())) {
            throw new UnauthorizedMobileActorException(caregiverProfile.getAgencyMembershipId());
        }
    }

    private TaskTemplate resolveTaskTemplate(UUID agencyId, UUID taskTemplateId) {
        if (taskTemplateId == null) {
            return null;
        }
        return taskTemplateRepository.findById(taskTemplateId)
                .filter(template -> Objects.equals(template.getAgencyId(), agencyId))
                .orElseThrow(() -> new MobileEntityNotFoundException("TaskTemplate", taskTemplateId));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedMobileActorException::new);
    }

    private static String patientDisplaySummary(Patient patient) {
        String middleName = normalizeOptional(patient.getMiddleName());
        return Stream.of(patient.getFirstName(), middleName, patient.getLastName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(patient.getExternalReference());
    }

    private static String addressSummary(PatientAddress address) {
        if (address == null) {
            return null;
        }
        return Stream.of(address.getAddressLine1(), address.getCity(), address.getState(), address.getPostalCode())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private static String executionMetadata(MobileVisitExecutionSession session) {
        return "{\"visitOccurrenceId\":\"" + session.getVisitOccurrenceId() + "\",\"executionStatus\":\"" + session.getExecutionStatus().name() + "\"}";
    }

    private static String taskMetadata(MobileVisitTaskChecklistEntry entry) {
        return "{\"executionSessionId\":\"" + entry.getExecutionSessionId() + "\",\"completed\":" + entry.isCompleted() + "}";
    }

    private static String quickNoteMetadata(MobileQuickNoteEntry note) {
        return "{\"executionSessionId\":\"" + note.getExecutionSession().getId() + "\",\"status\":\"" + note.getStatus().name() + "\"}";
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static <T> T coalesce(T value, T fallback) {
        return value == null ? fallback : value;
    }

    public record CaregiverTodayWorkItem(
            UUID visitId,
            String patientDisplaySummary,
            String branchName,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            String scheduleStatus,
            Integer routeOrder,
            MobileExecutionSessionStatus executionStatus) {
    }

    public record CaregiverRouteProjection(
            UUID caregiverProfileId,
            LocalDate day,
            String timezone,
            List<RouteStop> stops) {
    }

    public record RouteStop(
            UUID visitId,
            String patientDisplaySummary,
            String addressSummary,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            int sortOrder,
            MobileExecutionSessionStatus executionStatus) {
    }

    public record ContactSummary(
            String fullName,
            String relationshipType,
            String phone,
            String email) {
    }

    public record MobilePatientSummary(
            UUID patientId,
            String patientDisplaySummary,
            LocalDate dateOfBirth,
            String addressSummary,
            ContactSummary contactSummary,
            List<String> diagnosisSummaries,
            String serviceLineSummary,
            String visitTypeSummary,
            String payerSnippet) {
    }

    public record MobileCareInstructionSummary(
            UUID visitId,
            String visitTypeInstructions,
            String serviceLineInstructions,
            String branchInstructions,
            String patientSpecificCareNotes) {
    }

    public record StartVisitExecutionCommand(
            @NotNull OffsetDateTime startedAt,
            BigDecimal startedLatitude,
            BigDecimal startedLongitude,
            String startSource,
            MobileSyncDisposition syncStatus) {
    }

    public record EndVisitExecutionCommand(
            @NotNull OffsetDateTime endedAt,
            BigDecimal endedLatitude,
            BigDecimal endedLongitude,
            String endSource,
            MobileSyncDisposition syncStatus) {
    }

    public record SaveTaskChecklistItemCommand(
            UUID taskTemplateId,
            @NotBlank String title,
            String description,
            TaskTemplateCategory category,
            int sortOrder,
            boolean completed,
            OffsetDateTime completedAt,
            String completionNotes) {
    }

    public record SaveQuickNoteCommand(
            @NotNull MobileQuickNoteStatus status,
            @NotBlank String noteText,
            OffsetDateTime authoredAt) {
    }
}
