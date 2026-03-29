package com.homehealthcare.evv.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.evv.application.EvvVerificationService;
import com.homehealthcare.evv.application.EvvVerificationService.CreateEscalationCommand;
import com.homehealthcare.evv.application.EvvVerificationService.LogVisitExceptionCommand;
import com.homehealthcare.evv.application.EvvVerificationService.NotifySupervisorCommand;
import com.homehealthcare.evv.application.EvvVerificationService.RecordClockEventCommand;
import com.homehealthcare.evv.application.EvvVerificationService.RecordSignatureStatusCommand;
import com.homehealthcare.evv.application.EvvVerificationService.ReportMissedVisitCommand;
import com.homehealthcare.evv.application.EvvVerificationService.UpdateExceptionStatusCommand;
import com.homehealthcare.evv.application.UnauthorizedEvvActorException;
import com.homehealthcare.evv.domain.EscalationRequest;
import com.homehealthcare.evv.domain.EvvClockEvent;
import com.homehealthcare.evv.domain.EvvClockEventType;
import com.homehealthcare.evv.domain.EvvGeofenceEvaluation;
import com.homehealthcare.evv.domain.EvvGeofenceEvaluationRepository;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.MissedVisitRecord;
import com.homehealthcare.evv.domain.MissedVisitRecordRepository;
import com.homehealthcare.evv.domain.SignatureVerificationLink;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.evv.domain.SupervisorNotificationEvent;
import com.homehealthcare.evv.domain.VisitExceptionRecord;
import com.homehealthcare.evv.domain.VisitExceptionRecordRepository;
import com.homehealthcare.evv.domain.VisitExceptionSeverity;
import com.homehealthcare.evv.domain.VisitExceptionStatus;
import com.homehealthcare.evv.domain.VisitExceptionType;
import com.homehealthcare.evv.foundation.EvvComplianceOutcome;
import com.homehealthcare.evv.foundation.EvvComplianceProjection;
import com.homehealthcare.evv.foundation.EvvVerificationStatus;
import com.homehealthcare.evv.foundation.GeofenceEvaluationOutcome;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evv")
class EvvController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final EvvGeofenceEvaluationRepository evvGeofenceEvaluationRepository;
    private final VisitExceptionRecordRepository visitExceptionRecordRepository;
    private final MissedVisitRecordRepository missedVisitRecordRepository;
    private final EvvVerificationService evvVerificationService;

    EvvController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            CaregiverProfileRepository caregiverProfileRepository,
            CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository,
            VisitOccurrenceRepository visitOccurrenceRepository,
            EvvVerificationSessionRepository evvVerificationSessionRepository,
            EvvGeofenceEvaluationRepository evvGeofenceEvaluationRepository,
            VisitExceptionRecordRepository visitExceptionRecordRepository,
            MissedVisitRecordRepository missedVisitRecordRepository,
            EvvVerificationService evvVerificationService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.caregiverProfileRepository = caregiverProfileRepository;
        this.caregiverVisitAssignmentRepository = caregiverVisitAssignmentRepository;
        this.visitOccurrenceRepository = visitOccurrenceRepository;
        this.evvVerificationSessionRepository = evvVerificationSessionRepository;
        this.evvGeofenceEvaluationRepository = evvGeofenceEvaluationRepository;
        this.visitExceptionRecordRepository = visitExceptionRecordRepository;
        this.missedVisitRecordRepository = missedVisitRecordRepository;
        this.evvVerificationService = evvVerificationService;
    }

    @PostMapping("/visits/{visitId}/clock-in")
    ClockEventResponse clockIn(@PathVariable UUID visitId, @Valid @RequestBody ClockEventRequest request) {
        return recordClockEvent(visitId, EvvClockEventType.CLOCK_IN, request);
    }

    @PostMapping("/visits/{visitId}/clock-out")
    ClockEventResponse clockOut(@PathVariable UUID visitId, @Valid @RequestBody ClockEventRequest request) {
        return recordClockEvent(visitId, EvvClockEventType.CLOCK_OUT, request);
    }

    @PostMapping("/verification-sessions/{verificationSessionId}/signatures")
    SignatureResponse recordSignature(
            @PathVariable UUID verificationSessionId,
            @Valid @RequestBody RecordSignatureRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        SignatureVerificationLink link = evvVerificationService.recordSignatureStatus(
                actorMembership,
                verificationSessionId,
                new RecordSignatureStatusCommand(
                        request.artifactId(),
                        request.signerRole(),
                        request.verificationStatus(),
                        request.recordedAt()));
        return new SignatureResponse(
                link.getId(),
                link.getVerificationSession().getId(),
                link.getArtifact() == null ? null : link.getArtifact().getId(),
                link.getSignerRole(),
                link.getVerificationStatus(),
                link.getRecordedAt());
    }

    @PostMapping("/visits/{visitId}/missed-visits")
    MissedVisitResponse reportMissedVisit(@PathVariable UUID visitId, @Valid @RequestBody ReportMissedVisitRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        MissedVisitRecord record = evvVerificationService.reportMissedVisit(
                actorMembership,
                visitId,
                new ReportMissedVisitCommand(request.reasonCode(), request.narrative(), request.reportedAt()));
        return toMissedVisitResponse(record);
    }

    @PostMapping("/verification-sessions/{verificationSessionId}/exceptions")
    ExceptionResponse createException(
            @PathVariable UUID verificationSessionId,
            @Valid @RequestBody CreateExceptionRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        VisitExceptionRecord record = evvVerificationService.logVisitException(
                actorMembership,
                verificationSessionId,
                new LogVisitExceptionCommand(
                        request.exceptionType(),
                        request.severity(),
                        request.reasonCode(),
                        request.narrative()));
        return toExceptionResponse(record);
    }

    @GetMapping("/exceptions")
    List<ExceptionResponse> listExceptions(
            @RequestParam(name = "status", required = false) VisitExceptionStatus status,
            @RequestParam(name = "visitId", required = false) UUID visitId) {
        AgencyMembership actorMembership = requireOperationalEvvAccess();
        return visitExceptionRecordRepository.findAllByAgency_IdOrderByCreatedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(record -> status == null || record.getStatus() == status)
                .filter(record -> visitId == null || visitId.equals(record.getVisitOccurrence().getId()))
                .map(EvvController::toExceptionResponse)
                .toList();
    }

    @GetMapping("/exceptions/{exceptionId}")
    ExceptionResponse getException(@PathVariable UUID exceptionId) {
        return toExceptionResponse(evvVerificationService.getVisitException(
                configurationActorResolver.requireActorMembership(),
                exceptionId));
    }

    @PutMapping("/exceptions/{exceptionId}/status")
    ExceptionResponse updateExceptionStatus(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody UpdateExceptionStatusRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        VisitExceptionRecord record = evvVerificationService.updateExceptionStatus(
                actorMembership,
                exceptionId,
                new UpdateExceptionStatusCommand(request.status(), request.actedAt()));
        return toExceptionResponse(record);
    }

    @GetMapping("/my/visits/{visitId}/summary")
    EvvSummaryResponse ownVisitSummary(@PathVariable UUID visitId) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        requirePermission(actorMembership, AgencyPermission.VIEW_OWN_EVV);
        UUID caregiverProfileId = caregiverProfileRepository.findFirstByAgency_IdAndAgencyMembership_Id(actorMembership.getAgencyId(), actorMembership.getId())
                .orElseThrow(() -> new UnauthorizedEvvActorException(actorMembership.getId()))
                .getId();
        if (caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visitId, CaregiverAssignmentStatus.ACTIVE)
                .filter(assignment -> caregiverProfileId.equals(assignment.getCaregiverProfileId()))
                .isEmpty()) {
            throw new UnauthorizedEvvActorException(actorMembership.getId());
        }
        EvvVerificationSession session = evvVerificationSessionRepository
                .findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByOpenedAtDesc(visitId, caregiverProfileId)
                .orElse(null);
        return buildSummary(visitId, session);
    }

    @GetMapping("/visits/{visitId}/summary")
    EvvSummaryResponse visitSummary(@PathVariable UUID visitId) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        if (agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.VIEW_OWN_EVV)
                && !hasOperationalEvvAccess(actorMembership)) {
            return ownVisitSummary(visitId);
        }
        requireOperationalEvvAccess();
        return buildSummary(visitId, evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(visitId).orElse(null));
    }

    @GetMapping("/readiness")
    List<EvvSummaryResponse> readiness(
            @RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "verificationStatus", required = false) EvvVerificationStatus verificationStatus,
            @RequestParam(name = "complianceOutcome", required = false) EvvComplianceOutcome complianceOutcome) {
        AgencyMembership actorMembership = requireOperationalEvvAccess();
        return visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                .filter(visit -> visit.getPlannedStartAt().toLocalDate().equals(day))
                .filter(visit -> branchId == null || branchId.equals(visit.getBranchId()))
                .map(visit -> buildSummary(visit.getId(), evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(visit.getId()).orElse(null)))
                .filter(summary -> verificationStatus == null || summary.verificationStatus() == verificationStatus)
                .filter(summary -> complianceOutcome == null || summary.complianceOutcome() == complianceOutcome)
                .toList();
    }

    @PostMapping("/missed-visits/{missedVisitId}/notify-supervisor")
    NotificationResponse notifySupervisorForMissedVisit(
            @PathVariable UUID missedVisitId,
            @Valid @RequestBody NotifySupervisorRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        SupervisorNotificationEvent event = evvVerificationService.notifySupervisorForMissedVisit(
                actorMembership,
                missedVisitId,
                new NotifySupervisorCommand(
                        request.recipientMembershipId(),
                        request.channel(),
                        request.rationale(),
                        request.createdAt()));
        return toNotificationResponse(event);
    }

    @PostMapping("/exceptions/{exceptionId}/notify-supervisor")
    NotificationResponse notifySupervisorForException(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody NotifySupervisorRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        SupervisorNotificationEvent event = evvVerificationService.notifySupervisorForException(
                actorMembership,
                exceptionId,
                new NotifySupervisorCommand(
                        request.recipientMembershipId(),
                        request.channel(),
                        request.rationale(),
                        request.createdAt()));
        return toNotificationResponse(event);
    }

    @PostMapping("/missed-visits/{missedVisitId}/escalations")
    EscalationResponse createMissedVisitEscalation(
            @PathVariable UUID missedVisitId,
            @Valid @RequestBody CreateEscalationRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        EscalationRequest escalationRequest = evvVerificationService.createEscalationForMissedVisit(
                actorMembership,
                missedVisitId,
                new CreateEscalationCommand(
                        request.targetRoleKey(),
                        request.severity(),
                        request.rationale(),
                        request.slaDueAt()));
        return toEscalationResponse(escalationRequest);
    }

    @PostMapping("/exceptions/{exceptionId}/escalations")
    EscalationResponse createExceptionEscalation(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody CreateEscalationRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        EscalationRequest escalationRequest = evvVerificationService.createEscalationForException(
                actorMembership,
                exceptionId,
                new CreateEscalationCommand(
                        request.targetRoleKey(),
                        request.severity(),
                        request.rationale(),
                        request.slaDueAt()));
        return toEscalationResponse(escalationRequest);
    }

    private ClockEventResponse recordClockEvent(UUID visitId, EvvClockEventType eventType, ClockEventRequest request) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        EvvVerificationSession verificationSession = evvVerificationService.openVerificationSession(
                actorMembership,
                visitId,
                request.executionSessionId(),
                request.capturedAt());
        EvvClockEvent event = evvVerificationService.recordClockEvent(
                actorMembership,
                verificationSession.getId(),
                new RecordClockEventCommand(
                        eventType,
                        request.capturedAt(),
                        request.capturedLatitude(),
                        request.capturedLongitude(),
                        request.timezone(),
                        request.captureSource(),
                        request.platformSummary(),
                        request.appVersion(),
                        request.deviceClass(),
                        request.timezoneOffsetMinutes(),
                        request.userAgentHash(),
                        request.sessionFingerprintHash()));
        EvvGeofenceEvaluation evaluation = evvGeofenceEvaluationRepository.findFirstByClockEvent_Id(event.getId()).orElse(null);
        EvvComplianceProjection projection = evvVerificationService.getComplianceProjection(actorMembership, verificationSession.getId());
        return new ClockEventResponse(
                event.getId(),
                verificationSession.getId(),
                event.getEventType(),
                event.getCapturedAt(),
                event.getVerificationStatus(),
                evaluation == null ? null : evaluation.getOutcome(),
                evaluation == null ? null : evaluation.getReasonCode(),
                evaluation == null ? null : evaluation.getDistanceFromExpectedMeters(),
                evaluation == null ? null : evaluation.getToleranceMetersUsed(),
                projection.overallOutcome(),
                describeWarnings(projection),
                describeBlockers(projection));
    }

    private EvvSummaryResponse buildSummary(UUID visitId, EvvVerificationSession session) {
        VisitOccurrence visit = visitOccurrenceRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("VisitOccurrence was not found: " + visitId));
        EvvComplianceProjection projection = session == null
                ? EvvComplianceProjection.blocked(false, false, GeofenceEvaluationOutcome.NOT_EVALUABLE, false, 0)
                : evvVerificationService.getComplianceProjection(configurationActorResolver.requireActorMembership(), session.getId());
        EvvVerificationStatus status = session == null ? EvvVerificationStatus.PENDING_VERIFICATION : session.getVerificationStatus();
        EvvComplianceOutcome outcome = session == null ? EvvComplianceOutcome.BLOCKED : session.getComplianceOutcome();
        return new EvvSummaryResponse(
                visitId,
                session == null ? null : session.getId(),
                visit.getPatient().getId(),
                visit.getBranchId(),
                session == null ? null : session.getCaregiverProfile().getId(),
                status,
                outcome,
                projection.startEventPresent(),
                projection.endEventPresent(),
                projection.geofenceOutcome(),
                projection.signatureComplete(),
                projection.openExceptionCount(),
                projection.missedVisitReported(),
                describeWarnings(projection),
                describeBlockers(projection));
    }

    private AgencyMembership requireOperationalEvvAccess() {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        if (!hasOperationalEvvAccess(actorMembership)) {
            throw new UnauthorizedEvvActorException(actorMembership.getId());
        }
        return actorMembership;
    }

    private boolean hasOperationalEvvAccess(AgencyMembership actorMembership) {
        return agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.MANAGE_EVV_EXCEPTIONS)
                || agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.VIEW_MISSED_VISITS)
                || agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.RECEIVE_EVV_NOTIFICATIONS)
                || agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.RESOLVE_MISSED_VISITS);
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedEvvActorException::new);
    }

    private static List<String> describeWarnings(EvvComplianceProjection projection) {
        List<String> warnings = new ArrayList<>();
        if (projection.geofenceOutcome() == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_WARNING) {
            warnings.add("Geofence warning");
        }
        if (projection.geofenceOutcome() == GeofenceEvaluationOutcome.NOT_EVALUABLE) {
            warnings.add("Geofence could not be evaluated");
        }
        return warnings;
    }

    private static List<String> describeBlockers(EvvComplianceProjection projection) {
        List<String> blockers = new ArrayList<>();
        if (!projection.startEventPresent()) {
            blockers.add("Missing clock-in");
        }
        if (!projection.endEventPresent() && !projection.missedVisitReported()) {
            blockers.add("Missing clock-out");
        }
        if (!projection.signatureComplete() && !projection.missedVisitReported()) {
            blockers.add("Missing required signature");
        }
        if (projection.geofenceOutcome() == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_BLOCKED) {
            blockers.add("Geofence blocked");
        }
        if (projection.openExceptionCount() > 0) {
            blockers.add("Open EVV exceptions");
        }
        if (projection.missedVisitReported()) {
            blockers.add("Missed visit reported");
        }
        return blockers;
    }

    private static ExceptionResponse toExceptionResponse(VisitExceptionRecord record) {
        return new ExceptionResponse(
                record.getId(),
                record.getVerificationSession() == null ? null : record.getVerificationSession().getId(),
                record.getVisitOccurrence().getId(),
                record.getCaregiverProfile() == null ? null : record.getCaregiverProfile().getId(),
                record.getPatient().getId(),
                record.getBranch() == null ? null : record.getBranch().getId(),
                record.getExceptionType(),
                record.getSeverity(),
                record.getReasonCode(),
                record.getNarrative(),
                record.getStatus(),
                record.getAcknowledgedAt(),
                record.getResolvedAt());
    }

    private static MissedVisitResponse toMissedVisitResponse(MissedVisitRecord record) {
        return new MissedVisitResponse(
                record.getId(),
                record.getVisitOccurrence().getId(),
                record.getCaregiverProfile() == null ? null : record.getCaregiverProfile().getId(),
                record.getPatient().getId(),
                record.getBranch() == null ? null : record.getBranch().getId(),
                record.getReasonCode(),
                record.getNarrative(),
                record.getReportedAt(),
                record.getStatus());
    }

    private static NotificationResponse toNotificationResponse(SupervisorNotificationEvent event) {
        return new NotificationResponse(
                event.getId(),
                event.getMissedVisitRecord() == null ? null : event.getMissedVisitRecord().getId(),
                event.getVisitExceptionRecord() == null ? null : event.getVisitExceptionRecord().getId(),
                event.getRecipientMembership().getId(),
                event.getChannel(),
                event.getRationale(),
                event.getCreatedAtEvent(),
                event.getStatus());
    }

    private static EscalationResponse toEscalationResponse(EscalationRequest request) {
        return new EscalationResponse(
                request.getId(),
                request.getMissedVisitRecord() == null ? null : request.getMissedVisitRecord().getId(),
                request.getVisitExceptionRecord() == null ? null : request.getVisitExceptionRecord().getId(),
                request.getTargetRoleKey(),
                request.getSeverity(),
                request.getRationale(),
                request.getSlaDueAt(),
                request.getStatus());
    }

    record ClockEventRequest(
            UUID executionSessionId,
            @NotNull OffsetDateTime capturedAt,
            BigDecimal capturedLatitude,
            BigDecimal capturedLongitude,
            @NotBlank String timezone,
            @NotBlank String captureSource,
            String platformSummary,
            String appVersion,
            String deviceClass,
            Integer timezoneOffsetMinutes,
            String userAgentHash,
            String sessionFingerprintHash) {
    }

    record ClockEventResponse(
            UUID eventId,
            UUID verificationSessionId,
            EvvClockEventType eventType,
            OffsetDateTime capturedAt,
            EvvVerificationStatus verificationStatus,
            GeofenceEvaluationOutcome geofenceOutcome,
            String geofenceReasonCode,
            Integer distanceFromExpectedMeters,
            Integer toleranceMetersUsed,
            EvvComplianceOutcome overallOutcome,
            List<String> warnings,
            List<String> blockers) {
    }

    record RecordSignatureRequest(
            UUID artifactId,
            @NotNull SignatureSignerRole signerRole,
            @NotNull SignatureVerificationStatus verificationStatus,
            @NotNull OffsetDateTime recordedAt) {
    }

    record SignatureResponse(
            UUID id,
            UUID verificationSessionId,
            UUID artifactId,
            SignatureSignerRole signerRole,
            SignatureVerificationStatus verificationStatus,
            OffsetDateTime recordedAt) {
    }

    record ReportMissedVisitRequest(
            @NotBlank String reasonCode,
            @NotBlank String narrative,
            @NotNull OffsetDateTime reportedAt) {
    }

    record MissedVisitResponse(
            UUID id,
            UUID visitId,
            UUID caregiverProfileId,
            UUID patientId,
            UUID branchId,
            String reasonCode,
            String narrative,
            OffsetDateTime reportedAt,
            com.homehealthcare.evv.domain.MissedVisitStatus status) {
    }

    record CreateExceptionRequest(
            @NotNull VisitExceptionType exceptionType,
            @NotNull VisitExceptionSeverity severity,
            @NotBlank String reasonCode,
            @NotBlank String narrative) {
    }

    record ExceptionResponse(
            UUID id,
            UUID verificationSessionId,
            UUID visitId,
            UUID caregiverProfileId,
            UUID patientId,
            UUID branchId,
            VisitExceptionType exceptionType,
            VisitExceptionSeverity severity,
            String reasonCode,
            String narrative,
            VisitExceptionStatus status,
            OffsetDateTime acknowledgedAt,
            OffsetDateTime resolvedAt) {
    }

    record UpdateExceptionStatusRequest(
            @NotNull VisitExceptionStatus status,
            @NotNull OffsetDateTime actedAt) {
    }

    record EvvSummaryResponse(
            UUID visitId,
            UUID verificationSessionId,
            UUID patientId,
            UUID branchId,
            UUID caregiverProfileId,
            EvvVerificationStatus verificationStatus,
            EvvComplianceOutcome complianceOutcome,
            boolean startEventPresent,
            boolean endEventPresent,
            GeofenceEvaluationOutcome geofenceOutcome,
            boolean signatureComplete,
            int openExceptionCount,
            boolean missedVisitReported,
            List<String> warnings,
            List<String> blockers) {
    }

    record NotifySupervisorRequest(
            @NotNull UUID recipientMembershipId,
            @NotBlank String channel,
            @NotBlank String rationale,
            @NotNull OffsetDateTime createdAt) {
    }

    record NotificationResponse(
            UUID id,
            UUID missedVisitRecordId,
            UUID visitExceptionRecordId,
            UUID recipientMembershipId,
            String channel,
            String rationale,
            OffsetDateTime createdAt,
            com.homehealthcare.evv.domain.SupervisorNotificationStatus status) {
    }

    record CreateEscalationRequest(
            @NotBlank String targetRoleKey,
            @NotNull VisitExceptionSeverity severity,
            @NotBlank String rationale,
            OffsetDateTime slaDueAt) {
    }

    record EscalationResponse(
            UUID id,
            UUID missedVisitRecordId,
            UUID visitExceptionRecordId,
            String targetRoleKey,
            VisitExceptionSeverity severity,
            String rationale,
            OffsetDateTime slaDueAt,
            com.homehealthcare.evv.domain.EscalationStatus status) {
    }
}
