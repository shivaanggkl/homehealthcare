package com.homehealthcare.patientevent.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLink;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLinkRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.patientevent.domain.IncidentRecord;
import com.homehealthcare.patientevent.domain.IncidentRecordRepository;
import com.homehealthcare.patientevent.domain.InfectionRecord;
import com.homehealthcare.patientevent.domain.InfectionRecordRepository;
import com.homehealthcare.patientevent.domain.PatientEventEvidenceLink;
import com.homehealthcare.patientevent.domain.PatientEventEvidenceLinkRepository;
import com.homehealthcare.patientevent.domain.PatientEventEscalationRecord;
import com.homehealthcare.patientevent.domain.PatientEventEscalationRecordRepository;
import com.homehealthcare.patientevent.domain.PatientEventFollowUpAssignment;
import com.homehealthcare.patientevent.domain.PatientEventFollowUpAssignmentRepository;
import com.homehealthcare.patientevent.domain.WoundHistoryEntry;
import com.homehealthcare.patientevent.domain.WoundHistoryEntryRepository;
import com.homehealthcare.patientevent.domain.WoundRecord;
import com.homehealthcare.patientevent.domain.WoundRecordRepository;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.IncidentRecordStatus;
import com.homehealthcare.patientevent.foundation.InfectionRecordStatus;
import com.homehealthcare.patientevent.foundation.PatientEventAlertContract;
import com.homehealthcare.patientevent.foundation.PatientEventAlertType;
import com.homehealthcare.patientevent.foundation.PatientEventAuditService;
import com.homehealthcare.patientevent.foundation.PatientEventEscalationStatus;
import com.homehealthcare.patientevent.foundation.PatientEventFollowUpStatus;
import com.homehealthcare.patientevent.foundation.PatientEventHistoryEntryType;
import com.homehealthcare.patientevent.foundation.WoundRecordStatus;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
public class PatientEventRecordService {

    private static final Set<Epic12PatientEventTargetType> EVIDENCE_TARGETS = Set.of(
            Epic12PatientEventTargetType.INCIDENT_RECORD,
            Epic12PatientEventTargetType.INFECTION_RECORD,
            Epic12PatientEventTargetType.WOUND_RECORD,
            Epic12PatientEventTargetType.WOUND_HISTORY_ENTRY);

    private final IncidentRecordRepository incidentRecordRepository;
    private final InfectionRecordRepository infectionRecordRepository;
    private final WoundRecordRepository woundRecordRepository;
    private final WoundHistoryEntryRepository woundHistoryEntryRepository;
    private final PatientEventEvidenceLinkRepository patientEventEvidenceLinkRepository;
    private final PatientEventFollowUpAssignmentRepository patientEventFollowUpAssignmentRepository;
    private final PatientEventEscalationRecordRepository patientEventEscalationRecordRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final PatientAttachmentRepository patientAttachmentRepository;
    private final MobileFieldArtifactRepository mobileFieldArtifactRepository;
    private final DocumentationAttachmentLinkRepository documentationAttachmentLinkRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientEventAuditService patientEventAuditService;

    @Transactional
    public IncidentRecord createIncident(@NotNull AgencyMembership actorMembership, @Valid CreateIncidentCommand command) {
        requirePermission(actorMembership, AgencyPermission.CREATE_INCIDENT_RECORDS, "create incident records");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        VisitOccurrence visitOccurrence = resolveVisitOccurrence(actorMembership.getAgencyId(), command.visitOccurrenceId(), patient.getId());
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.CREATE_INCIDENT_RECORDS,
                firstNonNull(command.branchId(), visitOccurrence == null ? null : visitOccurrence.getBranchId()),
                "create incident records");
        AgencyMembership reportedByMembership = resolveMembership(actorMembership.getAgencyId(), firstNonNull(command.reportedByMembershipId(), actorMembership.getId()));

        IncidentRecord saved = incidentRecordRepository.saveAndFlush(IncidentRecord.create(
                patient,
                branch,
                visitOccurrence,
                command.incidentType(),
                command.severityLabel(),
                command.occurredAt(),
                command.reportedAt(),
                command.summary(),
                reportedByMembership));
        patientEventAuditService.recordIncidentCreated(actorMembership, saved.getId(), saved.getBranchId(), incidentMetadata(saved));
        return saved;
    }

    @Transactional
    public IncidentRecord updateIncident(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID incidentId,
            @Valid UpdateIncidentCommand command) {
        requirePermission(actorMembership, AgencyPermission.CREATE_INCIDENT_RECORDS, "update incident records");
        IncidentRecord incident = resolveIncident(actorMembership.getAgencyId(), incidentId);
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.CREATE_INCIDENT_RECORDS,
                firstNonNull(command.branchId(), incident.getBranchId()),
                "update incident records");
        VisitOccurrence visitOccurrence = resolveVisitOccurrence(
                actorMembership.getAgencyId(),
                command.visitOccurrenceId(),
                incident.getPatientId());
        AgencyMembership reportedByMembership = resolveMembership(
                actorMembership.getAgencyId(),
                firstNonNull(command.reportedByMembershipId(), incident.getReportedByMembership() == null ? null : incident.getReportedByMembership().getId()));
        incident.updateDetails(
                branch,
                visitOccurrence,
                command.incidentType(),
                command.severityLabel(),
                command.occurredAt(),
                command.reportedAt(),
                command.summary(),
                command.status(),
                reportedByMembership);
        IncidentRecord saved = incidentRecordRepository.saveAndFlush(incident);
        patientEventAuditService.recordIncidentUpdated(actorMembership, saved.getId(), saved.getBranchId(), incidentMetadata(saved));
        return saved;
    }

    @Transactional
    public IncidentRecord resolveIncident(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID incidentId,
            @NotNull OffsetDateTime resolvedAt) {
        requirePermission(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, "resolve incident records");
        IncidentRecord incident = resolveIncident(actorMembership.getAgencyId(), incidentId);
        requireBranchAccess(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, incident.getBranchId(), "resolve incident records");
        incident.resolve(resolvedAt);
        IncidentRecord saved = incidentRecordRepository.saveAndFlush(incident);
        patientEventAuditService.recordRecordResolved(
                actorMembership,
                Epic12PatientEventTargetType.INCIDENT_RECORD,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        return saved;
    }

    @Transactional
    public InfectionRecord createInfection(@NotNull AgencyMembership actorMembership, @Valid CreateInfectionCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_INFECTION_RECORDS, "manage infection records");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.MANAGE_INFECTION_RECORDS,
                command.branchId(),
                "manage infection records");
        IncidentRecord relatedIncident = resolveRelatedIncident(actorMembership.getAgencyId(), command.relatedIncidentId(), patient.getId());

        InfectionRecord saved = infectionRecordRepository.saveAndFlush(InfectionRecord.create(
                patient,
                branch,
                relatedIncident,
                command.onsetDate(),
                command.identifiedAt(),
                command.infectionType(),
                command.summary(),
                command.status()));
        patientEventAuditService.recordInfectionCreated(actorMembership, saved.getId(), saved.getBranchId(), infectionMetadata(saved));
        return saved;
    }

    @Transactional
    public InfectionRecord updateInfection(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID infectionId,
            @Valid UpdateInfectionCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_INFECTION_RECORDS, "manage infection records");
        InfectionRecord infection = resolveInfection(actorMembership.getAgencyId(), infectionId);
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.MANAGE_INFECTION_RECORDS,
                firstNonNull(command.branchId(), infection.getBranchId()),
                "manage infection records");
        IncidentRecord relatedIncident = resolveRelatedIncident(actorMembership.getAgencyId(), command.relatedIncidentId(), infection.getPatientId());
        infection.updateDetails(
                branch,
                relatedIncident,
                command.onsetDate(),
                command.identifiedAt(),
                command.infectionType(),
                command.summary(),
                command.status());
        InfectionRecord saved = infectionRecordRepository.saveAndFlush(infection);
        patientEventAuditService.recordInfectionUpdated(actorMembership, saved.getId(), saved.getBranchId(), infectionMetadata(saved));
        return saved;
    }

    @Transactional
    public InfectionRecord resolveInfection(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID infectionId,
            @NotNull OffsetDateTime resolvedAt) {
        requirePermission(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, "resolve infection records");
        InfectionRecord infection = resolveInfection(actorMembership.getAgencyId(), infectionId);
        requireBranchAccess(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, infection.getBranchId(), "resolve infection records");
        infection.resolve(resolvedAt);
        InfectionRecord saved = infectionRecordRepository.saveAndFlush(infection);
        patientEventAuditService.recordRecordResolved(
                actorMembership,
                Epic12PatientEventTargetType.INFECTION_RECORD,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getStatus().name() + "\"}");
        return saved;
    }

    @Transactional
    public WoundRecord createWound(@NotNull AgencyMembership actorMembership, @Valid CreateWoundCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_WOUND_RECORDS, "manage wound records");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.MANAGE_WOUND_RECORDS,
                command.branchId(),
                "manage wound records");

        WoundRecord saved = woundRecordRepository.saveAndFlush(WoundRecord.create(
                patient,
                branch,
                command.identifiedAt(),
                command.woundTypeOrSite(),
                command.currentStatus(),
                command.baselineSummary()));
        patientEventAuditService.recordWoundCreated(actorMembership, saved.getId(), saved.getBranchId(), woundMetadata(saved));
        return saved;
    }

    @Transactional
    public WoundRecord updateWound(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID woundId,
            @Valid UpdateWoundCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_WOUND_RECORDS, "manage wound records");
        WoundRecord wound = resolveWound(actorMembership.getAgencyId(), woundId);
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.MANAGE_WOUND_RECORDS,
                firstNonNull(command.branchId(), wound.getBranchId()),
                "manage wound records");
        wound.updateDetails(branch, command.woundTypeOrSite(), command.currentStatus(), command.baselineSummary());
        WoundRecord saved = woundRecordRepository.saveAndFlush(wound);
        patientEventAuditService.recordWoundUpdated(actorMembership, saved.getId(), saved.getBranchId(), woundMetadata(saved));
        return saved;
    }

    @Transactional
    public WoundRecord resolveWound(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID woundId,
            @NotNull OffsetDateTime resolvedAt) {
        requirePermission(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, "resolve wound records");
        WoundRecord wound = resolveWound(actorMembership.getAgencyId(), woundId);
        requireBranchAccess(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, wound.getBranchId(), "resolve wound records");
        wound.resolve(resolvedAt);
        WoundRecord saved = woundRecordRepository.saveAndFlush(wound);
        patientEventAuditService.recordRecordResolved(
                actorMembership,
                Epic12PatientEventTargetType.WOUND_RECORD,
                saved.getId(),
                saved.getBranchId(),
                "{\"status\":\"" + saved.getCurrentStatus().name() + "\"}");
        return saved;
    }

    @Transactional
    public WoundHistoryEntry addWoundHistory(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID woundId,
            @Valid AddWoundHistoryCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_WOUND_RECORDS, "manage wound records");
        WoundRecord wound = resolveWound(actorMembership.getAgencyId(), woundId);
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_WOUND_RECORDS, firstNonNull(command.branchId(), wound.getBranchId()), "manage wound records");
        AgencyMembership capturedBy = resolveMembership(
                actorMembership.getAgencyId(),
                firstNonNull(command.capturedByMembershipId(), actorMembership.getId()));
        Branch branch = resolveBranch(actorMembership.getAgencyId(), firstNonNull(command.branchId(), wound.getBranchId()));
        WoundHistoryEntry saved = woundHistoryEntryRepository.saveAndFlush(WoundHistoryEntry.create(
                wound,
                resolvePatient(actorMembership.getAgencyId(), wound.getPatientId()),
                branch,
                command.capturedAt(),
                command.observationSummary(),
                command.lengthCm(),
                command.widthCm(),
                command.depthCm(),
                command.progressionMarker(),
                capturedBy));
        patientEventAuditService.recordWoundHistoryAdded(actorMembership, saved.getId(), saved.getBranchId(), woundHistoryMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventEvidenceLink linkEvidence(
            @NotNull AgencyMembership actorMembership,
            @Valid LinkEvidenceCommand command) {
        requirePermission(actorMembership, AgencyPermission.LINK_PATIENT_EVENT_EVIDENCE, "link patient-event evidence");
        if (!EVIDENCE_TARGETS.contains(command.targetType())) {
            throw new PatientEventConflictException("Evidence links may only target incident, infection, wound, or wound-history records.");
        }
        TargetContext target = resolveTarget(actorMembership.getAgencyId(), command.targetType(), command.targetId());
        requireBranchAccess(
                actorMembership,
                AgencyPermission.LINK_PATIENT_EVENT_EVIDENCE,
                firstNonNull(command.branchId(), target.branchId()),
                "link patient-event evidence");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), target.patientId());
        Branch branch = resolveBranch(actorMembership.getAgencyId(), firstNonNull(command.branchId(), target.branchId()));
        AgencyMembership linkedBy = resolveMembership(actorMembership.getAgencyId(), firstNonNull(command.linkedByMembershipId(), actorMembership.getId()));

        PatientEventEvidenceLink saved;
        if (command.patientAttachmentId() != null) {
            PatientAttachment attachment = patientAttachmentRepository.findById(command.patientAttachmentId())
                    .orElseThrow(() -> new PatientEventEntityNotFoundException("PatientAttachment", command.patientAttachmentId()));
            saved = patientEventEvidenceLinkRepository.saveAndFlush(PatientEventEvidenceLink.linkPatientAttachment(
                    patient,
                    branch,
                    command.targetType(),
                    command.targetId(),
                    attachment,
                    linkedBy,
                    command.linkedAt()));
        } else if (command.mobileArtifactId() != null) {
            MobileFieldArtifact artifact = mobileFieldArtifactRepository.findByIdAndAgency_Id(command.mobileArtifactId(), actorMembership.getAgencyId())
                    .orElseThrow(() -> new PatientEventEntityNotFoundException("MobileFieldArtifact", command.mobileArtifactId()));
            saved = patientEventEvidenceLinkRepository.saveAndFlush(PatientEventEvidenceLink.linkMobileArtifact(
                    patient,
                    branch,
                    command.targetType(),
                    command.targetId(),
                    artifact,
                    linkedBy,
                    command.linkedAt()));
        } else if (command.documentationAttachmentLinkId() != null) {
            DocumentationAttachmentLink link = documentationAttachmentLinkRepository.findByIdAndAgency_Id(command.documentationAttachmentLinkId(), actorMembership.getAgencyId())
                    .orElseThrow(() -> new PatientEventEntityNotFoundException("DocumentationAttachmentLink", command.documentationAttachmentLinkId()));
            saved = patientEventEvidenceLinkRepository.saveAndFlush(PatientEventEvidenceLink.linkDocumentationAttachment(
                    patient,
                    branch,
                    command.targetType(),
                    command.targetId(),
                    link,
                    linkedBy,
                    command.linkedAt()));
        } else {
            throw new PatientEventConflictException("Provide one evidence source identifier before linking evidence.");
        }
        patientEventAuditService.recordEvidenceLinked(actorMembership, saved.getId(), saved.getBranchId(), evidenceMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventFollowUpAssignment assignFollowUp(
            @NotNull AgencyMembership actorMembership,
            @Valid AssignFollowUpCommand command) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_PATIENT_EVENT_FOLLOW_UP, "assign patient-event follow-up");
        TargetContext target = resolveTarget(actorMembership.getAgencyId(), command.targetType(), command.targetId());
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.ASSIGN_PATIENT_EVENT_FOLLOW_UP,
                firstNonNull(command.branchId(), target.branchId()),
                "assign patient-event follow-up");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), target.patientId());
        if (patientEventFollowUpAssignmentRepository.existsByAgency_IdAndTargetTypeAndTargetIdAndStatus(
                actorMembership.getAgencyId(),
                command.targetType(),
                command.targetId(),
                PatientEventFollowUpStatus.OPEN)) {
            throw new PatientEventConflictException("An active follow-up assignment already exists for the target.");
        }
        AgencyMembership ownerMembership = resolveMembership(actorMembership.getAgencyId(), command.ownerMembershipId());
        PatientEventFollowUpAssignment saved = patientEventFollowUpAssignmentRepository.saveAndFlush(PatientEventFollowUpAssignment.assign(
                patient,
                branch,
                command.targetType(),
                command.targetId(),
                ownerMembership,
                command.ownerRole(),
                command.assignedAt(),
                command.dueAt(),
                command.followUpNote()));
        patientEventAuditService.recordFollowUpAssigned(actorMembership, saved.getId(), saved.getBranchId(), followUpMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventFollowUpAssignment updateFollowUp(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID followUpAssignmentId,
            @Valid UpdateFollowUpCommand command) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_PATIENT_EVENT_FOLLOW_UP, "update patient-event follow-up");
        PatientEventFollowUpAssignment followUp = resolveFollowUp(actorMembership.getAgencyId(), followUpAssignmentId);
        requireBranchAccess(actorMembership, AgencyPermission.ASSIGN_PATIENT_EVENT_FOLLOW_UP, followUp.getBranchId(), "update patient-event follow-up");
        AgencyMembership ownerMembership = resolveMembership(actorMembership.getAgencyId(), command.ownerMembershipId());
        Branch branch = resolveBranch(actorMembership.getAgencyId(), firstNonNull(command.branchId(), followUp.getBranchId()));
        followUp.updateAssignment(branch, ownerMembership, command.ownerRole(), command.dueAt(), command.followUpNote());
        PatientEventFollowUpAssignment saved = patientEventFollowUpAssignmentRepository.saveAndFlush(followUp);
        patientEventAuditService.recordFollowUpUpdated(actorMembership, saved.getId(), saved.getBranchId(), followUpMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventFollowUpAssignment completeFollowUp(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID followUpAssignmentId,
            @NotNull OffsetDateTime completionAt,
            String followUpNote) {
        requirePermission(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, "complete patient-event follow-up");
        PatientEventFollowUpAssignment followUp = resolveFollowUp(actorMembership.getAgencyId(), followUpAssignmentId);
        requireBranchAccess(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, followUp.getBranchId(), "complete patient-event follow-up");
        followUp.complete(completionAt, followUpNote);
        PatientEventFollowUpAssignment saved = patientEventFollowUpAssignmentRepository.saveAndFlush(followUp);
        patientEventAuditService.recordFollowUpUpdated(actorMembership, saved.getId(), saved.getBranchId(), followUpMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventFollowUpAssignment cancelFollowUp(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID followUpAssignmentId,
            String followUpNote) {
        requirePermission(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, "cancel patient-event follow-up");
        PatientEventFollowUpAssignment followUp = resolveFollowUp(actorMembership.getAgencyId(), followUpAssignmentId);
        requireBranchAccess(actorMembership, AgencyPermission.RESOLVE_PATIENT_EVENTS, followUp.getBranchId(), "cancel patient-event follow-up");
        followUp.cancel(followUpNote);
        PatientEventFollowUpAssignment saved = patientEventFollowUpAssignmentRepository.saveAndFlush(followUp);
        patientEventAuditService.recordFollowUpUpdated(actorMembership, saved.getId(), saved.getBranchId(), followUpMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventEscalationRecord createEscalation(
            @NotNull AgencyMembership actorMembership,
            @Valid CreateEscalationCommand command) {
        requirePermission(actorMembership, AgencyPermission.ESCALATE_PATIENT_EVENTS, "escalate patient-event records");
        TargetContext target = resolveTarget(actorMembership.getAgencyId(), command.targetType(), command.targetId());
        Branch branch = resolveAccessibleBranch(
                actorMembership,
                AgencyPermission.ESCALATE_PATIENT_EVENTS,
                firstNonNull(command.branchId(), target.branchId()),
                "escalate patient-event records");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), target.patientId());
        if (patientEventEscalationRecordRepository.existsByAgency_IdAndTargetTypeAndTargetIdAndStatus(
                actorMembership.getAgencyId(),
                command.targetType(),
                command.targetId(),
                PatientEventEscalationStatus.ACTIVE)) {
            throw new PatientEventConflictException("An active escalation already exists for the target.");
        }
        AgencyMembership escalatedBy = resolveMembership(actorMembership.getAgencyId(), firstNonNull(command.escalatedByMembershipId(), actorMembership.getId()));
        PatientEventEscalationRecord saved = patientEventEscalationRecordRepository.saveAndFlush(PatientEventEscalationRecord.create(
                patient,
                branch,
                command.targetType(),
                command.targetId(),
                command.severityLabel(),
                command.reasonTag(),
                escalatedBy,
                command.escalatedAt()));
        patientEventAuditService.recordEscalationCreated(actorMembership, saved.getId(), saved.getBranchId(), escalationMetadata(saved));
        return saved;
    }

    @Transactional
    public PatientEventEscalationRecord clearEscalation(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID escalationId,
            @NotNull OffsetDateTime clearedAt,
            UUID clearedByMembershipId) {
        requirePermission(actorMembership, AgencyPermission.ESCALATE_PATIENT_EVENTS, "clear patient-event escalations");
        PatientEventEscalationRecord escalation = resolveEscalation(actorMembership.getAgencyId(), escalationId);
        requireBranchAccess(actorMembership, AgencyPermission.ESCALATE_PATIENT_EVENTS, escalation.getBranchId(), "clear patient-event escalations");
        AgencyMembership clearedBy = resolveMembership(actorMembership.getAgencyId(), firstNonNull(clearedByMembershipId, actorMembership.getId()));
        escalation.clear(clearedBy, clearedAt);
        PatientEventEscalationRecord saved = patientEventEscalationRecordRepository.saveAndFlush(escalation);
        patientEventAuditService.recordEscalationCleared(actorMembership, saved.getId(), saved.getBranchId(), escalationMetadata(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<IncidentRecord> listIncidents(
            @NotNull AgencyMembership actorMembership,
            UUID patientId,
            UUID branchId,
            IncidentRecordStatus status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view incident records");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId, "view incident records");
        }
        if (patientId != null) {
            resolvePatient(actorMembership.getAgencyId(), patientId);
        }
        return incidentRecordRepository.findAll().stream()
                .filter(record -> Objects.equals(record.getAgencyId(), actorMembership.getAgencyId()))
                .filter(record -> patientId == null || Objects.equals(record.getPatientId(), patientId))
                .filter(record -> branchId == null || Objects.equals(record.getBranchId(), branchId))
                .filter(record -> status == null || record.getStatus() == status)
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .sorted(Comparator.comparing(IncidentRecord::getOccurredAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentRecord getIncident(@NotNull AgencyMembership actorMembership, @NotNull UUID incidentId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view incident records");
        IncidentRecord incident = resolveIncident(actorMembership.getAgencyId(), incidentId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, incident.getBranchId(), "view incident records");
        return incident;
    }

    @Transactional(readOnly = true)
    public List<InfectionRecord> listInfections(
            @NotNull AgencyMembership actorMembership,
            UUID patientId,
            UUID branchId,
            InfectionRecordStatus status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view infection records");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId, "view infection records");
        }
        if (patientId != null) {
            resolvePatient(actorMembership.getAgencyId(), patientId);
        }
        return infectionRecordRepository.findAll().stream()
                .filter(record -> Objects.equals(record.getAgencyId(), actorMembership.getAgencyId()))
                .filter(record -> patientId == null || Objects.equals(record.getPatientId(), patientId))
                .filter(record -> branchId == null || Objects.equals(record.getBranchId(), branchId))
                .filter(record -> status == null || record.getStatus() == status)
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .sorted(Comparator.comparing(InfectionRecord::getIdentifiedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public InfectionRecord getInfection(@NotNull AgencyMembership actorMembership, @NotNull UUID infectionId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view infection records");
        InfectionRecord infection = resolveInfection(actorMembership.getAgencyId(), infectionId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, infection.getBranchId(), "view infection records");
        return infection;
    }

    @Transactional(readOnly = true)
    public List<WoundRecord> listWounds(
            @NotNull AgencyMembership actorMembership,
            UUID patientId,
            UUID branchId,
            WoundRecordStatus status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view wound records");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId, "view wound records");
        }
        if (patientId != null) {
            resolvePatient(actorMembership.getAgencyId(), patientId);
        }
        return woundRecordRepository.findAll().stream()
                .filter(record -> Objects.equals(record.getAgencyId(), actorMembership.getAgencyId()))
                .filter(record -> patientId == null || Objects.equals(record.getPatientId(), patientId))
                .filter(record -> branchId == null || Objects.equals(record.getBranchId(), branchId))
                .filter(record -> status == null || record.getCurrentStatus() == status)
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .sorted(Comparator.comparing(WoundRecord::getIdentifiedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public WoundRecord getWound(@NotNull AgencyMembership actorMembership, @NotNull UUID woundId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view wound records");
        WoundRecord wound = resolveWound(actorMembership.getAgencyId(), woundId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, wound.getBranchId(), "view wound records");
        return wound;
    }

    @Transactional(readOnly = true)
    public List<WoundHistoryEntry> listWoundHistory(@NotNull AgencyMembership actorMembership, @NotNull UUID woundId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view wound history");
        WoundRecord wound = resolveWound(actorMembership.getAgencyId(), woundId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, wound.getBranchId(), "view wound history");
        return woundHistoryEntryRepository.findAllByWoundRecord_IdOrderByCapturedAtAsc(woundId);
    }

    @Transactional(readOnly = true)
    public List<PatientEventEvidenceLink> listEvidenceLinks(
            @NotNull AgencyMembership actorMembership,
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view patient-event evidence");
        TargetContext target = resolveTarget(actorMembership.getAgencyId(), targetType, targetId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, target.branchId(), "view patient-event evidence");
        return patientEventEvidenceLinkRepository.findAllByAgency_IdAndTargetTypeAndTargetIdOrderByLinkedAtAsc(
                actorMembership.getAgencyId(),
                targetType,
                targetId);
    }

    @Transactional
    public void unlinkEvidence(@NotNull AgencyMembership actorMembership, @NotNull UUID evidenceLinkId) {
        requirePermission(actorMembership, AgencyPermission.LINK_PATIENT_EVENT_EVIDENCE, "unlink patient-event evidence");
        PatientEventEvidenceLink evidenceLink = patientEventEvidenceLinkRepository.findByIdAndAgency_Id(evidenceLinkId, actorMembership.getAgencyId())
                .orElseThrow(() -> new PatientEventEntityNotFoundException("PatientEventEvidenceLink", evidenceLinkId));
        requireBranchAccess(actorMembership, AgencyPermission.LINK_PATIENT_EVENT_EVIDENCE, evidenceLink.getBranchId(), "unlink patient-event evidence");
        patientEventEvidenceLinkRepository.delete(evidenceLink);
        patientEventAuditService.recordEvidenceUnlinked(actorMembership, evidenceLinkId, evidenceLink.getBranchId(), evidenceMetadata(evidenceLink));
    }

    @Transactional(readOnly = true)
    public List<PatientEventFollowUpAssignment> listFollowUps(
            @NotNull AgencyMembership actorMembership,
            UUID patientId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventFollowUpStatus status,
            OffsetDateTime overdueAsOf) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view patient-event follow-up");
        if (patientId != null) {
            resolvePatient(actorMembership.getAgencyId(), patientId);
        }
        return patientEventFollowUpAssignmentRepository.findAll().stream()
                .filter(item -> Objects.equals(item.getAgencyId(), actorMembership.getAgencyId()))
                .filter(item -> patientId == null || Objects.equals(item.getPatientId(), patientId))
                .filter(item -> targetType == null || item.getTargetType() == targetType)
                .filter(item -> targetId == null || Objects.equals(item.getTargetId(), targetId))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> overdueAsOf == null || item.isOverdue(overdueAsOf))
                .filter(item -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, item.getBranchId()))
                .sorted(Comparator.comparing(PatientEventFollowUpAssignment::getDueAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientEventEscalationRecord> listEscalations(
            @NotNull AgencyMembership actorMembership,
            UUID patientId,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventEscalationStatus status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view patient-event escalations");
        if (patientId != null) {
            resolvePatient(actorMembership.getAgencyId(), patientId);
        }
        return patientEventEscalationRecordRepository.findAll().stream()
                .filter(item -> Objects.equals(item.getAgencyId(), actorMembership.getAgencyId()))
                .filter(item -> patientId == null || Objects.equals(item.getPatientId(), patientId))
                .filter(item -> targetType == null || item.getTargetType() == targetType)
                .filter(item -> targetId == null || Objects.equals(item.getTargetId(), targetId))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, item.getBranchId()))
                .sorted(Comparator.comparing(PatientEventEscalationRecord::getEscalatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientEventSummaryView getPatientEventSummary(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId) {
        List<IncidentRecord> incidents = listIncidents(actorMembership, patientId, null, null);
        List<InfectionRecord> infections = listInfections(actorMembership, patientId, null, null);
        List<WoundRecord> wounds = listWounds(actorMembership, patientId, null, null);
        List<PatientEventFollowUpAssignment> followUps = listFollowUps(actorMembership, patientId, null, null, null, null);
        List<PatientEventEscalationRecord> escalations = listEscalations(actorMembership, patientId, null, null, PatientEventEscalationStatus.ACTIVE);
        List<PatientEventAlertContract> alerts = projectPatientAlerts(actorMembership, patientId, OffsetDateTime.now());
        UUID branchId = incidents.stream().map(IncidentRecord::getBranchId).filter(Objects::nonNull).findFirst()
                .or(() -> infections.stream().map(InfectionRecord::getBranchId).filter(Objects::nonNull).findFirst())
                .or(() -> wounds.stream().map(WoundRecord::getBranchId).filter(Objects::nonNull).findFirst())
                .orElse(null);
        return new PatientEventSummaryView(
                patientId,
                branchId,
                incidents.stream().filter(item -> item.getStatus() == IncidentRecordStatus.OPEN || item.getStatus() == IncidentRecordStatus.IN_REVIEW).count(),
                infections.stream().filter(item -> item.getStatus() != InfectionRecordStatus.RESOLVED).count(),
                wounds.stream().filter(WoundRecord::isActive).count(),
                followUps.stream().filter(item -> item.getStatus() == PatientEventFollowUpStatus.OPEN).count(),
                escalations.size(),
                alerts);
    }

    @Transactional(readOnly = true)
    public List<LongitudinalHistoryEntryView> listPatientLongitudinalHistory(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view patient-event history");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), patientId);
        requirePatientWorkspaceAccess(actorMembership, patient.getId(), "view patient-event history");
        List<LongitudinalHistoryEntryView> entries = new ArrayList<>();

        incidentRecordRepository.findAllByAgency_IdAndPatient_IdOrderByOccurredAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .forEach(record -> entries.add(new LongitudinalHistoryEntryView(
                        PatientEventHistoryEntryType.INCIDENT_EVENT,
                        Epic12PatientEventTargetType.INCIDENT_RECORD,
                        record.getId(),
                        record.getOccurredAt(),
                        record.getBranchId(),
                        record.getStatus().name(),
                        record.getSeverityLabel(),
                        record.getSummary())));

        infectionRecordRepository.findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .forEach(record -> entries.add(new LongitudinalHistoryEntryView(
                        PatientEventHistoryEntryType.INFECTION_EVENT,
                        Epic12PatientEventTargetType.INFECTION_RECORD,
                        record.getId(),
                        record.getIdentifiedAt(),
                        record.getBranchId(),
                        record.getStatus().name(),
                        null,
                        record.getSummary())));

        woundRecordRepository.findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .forEach(record -> entries.add(new LongitudinalHistoryEntryView(
                        PatientEventHistoryEntryType.WOUND_CREATED,
                        Epic12PatientEventTargetType.WOUND_RECORD,
                        record.getId(),
                        record.getIdentifiedAt(),
                        record.getBranchId(),
                        record.getCurrentStatus().name(),
                        null,
                        record.getBaselineSummary() == null ? record.getWoundTypeOrSite() : record.getBaselineSummary())));

        woundHistoryEntryRepository.findAllByAgency_IdAndPatient_IdOrderByCapturedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(entry -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, entry.getBranchId()))
                .forEach(entry -> entries.add(new LongitudinalHistoryEntryView(
                        PatientEventHistoryEntryType.WOUND_HISTORY_CAPTURED,
                        Epic12PatientEventTargetType.WOUND_HISTORY_ENTRY,
                        entry.getId(),
                        entry.getCapturedAt(),
                        entry.getBranchId(),
                        entry.getProgressionMarker(),
                        null,
                        entry.getObservationSummary())));

        patientEventFollowUpAssignmentRepository.findAllByAgency_IdAndPatient_IdOrderByAssignedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .forEach(record -> {
                    entries.add(new LongitudinalHistoryEntryView(
                            PatientEventHistoryEntryType.FOLLOW_UP_MILESTONE,
                            Epic12PatientEventTargetType.FOLLOW_UP_ASSIGNMENT,
                            record.getId(),
                            record.getAssignedAt(),
                            record.getBranchId(),
                            record.getStatus().name(),
                            null,
                            "Follow-up assigned"));
                    if (record.getCompletionAt() != null) {
                        entries.add(new LongitudinalHistoryEntryView(
                                PatientEventHistoryEntryType.FOLLOW_UP_MILESTONE,
                                Epic12PatientEventTargetType.FOLLOW_UP_ASSIGNMENT,
                                record.getId(),
                                record.getCompletionAt(),
                                record.getBranchId(),
                                record.getStatus().name(),
                                null,
                                "Follow-up completed"));
                    }
                });

        patientEventEscalationRecordRepository.findAllByAgency_IdAndPatient_IdOrderByEscalatedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .forEach(record -> {
                    entries.add(new LongitudinalHistoryEntryView(
                            PatientEventHistoryEntryType.ESCALATION_MILESTONE,
                            Epic12PatientEventTargetType.ESCALATION_RECORD,
                            record.getId(),
                            record.getEscalatedAt(),
                            record.getBranchId(),
                            PatientEventEscalationStatus.ACTIVE.name(),
                            record.getSeverityLabel(),
                            record.getReasonTag() == null ? "Escalated" : record.getReasonTag()));
                    if (record.getClearedAt() != null) {
                        entries.add(new LongitudinalHistoryEntryView(
                                PatientEventHistoryEntryType.ESCALATION_MILESTONE,
                                Epic12PatientEventTargetType.ESCALATION_RECORD,
                                record.getId(),
                                record.getClearedAt(),
                                record.getBranchId(),
                                PatientEventEscalationStatus.CLEARED.name(),
                                record.getSeverityLabel(),
                                "Escalation cleared"));
                    }
                });

        patientEventEvidenceLinkRepository.findAllByAgency_IdAndPatient_IdOrderByLinkedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .forEach(record -> entries.add(new LongitudinalHistoryEntryView(
                        PatientEventHistoryEntryType.EVIDENCE_EVENT,
                        Epic12PatientEventTargetType.EVIDENCE_LINK,
                        record.getId(),
                        record.getLinkedAt(),
                        record.getBranchId(),
                        record.getSourceType().name(),
                        null,
                        "Evidence linked to " + record.getTargetType().name())));

        entries.sort(Comparator
                .comparing(LongitudinalHistoryEntryView::occurredAt)
                .thenComparing(LongitudinalHistoryEntryView::targetType)
                .thenComparing(LongitudinalHistoryEntryView::targetId));
        UUID branchIdForAudit = entries.stream()
                .map(LongitudinalHistoryEntryView::branchId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        patientEventAuditService.recordLongitudinalHistoryProjected(
                actorMembership,
                patient.getId(),
                branchIdForAudit,
                "{\"entryCount\":" + entries.size() + "}");
        return entries;
    }

    @Transactional(readOnly = true)
    public List<PatientEventAlertContract> projectPatientAlerts(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientId,
            @NotNull OffsetDateTime asOf) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, "view patient-event alerts");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), patientId);
        requirePatientWorkspaceAccess(actorMembership, patient.getId(), "view patient-event alerts");
        List<PatientEventAlertContract> alerts = new ArrayList<>();

        incidentRecordRepository.findAllByAgency_IdAndPatient_IdOrderByOccurredAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .filter(record -> "HIGH".equalsIgnoreCase(record.getSeverityLabel()) || "CRITICAL".equalsIgnoreCase(record.getSeverityLabel()))
                .forEach(record -> alerts.add(new PatientEventAlertContract(
                        patient.getId(),
                        record.getBranchId(),
                        PatientEventAlertType.HIGH_SEVERITY_INCIDENT,
                        record.getId(),
                        Epic12PatientEventTargetType.INCIDENT_RECORD.name(),
                        record.getSeverityLabel(),
                        record.getSummary())));

        patientEventFollowUpAssignmentRepository.findAllByAgency_IdAndStatusOrderByDueAtAsc(actorMembership.getAgencyId(), PatientEventFollowUpStatus.OPEN).stream()
                .filter(record -> Objects.equals(record.getPatientId(), patient.getId()))
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .filter(record -> record.isOverdue(asOf))
                .forEach(record -> alerts.add(new PatientEventAlertContract(
                        patient.getId(),
                        record.getBranchId(),
                        PatientEventAlertType.OVERDUE_FOLLOW_UP,
                        record.getId(),
                        Epic12PatientEventTargetType.FOLLOW_UP_ASSIGNMENT.name(),
                        null,
                        "Follow-up is overdue")));

        infectionRecordRepository.findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .filter(record -> record.getStatus() == InfectionRecordStatus.ACTIVE || record.getStatus() == InfectionRecordStatus.MONITORING)
                .forEach(record -> alerts.add(new PatientEventAlertContract(
                        patient.getId(),
                        record.getBranchId(),
                        PatientEventAlertType.ACTIVE_INFECTION,
                        record.getId(),
                        Epic12PatientEventTargetType.INFECTION_RECORD.name(),
                        record.getStatus().name(),
                        record.getSummary())));

        woundHistoryEntryRepository.findAllByAgency_IdAndPatient_IdOrderByCapturedAtAsc(actorMembership.getAgencyId(), patient.getId()).stream()
                .filter(record -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, record.getBranchId()))
                .filter(record -> "DETERIORATING".equalsIgnoreCase(record.getProgressionMarker()))
                .forEach(record -> alerts.add(new PatientEventAlertContract(
                        patient.getId(),
                        record.getBranchId(),
                        PatientEventAlertType.WOUND_PROGRESSION_ALERT,
                        record.getId(),
                        Epic12PatientEventTargetType.WOUND_HISTORY_ENTRY.name(),
                        record.getProgressionMarker(),
                        record.getObservationSummary())));
        return alerts;
    }

    private IncidentRecord resolveIncident(UUID agencyId, UUID incidentId) {
        return incidentRecordRepository.findByIdAndAgency_Id(incidentId, agencyId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("IncidentRecord", incidentId));
    }

    private InfectionRecord resolveInfection(UUID agencyId, UUID infectionId) {
        return infectionRecordRepository.findByIdAndAgency_Id(infectionId, agencyId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("InfectionRecord", infectionId));
    }

    private WoundRecord resolveWound(UUID agencyId, UUID woundId) {
        return woundRecordRepository.findByIdAndAgency_Id(woundId, agencyId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("WoundRecord", woundId));
    }

    private PatientEventFollowUpAssignment resolveFollowUp(UUID agencyId, UUID followUpId) {
        return patientEventFollowUpAssignmentRepository.findByIdAndAgency_Id(followUpId, agencyId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("PatientEventFollowUpAssignment", followUpId));
    }

    private PatientEventEscalationRecord resolveEscalation(UUID agencyId, UUID escalationId) {
        return patientEventEscalationRecordRepository.findByIdAndAgency_Id(escalationId, agencyId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("PatientEventEscalationRecord", escalationId));
    }

    private IncidentRecord resolveRelatedIncident(UUID agencyId, UUID incidentId, UUID patientId) {
        if (incidentId == null) {
            return null;
        }
        IncidentRecord incident = resolveIncident(agencyId, incidentId);
        if (!Objects.equals(incident.getPatientId(), patientId)) {
            throw new PatientEventConflictException("Related incident must belong to the same patient.");
        }
        return incident;
    }

    private Patient resolvePatient(UUID agencyId, UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("Patient", patientId));
        if (!Objects.equals(patient.getAgencyId(), agencyId)) {
            throw new PatientEventEntityNotFoundException("Patient", patientId);
        }
        return patient;
    }

    private VisitOccurrence resolveVisitOccurrence(UUID agencyId, UUID visitOccurrenceId, UUID patientId) {
        if (visitOccurrenceId == null) {
            return null;
        }
        VisitOccurrence visitOccurrence = visitOccurrenceRepository.findById(visitOccurrenceId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
        if (!Objects.equals(visitOccurrence.getAgencyId(), agencyId)) {
            throw new PatientEventEntityNotFoundException("VisitOccurrence", visitOccurrenceId);
        }
        if (!Objects.equals(visitOccurrence.getPatient().getId(), patientId)) {
            throw new PatientEventConflictException("Visit occurrence must belong to the same patient.");
        }
        return visitOccurrence;
    }

    private AgencyMembership resolveMembership(UUID agencyId, UUID membershipId) {
        if (membershipId == null) {
            return null;
        }
        AgencyMembership membership = agencyMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("AgencyMembership", membershipId));
        if (!Objects.equals(membership.getAgencyId(), agencyId)) {
            throw new PatientEventEntityNotFoundException("AgencyMembership", membershipId);
        }
        return membership;
    }

    private Branch resolveAccessibleBranch(
            AgencyMembership actorMembership,
            AgencyPermission permission,
            UUID branchId,
            String action) {
        if (branchId == null) {
            if (!permission.isAgencyWideFor(actorMembership.getRole())) {
                throw new PatientEventConflictException("A branch is required for branch-scoped patient-event workflows.");
            }
            return null;
        }
        requireBranchAccess(actorMembership, permission, branchId, action);
        return resolveBranch(actorMembership.getAgencyId(), branchId);
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new PatientEventEntityNotFoundException("Branch", branchId));
        if (!Objects.equals(branch.getAgencyId(), agencyId)) {
            throw new PatientEventEntityNotFoundException("Branch", branchId);
        }
        return branch;
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission, String action) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                permission,
                membershipId -> new UnauthorizedPatientEventActorException("Membership %s cannot %s.".formatted(membershipId, action)));
    }

    private void requireBranchAccess(
            AgencyMembership actorMembership,
            AgencyPermission permission,
            UUID branchId,
            String action) {
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
            return;
        }
        if (!branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                actorMembership.getId(),
                branchId,
                BranchAssignmentStatus.ACTIVE)) {
            throw new UnauthorizedPatientEventActorException("Membership %s cannot %s for branch %s.".formatted(
                    actorMembership.getId(),
                    action,
                    branchId));
        }
    }

    private boolean hasBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId) {
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
            return true;
        }
        return branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                actorMembership.getId(),
                branchId,
                BranchAssignmentStatus.ACTIVE);
    }

    private void requirePatientWorkspaceAccess(AgencyMembership actorMembership, UUID patientId, String action) {
        if (AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE.isAgencyWideFor(actorMembership.getRole())) {
            return;
        }
        boolean hasVisibleBranch = incidentRecordRepository.findAllByAgency_IdAndPatient_IdOrderByOccurredAtAsc(actorMembership.getAgencyId(), patientId).stream()
                .map(IncidentRecord::getBranchId)
                .filter(Objects::nonNull)
                .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        hasVisibleBranch = hasVisibleBranch
                || infectionRecordRepository.findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(actorMembership.getAgencyId(), patientId).stream()
                        .map(InfectionRecord::getBranchId)
                        .filter(Objects::nonNull)
                        .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        hasVisibleBranch = hasVisibleBranch
                || woundRecordRepository.findAllByAgency_IdAndPatient_IdOrderByIdentifiedAtAsc(actorMembership.getAgencyId(), patientId).stream()
                        .map(WoundRecord::getBranchId)
                        .filter(Objects::nonNull)
                        .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        hasVisibleBranch = hasVisibleBranch
                || woundHistoryEntryRepository.findAllByAgency_IdAndPatient_IdOrderByCapturedAtAsc(actorMembership.getAgencyId(), patientId).stream()
                        .map(WoundHistoryEntry::getBranchId)
                        .filter(Objects::nonNull)
                        .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        hasVisibleBranch = hasVisibleBranch
                || patientEventFollowUpAssignmentRepository.findAllByAgency_IdAndPatient_IdOrderByAssignedAtAsc(actorMembership.getAgencyId(), patientId).stream()
                        .map(PatientEventFollowUpAssignment::getBranchId)
                        .filter(Objects::nonNull)
                        .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        hasVisibleBranch = hasVisibleBranch
                || patientEventEscalationRecordRepository.findAllByAgency_IdAndPatient_IdOrderByEscalatedAtAsc(actorMembership.getAgencyId(), patientId).stream()
                        .map(PatientEventEscalationRecord::getBranchId)
                        .filter(Objects::nonNull)
                        .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        hasVisibleBranch = hasVisibleBranch
                || patientEventEvidenceLinkRepository.findAllByAgency_IdAndPatient_IdOrderByLinkedAtAsc(actorMembership.getAgencyId(), patientId).stream()
                        .map(PatientEventEvidenceLink::getBranchId)
                        .filter(Objects::nonNull)
                        .anyMatch(branchId -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE, branchId));
        if (!hasVisibleBranch) {
            throw new UnauthorizedPatientEventActorException("Membership %s cannot %s for patient %s.".formatted(
                    actorMembership.getId(),
                    action,
                    patientId));
        }
    }

    private TargetContext resolveTarget(UUID agencyId, Epic12PatientEventTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case INCIDENT_RECORD -> {
                IncidentRecord record = resolveIncident(agencyId, targetId);
                yield new TargetContext(record.getPatientId(), record.getBranchId());
            }
            case INFECTION_RECORD -> {
                InfectionRecord record = resolveInfection(agencyId, targetId);
                yield new TargetContext(record.getPatientId(), record.getBranchId());
            }
            case WOUND_RECORD -> {
                WoundRecord record = resolveWound(agencyId, targetId);
                yield new TargetContext(record.getPatientId(), record.getBranchId());
            }
            case WOUND_HISTORY_ENTRY -> {
                WoundHistoryEntry entry = woundHistoryEntryRepository.findByIdAndAgency_Id(targetId, agencyId)
                        .orElseThrow(() -> new PatientEventEntityNotFoundException("WoundHistoryEntry", targetId));
                yield new TargetContext(entry.getPatientId(), entry.getBranchId());
            }
            default -> throw new PatientEventConflictException("Unsupported evidence target type " + targetType + ".");
        };
    }

    private static UUID firstNonNull(UUID preferred, UUID fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static String incidentMetadata(IncidentRecord record) {
        return "{\"incidentType\":\"" + record.getIncidentType() + "\",\"status\":\"" + record.getStatus().name() + "\"}";
    }

    private static String infectionMetadata(InfectionRecord record) {
        return "{\"infectionType\":\"" + record.getInfectionType() + "\",\"status\":\"" + record.getStatus().name() + "\"}";
    }

    private static String woundMetadata(WoundRecord record) {
        return "{\"woundTypeOrSite\":\"" + record.getWoundTypeOrSite() + "\",\"status\":\"" + record.getCurrentStatus().name() + "\"}";
    }

    private static String woundHistoryMetadata(WoundHistoryEntry entry) {
        return "{\"progressionMarker\":\"" + (entry.getProgressionMarker() == null ? "" : entry.getProgressionMarker()) + "\"}";
    }

    private static String evidenceMetadata(PatientEventEvidenceLink link) {
        return "{\"targetType\":\"" + link.getTargetType().name() + "\",\"sourceType\":\"" + link.getSourceType().name() + "\"}";
    }

    private static String followUpMetadata(PatientEventFollowUpAssignment followUp) {
        return "{\"targetType\":\"" + followUp.getTargetType().name() + "\",\"status\":\"" + followUp.getStatus().name() + "\"}";
    }

    private static String escalationMetadata(PatientEventEscalationRecord escalation) {
        return "{\"targetType\":\"" + escalation.getTargetType().name() + "\",\"status\":\"" + escalation.getStatus().name() + "\"}";
    }

    private record TargetContext(UUID patientId, UUID branchId) {
    }

    public record CreateIncidentCommand(
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

    public record UpdateIncidentCommand(
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

    public record CreateInfectionCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID relatedIncidentId,
            LocalDate onsetDate,
            @NotNull OffsetDateTime identifiedAt,
            @NotBlank String infectionType,
            @NotBlank String summary,
            InfectionRecordStatus status) {
    }

    public record UpdateInfectionCommand(
            UUID branchId,
            UUID relatedIncidentId,
            LocalDate onsetDate,
            @NotNull OffsetDateTime identifiedAt,
            @NotBlank String infectionType,
            @NotBlank String summary,
            @NotNull InfectionRecordStatus status) {
    }

    public record CreateWoundCommand(
            @NotNull UUID patientId,
            UUID branchId,
            @NotNull OffsetDateTime identifiedAt,
            @NotBlank String woundTypeOrSite,
            WoundRecordStatus currentStatus,
            String baselineSummary) {
    }

    public record UpdateWoundCommand(
            UUID branchId,
            @NotBlank String woundTypeOrSite,
            @NotNull WoundRecordStatus currentStatus,
            String baselineSummary) {
    }

    public record AddWoundHistoryCommand(
            UUID branchId,
            @NotNull OffsetDateTime capturedAt,
            @NotBlank String observationSummary,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal depthCm,
            String progressionMarker,
            UUID capturedByMembershipId) {
    }

    public record LinkEvidenceCommand(
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId,
            UUID branchId,
            UUID patientAttachmentId,
            UUID mobileArtifactId,
            UUID documentationAttachmentLinkId,
            UUID linkedByMembershipId,
            @NotNull OffsetDateTime linkedAt) {
    }

    public record AssignFollowUpCommand(
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId,
            UUID branchId,
            UUID ownerMembershipId,
            AgencyRole ownerRole,
            @NotNull OffsetDateTime assignedAt,
            @NotNull OffsetDateTime dueAt,
            String followUpNote) {
    }

    public record UpdateFollowUpCommand(
            UUID branchId,
            UUID ownerMembershipId,
            AgencyRole ownerRole,
            @NotNull OffsetDateTime dueAt,
            String followUpNote) {
    }

    public record CreateEscalationCommand(
            @NotNull Epic12PatientEventTargetType targetType,
            @NotNull UUID targetId,
            UUID branchId,
            String severityLabel,
            String reasonTag,
            UUID escalatedByMembershipId,
            @NotNull OffsetDateTime escalatedAt) {
    }

    public record LongitudinalHistoryEntryView(
            PatientEventHistoryEntryType historyEntryType,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            OffsetDateTime occurredAt,
            UUID branchId,
            String status,
            String severity,
            String summary) {
    }

    public record PatientEventSummaryView(
            UUID patientId,
            UUID branchId,
            long openIncidentCount,
            long activeInfectionCount,
            long activeWoundCount,
            long openFollowUpCount,
            long activeEscalationCount,
            List<PatientEventAlertContract> alerts) {
    }
}
