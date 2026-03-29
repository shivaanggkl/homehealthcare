package com.homehealthcare.evv.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.evv.domain.DeviceMetadataSnapshot;
import com.homehealthcare.evv.domain.DeviceMetadataSnapshotRepository;
import com.homehealthcare.evv.domain.EscalationRequest;
import com.homehealthcare.evv.domain.EscalationRequestRepository;
import com.homehealthcare.evv.domain.EvvClockEvent;
import com.homehealthcare.evv.domain.EvvClockEventRepository;
import com.homehealthcare.evv.domain.EvvClockEventType;
import com.homehealthcare.evv.domain.EvvGeofenceEvaluation;
import com.homehealthcare.evv.domain.EvvGeofenceEvaluationRepository;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.GeofenceToleranceRule;
import com.homehealthcare.evv.domain.GeofenceToleranceRuleRepository;
import com.homehealthcare.evv.domain.MissedVisitRecord;
import com.homehealthcare.evv.domain.MissedVisitRecordRepository;
import com.homehealthcare.evv.domain.MissedVisitStatus;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationLink;
import com.homehealthcare.evv.domain.SignatureVerificationLinkRepository;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.evv.domain.SupervisorNotificationEvent;
import com.homehealthcare.evv.domain.SupervisorNotificationEventRepository;
import com.homehealthcare.evv.domain.VisitExceptionRecord;
import com.homehealthcare.evv.domain.VisitExceptionRecordRepository;
import com.homehealthcare.evv.domain.VisitExceptionSeverity;
import com.homehealthcare.evv.domain.VisitExceptionStatus;
import com.homehealthcare.evv.domain.VisitExceptionType;
import com.homehealthcare.evv.foundation.Epic7EvvAuditAction;
import com.homehealthcare.evv.foundation.EvvAuditService;
import com.homehealthcare.evv.foundation.EvvComplianceOutcome;
import com.homehealthcare.evv.foundation.EvvComplianceProjection;
import com.homehealthcare.evv.foundation.EvvVerificationStatus;
import com.homehealthcare.evv.foundation.GeofenceEvaluationOutcome;
import com.homehealthcare.evv.foundation.GeofenceEvaluationResult;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSessionRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class EvvVerificationService {

    private static final int DEFAULT_TOLERANCE_METERS = 250;

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final MobileVisitExecutionSessionRepository mobileVisitExecutionSessionRepository;
    private final MobileFieldArtifactRepository mobileFieldArtifactRepository;
    private final PatientAddressRepository patientAddressRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final EvvClockEventRepository evvClockEventRepository;
    private final DeviceMetadataSnapshotRepository deviceMetadataSnapshotRepository;
    private final GeofenceToleranceRuleRepository geofenceToleranceRuleRepository;
    private final EvvGeofenceEvaluationRepository evvGeofenceEvaluationRepository;
    private final SignatureVerificationLinkRepository signatureVerificationLinkRepository;
    private final MissedVisitRecordRepository missedVisitRecordRepository;
    private final VisitExceptionRecordRepository visitExceptionRecordRepository;
    private final SupervisorNotificationEventRepository supervisorNotificationEventRepository;
    private final EscalationRequestRepository escalationRequestRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final EvvAuditService evvAuditService;

    @Transactional
    public EvvVerificationSession openVerificationSession(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            UUID executionSessionId,
            @NotNull OffsetDateTime openedAt) {
        ActorContext actorContext = resolveActorContext(actorMembership, visitOccurrenceId, AgencyPermission.VIEW_OWN_EVV);
        return evvVerificationSessionRepository
                .findFirstByVisitOccurrence_IdAndCaregiverProfile_IdOrderByOpenedAtDesc(visitOccurrenceId, actorContext.caregiverProfile().getId())
                .orElseGet(() -> evvVerificationSessionRepository.saveAndFlush(EvvVerificationSession.open(
                        actorContext.visitOccurrence(),
                        actorContext.caregiverProfile(),
                        actorContext.visitOccurrence().getPatient(),
                        actorContext.visitOccurrence().getBranch(),
                        resolveExecutionSession(actorMembership.getAgencyId(), executionSessionId, actorContext.visitOccurrence().getId()),
                        openedAt)));
    }

    @Transactional
    public EvvClockEvent recordClockEvent(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID verificationSessionId,
            @Valid RecordClockEventCommand command) {
        requirePermission(actorMembership, AgencyPermission.SUBMIT_OWN_EVV);
        EvvVerificationSession session = resolveOwnedVerificationSession(actorMembership, verificationSessionId);
        if (evvClockEventRepository.existsByVerificationSession_IdAndEventType(session.getId(), command.eventType())) {
            throw new EvvConflictException("A " + command.eventType() + " event already exists for this verification session.");
        }

        GeofenceEvaluationResult geofenceResult = evaluateGeofence(session, command.capturedLatitude(), command.capturedLongitude());
        EvvVerificationStatus eventStatus = switch (geofenceResult.outcome()) {
            case WITHIN_TOLERANCE -> EvvVerificationStatus.PENDING_VERIFICATION;
            case OUTSIDE_TOLERANCE_WARNING -> EvvVerificationStatus.VERIFIED_WITH_WARNING;
            case OUTSIDE_TOLERANCE_BLOCKED -> EvvVerificationStatus.EXCEPTION_OPEN;
            case NOT_EVALUABLE -> EvvVerificationStatus.PENDING_VERIFICATION;
        };

        EvvClockEvent event = evvClockEventRepository.saveAndFlush(EvvClockEvent.record(
                session,
                session.getCaregiverProfile(),
                session.getPatient(),
                session.getBranch(),
                command.eventType(),
                command.capturedAt(),
                command.capturedLatitude(),
                command.capturedLongitude(),
                command.timezone(),
                command.captureSource(),
                eventStatus));

        if (command.hasDeviceMetadata()) {
            deviceMetadataSnapshotRepository.saveAndFlush(DeviceMetadataSnapshot.capture(
                    event,
                    command.platformSummary(),
                    command.appVersion(),
                    command.deviceClass(),
                    command.timezoneOffsetMinutes(),
                    command.userAgentHash(),
                    command.sessionFingerprintHash()));
        }

        Optional<GeofenceToleranceRule> rule = resolveActiveRule(session);
        evvGeofenceEvaluationRepository.saveAndFlush(EvvGeofenceEvaluation.record(
                session,
                event,
                rule.orElse(null),
                geofenceResult.outcome(),
                geofenceResult.distanceFromExpectedMeters(),
                geofenceResult.toleranceMetersUsed(),
                geofenceResult.blocking(),
                geofenceResult.reasonCode()));

        if (command.eventType() == EvvClockEventType.CLOCK_IN) {
            evvAuditService.recordClockInRecorded(actorMembership, event.getId(), session.getBranchId(), clockMetadata(event));
        } else {
            evvAuditService.recordClockOutRecorded(actorMembership, event.getId(), session.getBranchId(), clockMetadata(event));
        }
        evvAuditService.recordGeofenceEvaluated(actorMembership, event.getId(), session.getBranchId(), geofenceMetadata(geofenceResult));
        refreshCompliance(session);
        return event;
    }

    @Transactional
    public SignatureVerificationLink recordSignatureStatus(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID verificationSessionId,
            @Valid RecordSignatureStatusCommand command) {
        requirePermission(actorMembership, AgencyPermission.SUBMIT_OWN_EVV);
        EvvVerificationSession session = resolveOwnedVerificationSession(actorMembership, verificationSessionId);
        MobileFieldArtifact artifact = command.artifactId() == null ? null : mobileFieldArtifactRepository
                .findByIdAndAgency_Id(command.artifactId(), actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("MobileFieldArtifact", command.artifactId()));
        SignatureVerificationLink link = signatureVerificationLinkRepository.saveAndFlush(SignatureVerificationLink.record(
                session,
                artifact,
                command.signerRole(),
                command.verificationStatus(),
                command.recordedAt()));
        evvAuditService.recordSignatureStatusRecorded(actorMembership, link.getId(), session.getBranchId(), "{\"status\":\"" + link.getVerificationStatus().name() + "\"}");
        refreshCompliance(session);
        return link;
    }

    @Transactional
    public VisitExceptionRecord logVisitException(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID verificationSessionId,
            @Valid LogVisitExceptionCommand command) {
        if (!agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.SUBMIT_OWN_EVV)
                && !agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.MANAGE_EVV_EXCEPTIONS)) {
            throw new UnauthorizedEvvActorException(actorMembership.getId());
        }
        EvvVerificationSession session = evvVerificationSessionRepository.findByIdAndAgency_Id(verificationSessionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("EvvVerificationSession", verificationSessionId));
        VisitExceptionRecord record = visitExceptionRecordRepository.saveAndFlush(VisitExceptionRecord.log(
                session,
                session.getVisitOccurrence(),
                session.getCaregiverProfile(),
                session.getPatient(),
                session.getBranch(),
                actorMembership,
                command.exceptionType(),
                command.severity(),
                command.reasonCode(),
                command.narrative()));
        evvAuditService.recordExceptionLogged(actorMembership, record.getId(), session.getBranchId(), "{\"type\":\"" + record.getExceptionType().name() + "\"}");
        refreshCompliance(session);
        return record;
    }

    @Transactional
    public MissedVisitRecord reportMissedVisit(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @Valid ReportMissedVisitCommand command) {
        requirePermission(actorMembership, AgencyPermission.SUBMIT_OWN_EVV);
        ActorContext actorContext = resolveActorContext(actorMembership, visitOccurrenceId, AgencyPermission.VIEW_OWN_EVV);
        if (missedVisitRecordRepository.existsByVisitOccurrence_IdAndStatusIn(visitOccurrenceId, Set.of(
                MissedVisitStatus.REPORTED,
                MissedVisitStatus.NOTIFIED,
                MissedVisitStatus.ESCALATED))) {
            throw new EvvConflictException("An unresolved missed visit already exists for this visit.");
        }
        MissedVisitRecord record = missedVisitRecordRepository.saveAndFlush(MissedVisitRecord.report(
                actorContext.visitOccurrence(),
                actorContext.caregiverProfile(),
                actorContext.visitOccurrence().getPatient(),
                actorContext.visitOccurrence().getBranch(),
                actorMembership,
                command.reasonCode(),
                command.narrative(),
                command.reportedAt()));
        EvvVerificationSession session = openVerificationSession(actorMembership, visitOccurrenceId, null, command.reportedAt());
        refreshCompliance(session);
        evvAuditService.recordMissedVisitReported(actorMembership, record.getId(), actorContext.visitOccurrence().getBranchId(), "{\"reasonCode\":\"" + record.getReasonCode() + "\"}");
        return record;
    }

    @Transactional
    public SupervisorNotificationEvent notifySupervisorForMissedVisit(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID missedVisitRecordId,
            @Valid NotifySupervisorCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECEIVE_EVV_NOTIFICATIONS);
        MissedVisitRecord missedVisitRecord = missedVisitRecordRepository.findByIdAndAgency_Id(missedVisitRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("MissedVisitRecord", missedVisitRecordId));
        AgencyMembership recipientMembership = resolveNotificationRecipient(actorMembership.getAgencyId(), command.recipientMembershipId());
        SupervisorNotificationEvent event = supervisorNotificationEventRepository.saveAndFlush(
                SupervisorNotificationEvent.createForMissedVisit(
                        missedVisitRecord,
                        recipientMembership,
                        actorMembership,
                        missedVisitRecord.getBranch(),
                        command.channel(),
                        command.rationale(),
                        command.createdAt()));
        missedVisitRecord.markNotified();
        evvAuditService.recordSupervisorNotified(actorMembership, event.getId(), missedVisitRecord.getBranch() == null ? null : missedVisitRecord.getBranch().getId(), "{\"channel\":\"" + command.channel().trim().toUpperCase() + "\"}");
        return event;
    }

    @Transactional
    public SupervisorNotificationEvent notifySupervisorForException(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitExceptionRecordId,
            @Valid NotifySupervisorCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECEIVE_EVV_NOTIFICATIONS);
        VisitExceptionRecord visitExceptionRecord = visitExceptionRecordRepository.findByIdAndAgency_Id(visitExceptionRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("VisitExceptionRecord", visitExceptionRecordId));
        AgencyMembership recipientMembership = resolveNotificationRecipient(actorMembership.getAgencyId(), command.recipientMembershipId());
        SupervisorNotificationEvent event = supervisorNotificationEventRepository.saveAndFlush(
                SupervisorNotificationEvent.createForException(
                        visitExceptionRecord,
                        recipientMembership,
                        actorMembership,
                        visitExceptionRecord.getBranch(),
                        command.channel(),
                        command.rationale(),
                        command.createdAt()));
        evvAuditService.recordSupervisorNotified(
                actorMembership,
                event.getId(),
                visitExceptionRecord.getBranch() == null ? null : visitExceptionRecord.getBranch().getId(),
                "{\"channel\":\"" + command.channel().trim().toUpperCase() + "\"}");
        return event;
    }

    @Transactional
    public EscalationRequest createEscalationForException(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitExceptionRecordId,
            @Valid CreateEscalationCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_EVV_EXCEPTIONS);
        VisitExceptionRecord record = visitExceptionRecordRepository.findByIdAndAgency_Id(visitExceptionRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("VisitExceptionRecord", visitExceptionRecordId));
        EscalationRequest escalationRequest = escalationRequestRepository.saveAndFlush(EscalationRequest.createForException(
                record,
                actorMembership,
                record.getBranch(),
                command.targetRoleKey(),
                command.severity(),
                command.rationale(),
                command.slaDueAt()));
        record.markEscalated();
        evvAuditService.recordEscalationCreated(actorMembership, escalationRequest.getId(), record.getBranch() == null ? null : record.getBranch().getId(), "{\"targetRoleKey\":\"" + command.targetRoleKey().trim().toUpperCase() + "\"}");
        refreshCompliance(record.getVerificationSession());
        return escalationRequest;
    }

    @Transactional(readOnly = true)
    public VisitExceptionRecord getVisitException(@NotNull AgencyMembership actorMembership, @NotNull UUID visitExceptionRecordId) {
        if (!agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.MANAGE_EVV_EXCEPTIONS)
                && !agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.VIEW_MISSED_VISITS)
                && !agencyAuthorizationGuard.hasPermission(actorMembership, AgencyPermission.RECEIVE_EVV_NOTIFICATIONS)) {
            throw new UnauthorizedEvvActorException(actorMembership.getId());
        }
        return visitExceptionRecordRepository.findByIdAndAgency_Id(visitExceptionRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("VisitExceptionRecord", visitExceptionRecordId));
    }

    @Transactional
    public VisitExceptionRecord updateExceptionStatus(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitExceptionRecordId,
            @Valid UpdateExceptionStatusCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_EVV_EXCEPTIONS);
        VisitExceptionRecord record = visitExceptionRecordRepository.findByIdAndAgency_Id(visitExceptionRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("VisitExceptionRecord", visitExceptionRecordId));
        switch (command.status()) {
            case ACKNOWLEDGED -> record.acknowledge(command.actedAt());
            case RESOLVED -> record.resolve(command.actedAt());
            case OPEN, ESCALATED -> throw new IllegalArgumentException("Only ACKNOWLEDGED and RESOLVED are supported through the exception status API");
        }
        visitExceptionRecordRepository.saveAndFlush(record);
        if (record.getVerificationSession() != null) {
            refreshCompliance(record.getVerificationSession());
        }
        return record;
    }

    @Transactional
    public EscalationRequest createEscalationForMissedVisit(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID missedVisitRecordId,
            @Valid CreateEscalationCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_EVV_EXCEPTIONS);
        MissedVisitRecord record = missedVisitRecordRepository.findByIdAndAgency_Id(missedVisitRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("MissedVisitRecord", missedVisitRecordId));
        EscalationRequest escalationRequest = escalationRequestRepository.saveAndFlush(EscalationRequest.createForMissedVisit(
                record,
                actorMembership,
                record.getBranch(),
                command.targetRoleKey(),
                command.severity(),
                command.rationale(),
                command.slaDueAt()));
        record.markEscalated();
        evvAuditService.recordEscalationCreated(
                actorMembership,
                escalationRequest.getId(),
                record.getBranch() == null ? null : record.getBranch().getId(),
                "{\"targetRoleKey\":\"" + command.targetRoleKey().trim().toUpperCase() + "\"}");
        return escalationRequest;
    }

    @Transactional(readOnly = true)
    public EvvComplianceProjection getComplianceProjection(@NotNull AgencyMembership actorMembership, @NotNull UUID verificationSessionId) {
        EvvVerificationSession session = evvVerificationSessionRepository.findByIdAndAgency_Id(verificationSessionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("EvvVerificationSession", verificationSessionId));
        return computeProjection(session);
    }

    private void refreshCompliance(EvvVerificationSession session) {
        EvvComplianceProjection projection = computeProjection(session);
        EvvVerificationStatus status;
        if (projection.missedVisitReported()) {
            status = EvvVerificationStatus.MISSED_VISIT_REPORTED;
        } else if (projection.overallOutcome() == EvvComplianceOutcome.BLOCKED) {
            status = EvvVerificationStatus.EXCEPTION_OPEN;
        } else if (projection.overallOutcome() == EvvComplianceOutcome.READY_WITH_WARNING) {
            status = EvvVerificationStatus.VERIFIED_WITH_WARNING;
        } else if (projection.overallOutcome() == EvvComplianceOutcome.READY) {
            status = EvvVerificationStatus.VERIFIED;
        } else {
            status = EvvVerificationStatus.PENDING_VERIFICATION;
        }
        session.refreshOutcome(status, projection.overallOutcome());
        evvVerificationSessionRepository.saveAndFlush(session);
    }

    private EvvComplianceProjection computeProjection(EvvVerificationSession session) {
        List<EvvClockEvent> clockEvents = evvClockEventRepository.findAllByVerificationSession_IdOrderByCapturedAtAsc(session.getId());
        boolean hasClockIn = clockEvents.stream().anyMatch(event -> event.getEventType() == EvvClockEventType.CLOCK_IN);
        boolean hasClockOut = clockEvents.stream().anyMatch(event -> event.getEventType() == EvvClockEventType.CLOCK_OUT);
        List<EvvGeofenceEvaluation> evaluations = evvGeofenceEvaluationRepository.findAllByVerificationSession_IdOrderByCreatedAtAsc(session.getId());
        GeofenceEvaluationOutcome geofenceOutcome = evaluations.stream()
                .map(EvvGeofenceEvaluation::getOutcome)
                .reduce(GeofenceEvaluationOutcome.WITHIN_TOLERANCE, this::combineGeofenceOutcome);
        boolean signatureMissing = signatureVerificationLinkRepository.findAllByVerificationSession_IdOrderByRecordedAtAsc(session.getId()).stream()
                .anyMatch(link -> link.getVerificationStatus() == SignatureVerificationStatus.MISSING);
        boolean signatureComplete = signatureVerificationLinkRepository.findAllByVerificationSession_IdOrderByRecordedAtAsc(session.getId()).stream()
                .anyMatch(link -> link.getVerificationStatus() == SignatureVerificationStatus.PRESENT);
        int openExceptionCount = Math.toIntExact(visitExceptionRecordRepository.countByVerificationSession_IdAndStatusIn(
                session.getId(),
                EnumSet.of(VisitExceptionStatus.OPEN, VisitExceptionStatus.ACKNOWLEDGED, VisitExceptionStatus.ESCALATED)));
        boolean missedVisitReported = missedVisitRecordRepository.existsByVisitOccurrence_IdAndStatusIn(
                session.getVisitOccurrenceId(),
                EnumSet.of(MissedVisitStatus.REPORTED, MissedVisitStatus.NOTIFIED, MissedVisitStatus.ESCALATED));

        if (missedVisitReported) {
            return EvvComplianceProjection.missedVisit(openExceptionCount);
        }
        if (!hasClockIn || !hasClockOut || signatureMissing || geofenceOutcome == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_BLOCKED || openExceptionCount > 0) {
            return EvvComplianceProjection.blocked(hasClockIn, hasClockOut, geofenceOutcome, signatureComplete && !signatureMissing, openExceptionCount);
        }
        if (geofenceOutcome == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_WARNING || geofenceOutcome == GeofenceEvaluationOutcome.NOT_EVALUABLE) {
            return EvvComplianceProjection.readyWithWarning(hasClockIn, hasClockOut, geofenceOutcome, signatureComplete, openExceptionCount);
        }
        return EvvComplianceProjection.ready(hasClockIn, hasClockOut, signatureComplete);
    }

    private GeofenceEvaluationOutcome combineGeofenceOutcome(GeofenceEvaluationOutcome left, GeofenceEvaluationOutcome right) {
        if (left == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_BLOCKED || right == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_BLOCKED) {
            return GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_BLOCKED;
        }
        if (left == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_WARNING || right == GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_WARNING) {
            return GeofenceEvaluationOutcome.OUTSIDE_TOLERANCE_WARNING;
        }
        if (left == GeofenceEvaluationOutcome.NOT_EVALUABLE || right == GeofenceEvaluationOutcome.NOT_EVALUABLE) {
            return GeofenceEvaluationOutcome.NOT_EVALUABLE;
        }
        return GeofenceEvaluationOutcome.WITHIN_TOLERANCE;
    }

    private GeofenceEvaluationResult evaluateGeofence(EvvVerificationSession session, BigDecimal latitude, BigDecimal longitude) {
        PatientAddress patientAddress = patientAddressRepository.findByPatient_Id(session.getPatientId()).orElse(null);
        if (latitude == null || longitude == null) {
            return GeofenceEvaluationResult.notEvaluable("LOCATION_NOT_CAPTURED");
        }
        if (patientAddress == null || patientAddress.getLatitude() == null || patientAddress.getLongitude() == null) {
            return GeofenceEvaluationResult.notEvaluable("EXPECTED_LOCATION_UNAVAILABLE");
        }
        GeofenceToleranceRule rule = resolveActiveRule(session)
                .orElse(GeofenceToleranceRule.createAgencyDefault(session.getAgency(), "DEFAULT_EVV_RULE", DEFAULT_TOLERANCE_METERS, 100, false));
        int distance = haversineMeters(latitude, longitude, patientAddress.getLatitude(), patientAddress.getLongitude());
        if (distance <= rule.getToleranceMeters()) {
            return GeofenceEvaluationResult.withinTolerance(distance, rule.getToleranceMeters());
        }
        if (rule.isHardBlockOutsideTolerance()) {
            return GeofenceEvaluationResult.blocked(distance, rule.getToleranceMeters(), "OUTSIDE_TOLERANCE");
        }
        return GeofenceEvaluationResult.warning(distance, rule.getToleranceMeters(), "OUTSIDE_TOLERANCE");
    }

    private Optional<GeofenceToleranceRule> resolveActiveRule(EvvVerificationSession session) {
        UUID branchId = session.getBranchId();
        if (branchId != null) {
            Optional<GeofenceToleranceRule> branchRule = geofenceToleranceRuleRepository
                    .findFirstByAgency_IdAndBranch_IdAndActiveTrueOrderByCreatedAtDesc(session.getAgencyId(), branchId);
            if (branchRule.isPresent()) {
                return branchRule;
            }
        }
        return geofenceToleranceRuleRepository.findFirstByAgency_IdAndBranchIsNullAndActiveTrueOrderByCreatedAtDesc(session.getAgencyId());
    }

    private int haversineMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double earthRadiusMeters = 6371000d;
        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double startLat = Math.toRadians(lat1.doubleValue());
        double endLat = Math.toRadians(lat2.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(startLat) * Math.cos(endLat) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusMeters * c).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private ActorContext resolveActorContext(AgencyMembership actorMembership, UUID visitOccurrenceId, AgencyPermission ownPermission) {
        VisitOccurrence visitOccurrence = visitOccurrenceRepository.findByIdAndAgency_Id(visitOccurrenceId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
        if (agencyAuthorizationGuard.hasPermission(actorMembership, ownPermission)) {
            CaregiverProfile caregiverProfile = caregiverProfileRepository.findFirstByAgency_IdAndAgencyMembership_Id(actorMembership.getAgencyId(), actorMembership.getId())
                    .orElseThrow(() -> new UnauthorizedEvvActorException(actorMembership.getId()));
            CaregiverVisitAssignment assignment = caregiverVisitAssignmentRepository
                    .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visitOccurrenceId, CaregiverAssignmentStatus.ACTIVE)
                    .orElseThrow(() -> new UnauthorizedEvvActorException(actorMembership.getId()));
            if (!Objects.equals(assignment.getCaregiverProfileId(), caregiverProfile.getId())) {
                throw new UnauthorizedEvvActorException(actorMembership.getId());
            }
            return new ActorContext(visitOccurrence, caregiverProfile);
        }
        throw new UnauthorizedEvvActorException(actorMembership.getId());
    }

    private EvvVerificationSession resolveOwnedVerificationSession(AgencyMembership actorMembership, UUID verificationSessionId) {
        EvvVerificationSession session = evvVerificationSessionRepository.findByIdAndAgency_Id(verificationSessionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new EvvEntityNotFoundException("EvvVerificationSession", verificationSessionId));
        CaregiverProfile caregiverProfile = caregiverProfileRepository.findFirstByAgency_IdAndAgencyMembership_Id(actorMembership.getAgencyId(), actorMembership.getId())
                .orElseThrow(() -> new UnauthorizedEvvActorException(actorMembership.getId()));
        if (!Objects.equals(session.getCaregiverProfileId(), caregiverProfile.getId())) {
            throw new UnauthorizedEvvActorException(actorMembership.getId());
        }
        return session;
    }

    private MobileVisitExecutionSession resolveExecutionSession(UUID agencyId, UUID executionSessionId, UUID visitOccurrenceId) {
        if (executionSessionId == null) {
            return null;
        }
        MobileVisitExecutionSession session = mobileVisitExecutionSessionRepository.findByIdAndAgency_Id(executionSessionId, agencyId)
                .orElseThrow(() -> new EvvEntityNotFoundException("MobileVisitExecutionSession", executionSessionId));
        if (!Objects.equals(session.getVisitOccurrenceId(), visitOccurrenceId)) {
            throw new IllegalArgumentException("executionSession must belong to the same visit as the EVV verification session");
        }
        return session;
    }

    private AgencyMembership resolveNotificationRecipient(UUID agencyId, UUID recipientMembershipId) {
        AgencyMembership membership = agencyMembershipRepository.findById(recipientMembershipId)
                .orElseThrow(() -> new EvvEntityNotFoundException("AgencyMembership", recipientMembershipId));
        if (!Objects.equals(membership.getAgencyId(), agencyId) || membership.getStatus() != AgencyMembershipStatus.ACTIVE) {
            throw new IllegalArgumentException("recipientMembership must be active in the same agency");
        }
        if (!Set.of(AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.QA_CLINICAL_REVIEWER).contains(membership.getRole())) {
            throw new IllegalArgumentException("recipientMembership must be an operational EVV notification role");
        }
        return membership;
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedEvvActorException::new);
    }

    private String clockMetadata(EvvClockEvent event) {
        return "{\"eventType\":\"" + event.getEventType().name() + "\",\"verificationStatus\":\"" + event.getVerificationStatus().name() + "\"}";
    }

    private String geofenceMetadata(GeofenceEvaluationResult result) {
        return "{\"outcome\":\"" + result.outcome().name() + "\",\"reasonCode\":\"" + result.reasonCode() + "\"}";
    }

    private record ActorContext(VisitOccurrence visitOccurrence, CaregiverProfile caregiverProfile) {
    }

    public record RecordClockEventCommand(
            @NotNull EvvClockEventType eventType,
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
        boolean hasDeviceMetadata() {
            return platformSummary != null || appVersion != null || deviceClass != null || timezoneOffsetMinutes != null
                    || userAgentHash != null || sessionFingerprintHash != null;
        }
    }

    public record RecordSignatureStatusCommand(
            UUID artifactId,
            @NotNull SignatureSignerRole signerRole,
            @NotNull SignatureVerificationStatus verificationStatus,
            @NotNull OffsetDateTime recordedAt) {
    }

    public record LogVisitExceptionCommand(
            @NotNull VisitExceptionType exceptionType,
            @NotNull VisitExceptionSeverity severity,
            @NotBlank String reasonCode,
            @NotBlank String narrative) {
    }

    public record ReportMissedVisitCommand(
            @NotBlank String reasonCode,
            @NotBlank String narrative,
            @NotNull OffsetDateTime reportedAt) {
    }

    public record NotifySupervisorCommand(
            @NotNull UUID recipientMembershipId,
            @NotBlank String channel,
            @NotBlank String rationale,
            @NotNull OffsetDateTime createdAt) {
    }

    public record CreateEscalationCommand(
            @NotBlank String targetRoleKey,
            @NotNull VisitExceptionSeverity severity,
            @NotBlank String rationale,
            OffsetDateTime slaDueAt) {
    }

    public record UpdateExceptionStatusCommand(
            @NotNull VisitExceptionStatus status,
            @NotNull OffsetDateTime actedAt) {
    }
}
