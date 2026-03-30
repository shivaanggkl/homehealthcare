package com.homehealthcare.patientevent.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patientevent.application.PatientEventRecordService;
import com.homehealthcare.patientevent.application.PatientEventRecordService.AddWoundHistoryCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.AssignFollowUpCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateEscalationCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateIncidentCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateInfectionCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateWoundCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.LongitudinalHistoryEntryView;
import com.homehealthcare.patientevent.application.PatientEventRecordService.PatientEventSummaryView;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateFollowUpCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateIncidentCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateInfectionCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateWoundCommand;
import com.homehealthcare.patientevent.domain.IncidentRecord;
import com.homehealthcare.patientevent.domain.InfectionRecord;
import com.homehealthcare.patientevent.domain.PatientEventEscalationRecord;
import com.homehealthcare.patientevent.domain.PatientEventEvidenceLink;
import com.homehealthcare.patientevent.domain.PatientEventFollowUpAssignment;
import com.homehealthcare.patientevent.domain.WoundHistoryEntry;
import com.homehealthcare.patientevent.domain.WoundRecord;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.IncidentRecordStatus;
import com.homehealthcare.patientevent.foundation.InfectionRecordStatus;
import com.homehealthcare.patientevent.foundation.PatientEventAlertContract;
import com.homehealthcare.patientevent.foundation.PatientEventEscalationStatus;
import com.homehealthcare.patientevent.foundation.PatientEventEvidenceSourceType;
import com.homehealthcare.patientevent.foundation.PatientEventFollowUpStatus;
import com.homehealthcare.patientevent.foundation.PatientEventHistoryEntryType;
import com.homehealthcare.patientevent.foundation.WoundRecordStatus;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
@RequestMapping("/api/patient-events")
class PatientEventController {

    private final ConfigurationActorResolver actorResolver;
    private final PatientEventRecordService patientEventRecordService;

    PatientEventController(ConfigurationActorResolver actorResolver, PatientEventRecordService patientEventRecordService) {
        this.actorResolver = actorResolver;
        this.patientEventRecordService = patientEventRecordService;
    }

    @GetMapping("/incidents")
    List<IncidentResponse> listIncidents(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) IncidentRecordStatus status) {
        return patientEventRecordService.listIncidents(actorResolver.requireActorMembership(), patientId, branchId, status).stream()
                .map(PatientEventController::toIncidentResponse)
                .toList();
    }

    @PostMapping("/incidents")
    IncidentResponse createIncident(@Valid @RequestBody SaveIncidentRequest request) {
        return toIncidentResponse(patientEventRecordService.createIncident(
                actorResolver.requireActorMembership(),
                new CreateIncidentCommand(
                        request.patientId(),
                        request.branchId(),
                        request.visitOccurrenceId(),
                        request.incidentType(),
                        request.severityLabel(),
                        request.occurredAt(),
                        request.reportedAt(),
                        request.summary(),
                        request.reportedByMembershipId())));
    }

    @GetMapping("/incidents/{incidentId}")
    IncidentResponse getIncident(@PathVariable UUID incidentId) {
        return toIncidentResponse(patientEventRecordService.getIncident(actorResolver.requireActorMembership(), incidentId));
    }

    @PutMapping("/incidents/{incidentId}")
    IncidentResponse updateIncident(@PathVariable UUID incidentId, @Valid @RequestBody UpdateIncidentRequest request) {
        return toIncidentResponse(patientEventRecordService.updateIncident(
                actorResolver.requireActorMembership(),
                incidentId,
                new UpdateIncidentCommand(
                        request.branchId(),
                        request.visitOccurrenceId(),
                        request.incidentType(),
                        request.severityLabel(),
                        request.occurredAt(),
                        request.reportedAt(),
                        request.summary(),
                        request.status(),
                        request.reportedByMembershipId())));
    }

    @PostMapping("/incidents/{incidentId}/resolve")
    IncidentResponse resolveIncident(@PathVariable UUID incidentId, @Valid @RequestBody ResolveRequest request) {
        return toIncidentResponse(patientEventRecordService.resolveIncident(
                actorResolver.requireActorMembership(),
                incidentId,
                request.resolvedAt()));
    }

    @GetMapping("/infections")
    List<InfectionResponse> listInfections(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) InfectionRecordStatus status) {
        return patientEventRecordService.listInfections(actorResolver.requireActorMembership(), patientId, branchId, status).stream()
                .map(PatientEventController::toInfectionResponse)
                .toList();
    }

    @PostMapping("/infections")
    InfectionResponse createInfection(@Valid @RequestBody SaveInfectionRequest request) {
        return toInfectionResponse(patientEventRecordService.createInfection(
                actorResolver.requireActorMembership(),
                new CreateInfectionCommand(
                        request.patientId(),
                        request.branchId(),
                        request.relatedIncidentId(),
                        request.onsetDate(),
                        request.identifiedAt(),
                        request.infectionType(),
                        request.summary(),
                        request.status())));
    }

    @GetMapping("/infections/{infectionId}")
    InfectionResponse getInfection(@PathVariable UUID infectionId) {
        return toInfectionResponse(patientEventRecordService.getInfection(actorResolver.requireActorMembership(), infectionId));
    }

    @PutMapping("/infections/{infectionId}")
    InfectionResponse updateInfection(@PathVariable UUID infectionId, @Valid @RequestBody UpdateInfectionRequest request) {
        return toInfectionResponse(patientEventRecordService.updateInfection(
                actorResolver.requireActorMembership(),
                infectionId,
                new UpdateInfectionCommand(
                        request.branchId(),
                        request.relatedIncidentId(),
                        request.onsetDate(),
                        request.identifiedAt(),
                        request.infectionType(),
                        request.summary(),
                        request.status())));
    }

    @PostMapping("/infections/{infectionId}/resolve")
    InfectionResponse resolveInfection(@PathVariable UUID infectionId, @Valid @RequestBody ResolveRequest request) {
        return toInfectionResponse(patientEventRecordService.resolveInfection(
                actorResolver.requireActorMembership(),
                infectionId,
                request.resolvedAt()));
    }

    @GetMapping("/wounds")
    List<WoundResponse> listWounds(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) WoundRecordStatus status) {
        return patientEventRecordService.listWounds(actorResolver.requireActorMembership(), patientId, branchId, status).stream()
                .map(PatientEventController::toWoundResponse)
                .toList();
    }

    @PostMapping("/wounds")
    WoundResponse createWound(@Valid @RequestBody SaveWoundRequest request) {
        return toWoundResponse(patientEventRecordService.createWound(
                actorResolver.requireActorMembership(),
                new CreateWoundCommand(
                        request.patientId(),
                        request.branchId(),
                        request.identifiedAt(),
                        request.woundTypeOrSite(),
                        request.currentStatus(),
                        request.baselineSummary())));
    }

    @GetMapping("/wounds/{woundId}")
    WoundResponse getWound(@PathVariable UUID woundId) {
        return toWoundResponse(patientEventRecordService.getWound(actorResolver.requireActorMembership(), woundId));
    }

    @PutMapping("/wounds/{woundId}")
    WoundResponse updateWound(@PathVariable UUID woundId, @Valid @RequestBody UpdateWoundRequest request) {
        return toWoundResponse(patientEventRecordService.updateWound(
                actorResolver.requireActorMembership(),
                woundId,
                new UpdateWoundCommand(
                        request.branchId(),
                        request.woundTypeOrSite(),
                        request.currentStatus(),
                        request.baselineSummary())));
    }

    @PostMapping("/wounds/{woundId}/resolve")
    WoundResponse resolveWound(@PathVariable UUID woundId, @Valid @RequestBody ResolveRequest request) {
        return toWoundResponse(patientEventRecordService.resolveWound(
                actorResolver.requireActorMembership(),
                woundId,
                request.resolvedAt()));
    }

    @GetMapping("/wounds/{woundId}/history")
    List<WoundHistoryResponse> listWoundHistory(@PathVariable UUID woundId) {
        return patientEventRecordService.listWoundHistory(actorResolver.requireActorMembership(), woundId).stream()
                .map(PatientEventController::toWoundHistoryResponse)
                .toList();
    }

    @PostMapping("/wounds/{woundId}/history")
    WoundHistoryResponse addWoundHistory(@PathVariable UUID woundId, @Valid @RequestBody SaveWoundHistoryRequest request) {
        return toWoundHistoryResponse(patientEventRecordService.addWoundHistory(
                actorResolver.requireActorMembership(),
                woundId,
                new AddWoundHistoryCommand(
                        request.branchId(),
                        request.capturedAt(),
                        request.observationSummary(),
                        request.lengthCm(),
                        request.widthCm(),
                        request.depthCm(),
                        request.progressionMarker(),
                        request.capturedByMembershipId())));
    }

    @GetMapping("/evidence-links")
    List<EvidenceLinkResponse> listEvidenceLinks(
            @RequestParam(name = "targetType") Epic12PatientEventTargetType targetType,
            @RequestParam(name = "targetId") UUID targetId) {
        return patientEventRecordService.listEvidenceLinks(actorResolver.requireActorMembership(), targetType, targetId).stream()
                .map(PatientEventController::toEvidenceLinkResponse)
                .toList();
    }

    @PostMapping("/evidence-links")
    EvidenceLinkResponse linkEvidence(@Valid @RequestBody LinkEvidenceRequest request) {
        return toEvidenceLinkResponse(patientEventRecordService.linkEvidence(
                actorResolver.requireActorMembership(),
                new PatientEventRecordService.LinkEvidenceCommand(
                        request.targetType(),
                        request.targetId(),
                        request.branchId(),
                        request.patientAttachmentId(),
                        request.mobileArtifactId(),
                        request.documentationAttachmentLinkId(),
                        request.linkedByMembershipId(),
                        request.linkedAt())));
    }

    @DeleteMapping("/evidence-links/{evidenceLinkId}")
    void unlinkEvidence(@PathVariable UUID evidenceLinkId) {
        patientEventRecordService.unlinkEvidence(actorResolver.requireActorMembership(), evidenceLinkId);
    }

    @GetMapping("/follow-ups")
    List<FollowUpResponse> listFollowUps(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "targetType", required = false) Epic12PatientEventTargetType targetType,
            @RequestParam(name = "targetId", required = false) UUID targetId,
            @RequestParam(name = "status", required = false) PatientEventFollowUpStatus status,
            @RequestParam(name = "overdueAsOf", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime overdueAsOf) {
        return patientEventRecordService.listFollowUps(actorResolver.requireActorMembership(), patientId, targetType, targetId, status, overdueAsOf).stream()
                .map(PatientEventController::toFollowUpResponse)
                .toList();
    }

    @PostMapping("/follow-ups")
    FollowUpResponse assignFollowUp(@Valid @RequestBody SaveFollowUpRequest request) {
        return toFollowUpResponse(patientEventRecordService.assignFollowUp(
                actorResolver.requireActorMembership(),
                new AssignFollowUpCommand(
                        request.targetType(),
                        request.targetId(),
                        request.branchId(),
                        request.ownerMembershipId(),
                        request.ownerRole(),
                        request.assignedAt(),
                        request.dueAt(),
                        request.followUpNote())));
    }

    @PutMapping("/follow-ups/{followUpId}")
    FollowUpResponse updateFollowUp(@PathVariable UUID followUpId, @Valid @RequestBody UpdateFollowUpRequest request) {
        return toFollowUpResponse(patientEventRecordService.updateFollowUp(
                actorResolver.requireActorMembership(),
                followUpId,
                new UpdateFollowUpCommand(
                        request.branchId(),
                        request.ownerMembershipId(),
                        request.ownerRole(),
                        request.dueAt(),
                        request.followUpNote())));
    }

    @PostMapping("/follow-ups/{followUpId}/complete")
    FollowUpResponse completeFollowUp(@PathVariable UUID followUpId, @Valid @RequestBody CompleteFollowUpRequest request) {
        return toFollowUpResponse(patientEventRecordService.completeFollowUp(
                actorResolver.requireActorMembership(),
                followUpId,
                request.completionAt(),
                request.followUpNote()));
    }

    @GetMapping("/escalations")
    List<EscalationResponse> listEscalations(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "targetType", required = false) Epic12PatientEventTargetType targetType,
            @RequestParam(name = "targetId", required = false) UUID targetId,
            @RequestParam(name = "status", required = false) PatientEventEscalationStatus status) {
        return patientEventRecordService.listEscalations(actorResolver.requireActorMembership(), patientId, targetType, targetId, status).stream()
                .map(PatientEventController::toEscalationResponse)
                .toList();
    }

    @PostMapping("/escalations")
    EscalationResponse createEscalation(@Valid @RequestBody SaveEscalationRequest request) {
        return toEscalationResponse(patientEventRecordService.createEscalation(
                actorResolver.requireActorMembership(),
                new CreateEscalationCommand(
                        request.targetType(),
                        request.targetId(),
                        request.branchId(),
                        request.severityLabel(),
                        request.reasonTag(),
                        request.escalatedByMembershipId(),
                        request.escalatedAt())));
    }

    @PostMapping("/escalations/{escalationId}/clear")
    EscalationResponse clearEscalation(@PathVariable UUID escalationId, @Valid @RequestBody ClearEscalationRequest request) {
        return toEscalationResponse(patientEventRecordService.clearEscalation(
                actorResolver.requireActorMembership(),
                escalationId,
                request.clearedAt(),
                request.clearedByMembershipId()));
    }

    @GetMapping("/patients/{patientId}/timeline")
    List<LongitudinalHistoryResponse> getTimeline(@PathVariable UUID patientId) {
        return patientEventRecordService.listPatientLongitudinalHistory(actorResolver.requireActorMembership(), patientId).stream()
                .map(PatientEventController::toLongitudinalHistoryResponse)
                .toList();
    }

    @GetMapping("/patients/{patientId}/alerts")
    List<AlertResponse> getAlerts(
            @PathVariable UUID patientId,
            @RequestParam(name = "asOf", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime asOf) {
        return patientEventRecordService.projectPatientAlerts(
                        actorResolver.requireActorMembership(),
                        patientId,
                        asOf == null ? OffsetDateTime.now() : asOf)
                .stream()
                .map(PatientEventController::toAlertResponse)
                .toList();
    }

    @GetMapping("/patients/{patientId}/summary")
    PatientEventSummaryResponse getSummary(@PathVariable UUID patientId) {
        return toPatientEventSummaryResponse(patientEventRecordService.getPatientEventSummary(actorResolver.requireActorMembership(), patientId));
    }

    private static IncidentResponse toIncidentResponse(IncidentRecord record) {
        return new IncidentResponse(
                record.getId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getVisitOccurrence() == null ? null : record.getVisitOccurrence().getId(),
                record.getIncidentType(),
                record.getSeverityLabel(),
                record.getOccurredAt(),
                record.getReportedAt(),
                record.getSummary(),
                record.getStatus(),
                record.getReportedByMembership() == null ? null : record.getReportedByMembership().getId(),
                record.getResolvedAt());
    }

    private static InfectionResponse toInfectionResponse(InfectionRecord record) {
        return new InfectionResponse(
                record.getId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getRelatedIncident() == null ? null : record.getRelatedIncident().getId(),
                record.getOnsetDate(),
                record.getIdentifiedAt(),
                record.getInfectionType(),
                record.getSummary(),
                record.getStatus(),
                record.getResolvedAt());
    }

    private static WoundResponse toWoundResponse(WoundRecord record) {
        return new WoundResponse(
                record.getId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getIdentifiedAt(),
                record.getWoundTypeOrSite(),
                record.getCurrentStatus(),
                record.getBaselineSummary(),
                record.isActive(),
                record.getResolvedAt());
    }

    private static WoundHistoryResponse toWoundHistoryResponse(WoundHistoryEntry entry) {
        return new WoundHistoryResponse(
                entry.getId(),
                entry.getWoundRecord().getId(),
                entry.getPatientId(),
                entry.getBranchId(),
                entry.getCapturedAt(),
                entry.getObservationSummary(),
                entry.getLengthCm(),
                entry.getWidthCm(),
                entry.getDepthCm(),
                entry.getProgressionMarker(),
                entry.getCapturedByMembership() == null ? null : entry.getCapturedByMembership().getId());
    }

    private static EvidenceLinkResponse toEvidenceLinkResponse(PatientEventEvidenceLink link) {
        return new EvidenceLinkResponse(
                link.getId(),
                link.getPatientId(),
                link.getBranchId(),
                link.getTargetType(),
                link.getTargetId(),
                link.getSourceType(),
                link.getPatientAttachment() == null ? null : link.getPatientAttachment().getId(),
                link.getMobileArtifact() == null ? null : link.getMobileArtifact().getId(),
                link.getDocumentationAttachmentLink() == null ? null : link.getDocumentationAttachmentLink().getId(),
                link.getLinkedByMembership().getId(),
                link.getLinkedAt());
    }

    private static FollowUpResponse toFollowUpResponse(PatientEventFollowUpAssignment assignment) {
        return new FollowUpResponse(
                assignment.getId(),
                assignment.getPatientId(),
                assignment.getBranchId(),
                assignment.getTargetType(),
                assignment.getTargetId(),
                assignment.getOwnerMembership() == null ? null : assignment.getOwnerMembership().getId(),
                assignment.getOwnerRole(),
                assignment.getAssignedAt(),
                assignment.getDueAt(),
                assignment.getCompletionAt(),
                assignment.getFollowUpNote(),
                assignment.getStatus());
    }

    private static EscalationResponse toEscalationResponse(PatientEventEscalationRecord record) {
        return new EscalationResponse(
                record.getId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getTargetType(),
                record.getTargetId(),
                record.getStatus(),
                record.getSeverityLabel(),
                record.getReasonTag(),
                record.getEscalatedByMembership().getId(),
                record.getEscalatedAt(),
                record.getClearedByMembership() == null ? null : record.getClearedByMembership().getId(),
                record.getClearedAt());
    }

    private static LongitudinalHistoryResponse toLongitudinalHistoryResponse(LongitudinalHistoryEntryView view) {
        return new LongitudinalHistoryResponse(
                view.historyEntryType(),
                view.targetType(),
                view.targetId(),
                view.occurredAt(),
                view.branchId(),
                view.status(),
                view.severity(),
                view.summary());
    }

    private static AlertResponse toAlertResponse(PatientEventAlertContract contract) {
        return new AlertResponse(
                contract.patientId(),
                contract.branchId(),
                contract.alertType(),
                contract.targetId(),
                contract.targetType(),
                contract.severity(),
                contract.summary());
    }

    private static PatientEventSummaryResponse toPatientEventSummaryResponse(PatientEventSummaryView view) {
        return new PatientEventSummaryResponse(
                view.patientId(),
                view.branchId(),
                view.openIncidentCount(),
                view.activeInfectionCount(),
                view.activeWoundCount(),
                view.openFollowUpCount(),
                view.activeEscalationCount(),
                view.alerts().stream().map(PatientEventController::toAlertResponse).toList());
    }

    record SaveIncidentRequest(
            @NotNull UUID patientId,
            UUID branchId,
            UUID visitOccurrenceId,
            @NotBlank String incidentType,
            String severityLabel,
            @NotNull OffsetDateTime occurredAt,
            @NotNull OffsetDateTime reportedAt,
            @NotBlank String summary,
            UUID reportedByMembershipId) {
    }

    record UpdateIncidentRequest(
            UUID branchId,
            UUID visitOccurrenceId,
            @NotBlank String incidentType,
            String severityLabel,
            @NotNull OffsetDateTime occurredAt,
            @NotNull OffsetDateTime reportedAt,
            @NotBlank String summary,
            @NotNull IncidentRecordStatus status,
            UUID reportedByMembershipId) {
    }

    record SaveInfectionRequest(
            @NotNull UUID patientId,
            UUID branchId,
            UUID relatedIncidentId,
            LocalDate onsetDate,
            @NotNull OffsetDateTime identifiedAt,
            @NotBlank String infectionType,
            @NotBlank String summary,
            InfectionRecordStatus status) {
    }

    record UpdateInfectionRequest(
            UUID branchId,
            UUID relatedIncidentId,
            LocalDate onsetDate,
            @NotNull OffsetDateTime identifiedAt,
            @NotBlank String infectionType,
            @NotBlank String summary,
            @NotNull InfectionRecordStatus status) {
    }

    record SaveWoundRequest(
            @NotNull UUID patientId,
            UUID branchId,
            @NotNull OffsetDateTime identifiedAt,
            @NotBlank String woundTypeOrSite,
            WoundRecordStatus currentStatus,
            String baselineSummary) {
    }

    record UpdateWoundRequest(
            UUID branchId,
            @NotBlank String woundTypeOrSite,
            @NotNull WoundRecordStatus currentStatus,
            String baselineSummary) {
    }

    record SaveWoundHistoryRequest(
            UUID branchId,
            @NotNull OffsetDateTime capturedAt,
            @NotBlank String observationSummary,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal depthCm,
            String progressionMarker,
            UUID capturedByMembershipId) {
    }

    record LinkEvidenceRequest(
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId,
            UUID branchId,
            UUID patientAttachmentId,
            UUID mobileArtifactId,
            UUID documentationAttachmentLinkId,
            UUID linkedByMembershipId,
            @NotNull OffsetDateTime linkedAt) {
    }

    record SaveFollowUpRequest(
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId,
            UUID branchId,
            UUID ownerMembershipId,
            AgencyRole ownerRole,
            @NotNull OffsetDateTime assignedAt,
            @NotNull OffsetDateTime dueAt,
            String followUpNote) {
    }

    record UpdateFollowUpRequest(
            UUID branchId,
            UUID ownerMembershipId,
            AgencyRole ownerRole,
            @NotNull OffsetDateTime dueAt,
            String followUpNote) {
    }

    record CompleteFollowUpRequest(@NotNull OffsetDateTime completionAt, String followUpNote) {
    }

    record SaveEscalationRequest(
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId,
            UUID branchId,
            String severityLabel,
            String reasonTag,
            UUID escalatedByMembershipId,
            @NotNull OffsetDateTime escalatedAt) {
    }

    record ClearEscalationRequest(@NotNull OffsetDateTime clearedAt, UUID clearedByMembershipId) {
    }

    record ResolveRequest(@NotNull OffsetDateTime resolvedAt) {
    }

    record IncidentResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID visitOccurrenceId,
            String incidentType,
            String severityLabel,
            OffsetDateTime occurredAt,
            OffsetDateTime reportedAt,
            String summary,
            IncidentRecordStatus status,
            UUID reportedByMembershipId,
            OffsetDateTime resolvedAt) {
    }

    record InfectionResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID relatedIncidentId,
            LocalDate onsetDate,
            OffsetDateTime identifiedAt,
            String infectionType,
            String summary,
            InfectionRecordStatus status,
            OffsetDateTime resolvedAt) {
    }

    record WoundResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            OffsetDateTime identifiedAt,
            String woundTypeOrSite,
            WoundRecordStatus currentStatus,
            String baselineSummary,
            boolean active,
            OffsetDateTime resolvedAt) {
    }

    record WoundHistoryResponse(
            UUID id,
            UUID woundId,
            UUID patientId,
            UUID branchId,
            OffsetDateTime capturedAt,
            String observationSummary,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal depthCm,
            String progressionMarker,
            UUID capturedByMembershipId) {
    }

    record EvidenceLinkResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventEvidenceSourceType sourceType,
            UUID patientAttachmentId,
            UUID mobileArtifactId,
            UUID documentationAttachmentLinkId,
            UUID linkedByMembershipId,
            OffsetDateTime linkedAt) {
    }

    record FollowUpResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            UUID ownerMembershipId,
            AgencyRole ownerRole,
            OffsetDateTime assignedAt,
            OffsetDateTime dueAt,
            OffsetDateTime completionAt,
            String followUpNote,
            PatientEventFollowUpStatus status) {
    }

    record EscalationResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventEscalationStatus status,
            String severityLabel,
            String reasonTag,
            UUID escalatedByMembershipId,
            OffsetDateTime escalatedAt,
            UUID clearedByMembershipId,
            OffsetDateTime clearedAt) {
    }

    record LongitudinalHistoryResponse(
            PatientEventHistoryEntryType historyEntryType,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            OffsetDateTime occurredAt,
            UUID branchId,
            String status,
            String severity,
            String summary) {
    }

    record AlertResponse(
            UUID patientId,
            UUID branchId,
            com.homehealthcare.patientevent.foundation.PatientEventAlertType alertType,
            UUID targetId,
            String targetType,
            String severity,
            String summary) {
    }

    record PatientEventSummaryResponse(
            UUID patientId,
            UUID branchId,
            long openIncidentCount,
            long activeInfectionCount,
            long activeWoundCount,
            long openFollowUpCount,
            long activeEscalationCount,
            List<AlertResponse> alerts) {
    }
}
