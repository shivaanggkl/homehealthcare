package com.homehealthcare.compliance.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.compliance.domain.CertificationPeriodRecord;
import com.homehealthcare.compliance.domain.CertificationPeriodRecordRepository;
import com.homehealthcare.compliance.domain.ComplianceChecklistDefinition;
import com.homehealthcare.compliance.domain.ComplianceChecklistDefinitionRepository;
import com.homehealthcare.compliance.domain.ComplianceChecklistResult;
import com.homehealthcare.compliance.domain.ComplianceChecklistResultRepository;
import com.homehealthcare.compliance.domain.ComplianceStatusProjection;
import com.homehealthcare.compliance.domain.ComplianceStatusProjectionRepository;
import com.homehealthcare.compliance.domain.ConsentAcknowledgmentRecord;
import com.homehealthcare.compliance.domain.ConsentAcknowledgmentRecordRepository;
import com.homehealthcare.compliance.domain.PatientRiskReminder;
import com.homehealthcare.compliance.domain.PatientRiskReminderRepository;
import com.homehealthcare.compliance.domain.RequiredDocumentationRequirement;
import com.homehealthcare.compliance.domain.RequiredDocumentationRequirementRepository;
import com.homehealthcare.compliance.foundation.CertificationPeriodStatus;
import com.homehealthcare.compliance.foundation.ComplianceAuditService;
import com.homehealthcare.compliance.foundation.ComplianceChecklistResultStatus;
import com.homehealthcare.compliance.foundation.ComplianceEvaluationCategory;
import com.homehealthcare.compliance.foundation.ComplianceReadinessStatus;
import com.homehealthcare.compliance.foundation.ConsentAcknowledgmentStatus;
import com.homehealthcare.compliance.foundation.PatientRiskReminderStatus;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLinkRepository;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.domain.VisitDocumentationRecordRepository;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.SignatureVerificationLinkRepository;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class ComplianceWorkspaceService {

    private static final Set<DocumentationRecordStatus> SATISFYING_DOCUMENTATION_STATUSES = Set.of(
            DocumentationRecordStatus.SUBMITTED,
            DocumentationRecordStatus.AMENDED,
            DocumentationRecordStatus.LOCKED);

    private final ComplianceChecklistDefinitionRepository checklistDefinitionRepository;
    private final RequiredDocumentationRequirementRepository documentationRequirementRepository;
    private final ComplianceChecklistResultRepository complianceChecklistResultRepository;
    private final ConsentAcknowledgmentRecordRepository consentAcknowledgmentRecordRepository;
    private final CertificationPeriodRecordRepository certificationPeriodRecordRepository;
    private final PatientRiskReminderRepository patientRiskReminderRepository;
    private final ComplianceStatusProjectionRepository complianceStatusProjectionRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final VisitDocumentationRecordRepository visitDocumentationRecordRepository;
    private final DocumentationAttachmentLinkRepository documentationAttachmentLinkRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final SignatureVerificationLinkRepository signatureVerificationLinkRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final PatientPayerLinkRepository patientPayerLinkRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ComplianceAuditService complianceAuditService;
    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public List<ComplianceChecklistDefinition> listChecklistDefinitions(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            UUID serviceLineId,
            Boolean active) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view compliance checklist definitions");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view compliance checklist definitions");
        }
        return checklistDefinitionRepository.findAllByAgency_IdOrderByItemCodeAsc(actorMembership.getAgencyId()).stream()
                .filter(item -> branchId == null || Objects.equals(branchId, item.getBranchId()))
                .filter(item -> serviceLineId == null || Objects.equals(serviceLineId, item.getServiceLineId()))
                .filter(item -> active == null || item.isActive() == active)
                .toList();
    }

    @Transactional
    public ComplianceChecklistDefinition updateChecklistDefinition(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID checklistDefinitionId,
            @Valid CreateChecklistDefinitionCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS, "manage compliance checklist definitions");
        ComplianceChecklistDefinition definition = checklistDefinitionRepository.findByIdAndAgency_Id(checklistDefinitionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("ComplianceChecklistDefinition", checklistDefinitionId));
        Branch branch = resolveManagedBranch(actorMembership, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS, command.branchId(), "manage compliance checklist definitions");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        definition.updateDetails(branch, serviceLine, command.description(), command.severityLabel(), command.weightScore(), command.active());
        ComplianceChecklistDefinition saved = checklistDefinitionRepository.saveAndFlush(definition);
        complianceAuditService.recordChecklistDefinitionSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"itemCode\":\"" + saved.getItemCode() + "\",\"active\":" + saved.isActive() + "}");
        return saved;
    }

    @Transactional
    public ComplianceChecklistDefinition deactivateChecklistDefinition(@NotNull AgencyMembership actorMembership, @NotNull UUID checklistDefinitionId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS, "manage compliance checklist definitions");
        ComplianceChecklistDefinition definition = checklistDefinitionRepository.findByIdAndAgency_Id(checklistDefinitionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("ComplianceChecklistDefinition", checklistDefinitionId));
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS, definition.getBranchId(), "manage compliance checklist definitions");
        definition.deactivate();
        ComplianceChecklistDefinition saved = checklistDefinitionRepository.saveAndFlush(definition);
        complianceAuditService.recordChecklistDefinitionSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"itemCode\":\"" + saved.getItemCode() + "\",\"active\":false}");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentationRequirement> listDocumentationRequirements(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            UUID serviceLineId,
            Boolean active) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view required documentation rules");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view required documentation rules");
        }
        return documentationRequirementRepository.findAllByAgency_IdOrderByRequirementCodeAsc(actorMembership.getAgencyId()).stream()
                .filter(item -> branchId == null || Objects.equals(branchId, item.getBranchId()))
                .filter(item -> serviceLineId == null || Objects.equals(serviceLineId, item.getServiceLineId()))
                .filter(item -> active == null || item.isActive() == active)
                .toList();
    }

    @Transactional
    public RequiredDocumentationRequirement updateDocumentationRequirement(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRequirementId,
            @Valid CreateDocumentationRequirementCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES, "manage required documentation rules");
        RequiredDocumentationRequirement requirement = documentationRequirementRepository.findByIdAndAgency_Id(documentationRequirementId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("RequiredDocumentationRequirement", documentationRequirementId));
        Branch branch = resolveManagedBranch(actorMembership, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES, command.branchId(), "manage required documentation rules");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        requirement.updateDetails(
                branch,
                serviceLine,
                command.sourceCategory(),
                command.description(),
                command.patientApplicable(),
                command.episodeApplicable(),
                command.dueDays(),
                command.recencyDays(),
                command.requiresSignatureVerification(),
                command.requiresAttachmentEvidence(),
                command.active());
        RequiredDocumentationRequirement saved = documentationRequirementRepository.saveAndFlush(requirement);
        complianceAuditService.recordDocumentationRequirementSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"requirementCode\":\"" + saved.getRequirementCode() + "\",\"active\":" + saved.isActive() + "}");
        return saved;
    }

    @Transactional
    public RequiredDocumentationRequirement deactivateDocumentationRequirement(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRequirementId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES, "manage required documentation rules");
        RequiredDocumentationRequirement requirement = documentationRequirementRepository.findByIdAndAgency_Id(documentationRequirementId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("RequiredDocumentationRequirement", documentationRequirementId));
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES, requirement.getBranchId(), "manage required documentation rules");
        requirement.updateDetails(
                requirement.getBranchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), requirement.getBranchId()),
                requirement.getServiceLineId() == null ? null : resolveServiceLine(actorMembership.getAgencyId(), requirement.getServiceLineId()),
                requirement.getSourceCategory(),
                requirement.getDescription(),
                requirement.isPatientApplicable(),
                requirement.isEpisodeApplicable(),
                requirement.getDueDays(),
                requirement.getRecencyDays(),
                requirement.isRequiresSignatureVerification(),
                requirement.isRequiresAttachmentEvidence(),
                false);
        RequiredDocumentationRequirement saved = documentationRequirementRepository.saveAndFlush(requirement);
        complianceAuditService.recordDocumentationRequirementSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"requirementCode\":\"" + saved.getRequirementCode() + "\",\"active\":false}");
        return saved;
    }

    @Transactional
    public ComplianceChecklistDefinition createChecklistDefinition(
            @NotNull AgencyMembership actorMembership,
            @Valid CreateChecklistDefinitionCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS, "manage compliance checklist definitions");
        Branch branch = resolveManagedBranch(actorMembership, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS, command.branchId(), "manage compliance checklist definitions");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        if (checklistDefinitionRepository.existsByAgency_IdAndItemCode(actorMembership.getAgencyId(), command.itemCode().trim().toUpperCase())) {
            throw new ComplianceConflictException("A compliance checklist definition already exists for code " + command.itemCode() + ".");
        }
        ComplianceChecklistDefinition definition = checklistDefinitionRepository.saveAndFlush(ComplianceChecklistDefinition.create(
                actorMembership.getAgency(),
                branch,
                serviceLine,
                command.itemCode(),
                command.description(),
                command.severityLabel(),
                command.weightScore(),
                command.active()));
        complianceAuditService.recordChecklistDefinitionSaved(
                actorMembership,
                definition.getId(),
                definition.getBranchId(),
                "{\"itemCode\":\"" + definition.getItemCode() + "\",\"active\":" + definition.isActive() + "}");
        return definition;
    }

    @Transactional
    public RequiredDocumentationRequirement createDocumentationRequirement(
            @NotNull AgencyMembership actorMembership,
            @Valid CreateDocumentationRequirementCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES, "manage required documentation rules");
        Branch branch = resolveManagedBranch(actorMembership, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES, command.branchId(), "manage required documentation rules");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        if (documentationRequirementRepository.existsByAgency_IdAndRequirementCode(
                actorMembership.getAgencyId(),
                command.requirementCode().trim().toUpperCase())) {
            throw new ComplianceConflictException("A required documentation rule already exists for code " + command.requirementCode() + ".");
        }
        RequiredDocumentationRequirement requirement = documentationRequirementRepository.saveAndFlush(RequiredDocumentationRequirement.create(
                actorMembership.getAgency(),
                branch,
                serviceLine,
                command.sourceCategory(),
                command.requirementCode(),
                command.description(),
                command.patientApplicable(),
                command.episodeApplicable(),
                command.dueDays(),
                command.recencyDays(),
                command.requiresSignatureVerification(),
                command.requiresAttachmentEvidence(),
                command.active()));
        complianceAuditService.recordDocumentationRequirementSaved(
                actorMembership,
                requirement.getId(),
                requirement.getBranchId(),
                "{\"requirementCode\":\"" + requirement.getRequirementCode() + "\",\"active\":" + requirement.isActive() + "}");
        return requirement;
    }

    @Transactional
    public ComplianceChecklistResult recalculateChecklistResult(
            @NotNull AgencyMembership actorMembership,
            @Valid RecalculateChecklistResultCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS, "recalculate compliance checklist results");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolvePatientScopeBranch(actorMembership, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS, command.branchId(), "recalculate compliance checklist results");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        ComplianceChecklistDefinition definition = checklistDefinitionRepository.findByIdAndAgency_Id(command.checklistDefinitionId(), actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("ComplianceChecklistDefinition", command.checklistDefinitionId()));
        if (definition.getBranchId() != null && !Objects.equals(definition.getBranchId(), command.branchId())) {
            throw new ComplianceConflictException("Checklist definition branch scope does not match the requested compliance context.");
        }
        if (definition.getServiceLineId() != null && !Objects.equals(definition.getServiceLineId(), command.serviceLineId())) {
            throw new ComplianceConflictException("Checklist definition service-line scope does not match the requested compliance context.");
        }
        ComplianceChecklistResult result = complianceChecklistResultRepository.saveAndFlush(ComplianceChecklistResult.createChecklistEvaluation(
                patient,
                branch,
                serviceLine,
                definition,
                command.resultStatus(),
                command.evidenceSourceType(),
                command.evidenceSourceId(),
                command.evidenceSummary(),
                command.evaluationOrigin(),
                command.contextPeriodStart(),
                command.contextPeriodEnd(),
                evaluatedAt(command.evaluatedAt())));
        complianceAuditService.recordChecklistResultRecalculated(
                actorMembership,
                result.getId(),
                result.getBranchId(),
                "{\"category\":\"" + result.getEvaluationCategory().name() + "\",\"status\":\"" + result.getResultStatus().name() + "\"}");
        return result;
    }

    @Transactional
    public ComplianceChecklistResult evaluateDocumentationRequirement(
            @NotNull AgencyMembership actorMembership,
            @Valid EvaluateDocumentationRequirementCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS, "evaluate required documentation compliance");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolvePatientScopeBranch(actorMembership, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS, command.branchId(), "evaluate required documentation compliance");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        RequiredDocumentationRequirement requirement = documentationRequirementRepository.findByIdAndAgency_Id(command.requirementId(), actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("RequiredDocumentationRequirement", command.requirementId()));
        if (requirement.getBranchId() != null && !Objects.equals(requirement.getBranchId(), command.branchId())) {
            throw new ComplianceConflictException("Documentation requirement branch scope does not match the requested compliance context.");
        }
        if (requirement.getServiceLineId() != null && !Objects.equals(requirement.getServiceLineId(), command.serviceLineId())) {
            throw new ComplianceConflictException("Documentation requirement service-line scope does not match the requested compliance context.");
        }

        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());
        VisitDocumentationRecord candidate = resolveDocumentationCandidate(patient, branch, serviceLine, requirement);
        ComplianceChecklistResultStatus status;
        String missingReason = null;
        String evidenceSummary;
        String satisfiedByRecordType = null;
        UUID satisfiedByRecordId = null;
        if (candidate == null) {
            if (requirement.getDueDays() != null && command.referenceAt() != null
                    && evaluatedAt.isBefore(command.referenceAt().plusDays(requirement.getDueDays()))) {
                status = ComplianceChecklistResultStatus.WARNING;
                missingReason = "DOCUMENTATION_DUE_SOON";
            } else {
                status = ComplianceChecklistResultStatus.FAIL;
                missingReason = "NO_SATISFYING_DOCUMENTATION";
            }
            evidenceSummary = "No submitted documentation record satisfied the requirement.";
        } else {
            OffsetDateTime evidenceTimestamp = candidate.getSubmittedAt() == null ? candidate.getLastSavedAt() : candidate.getSubmittedAt();
            status = ComplianceChecklistResultStatus.PASS;
            evidenceSummary = "Documentation record " + candidate.getId() + " satisfied the requirement.";
            satisfiedByRecordType = "VISIT_DOCUMENTATION_RECORD";
            satisfiedByRecordId = candidate.getId();
            if (requirement.getRecencyDays() != null && evidenceTimestamp.isBefore(evaluatedAt.minusDays(requirement.getRecencyDays()))) {
                status = ComplianceChecklistResultStatus.WARNING;
                missingReason = "DOCUMENTATION_STALE";
                evidenceSummary = "Documentation was found but falls outside the recency policy.";
            } else if (requirement.getDueDays() != null && command.referenceAt() != null
                    && evidenceTimestamp.isAfter(command.referenceAt().plusDays(requirement.getDueDays()))) {
                status = ComplianceChecklistResultStatus.WARNING;
                missingReason = "DOCUMENTATION_PAST_DUE";
                evidenceSummary = "Documentation was submitted after the due policy window.";
            } else if (requirement.isRequiresAttachmentEvidence()
                    && documentationAttachmentLinkRepository.findAllByDocumentationRecord_IdOrderByLinkedAtAsc(candidate.getId()).isEmpty()) {
                status = ComplianceChecklistResultStatus.FAIL;
                missingReason = "ATTACHMENT_EVIDENCE_MISSING";
                evidenceSummary = "Documentation exists but attachment evidence is missing.";
            } else if (requirement.isRequiresSignatureVerification() && !hasPresentSignature(candidate)) {
                status = ComplianceChecklistResultStatus.FAIL;
                missingReason = "SIGNATURE_VERIFICATION_MISSING";
                evidenceSummary = "Documentation exists but required signature verification is missing.";
            }
        }

        ComplianceChecklistResult result = complianceChecklistResultRepository.saveAndFlush(ComplianceChecklistResult.createDocumentationEvaluation(
                patient,
                branch,
                serviceLine,
                requirement,
                status,
                candidate == null ? null : "VISIT_DOCUMENTATION_RECORD",
                candidate == null ? null : candidate.getId(),
                evidenceSummary,
                command.evaluationOrigin(),
                command.contextPeriodStart(),
                command.contextPeriodEnd(),
                satisfiedByRecordType,
                satisfiedByRecordId,
                missingReason,
                evaluatedAt));
        complianceAuditService.recordChecklistResultRecalculated(
                actorMembership,
                result.getId(),
                result.getBranchId(),
                "{\"category\":\"" + result.getEvaluationCategory().name() + "\",\"status\":\"" + result.getResultStatus().name() + "\"}");
        return result;
    }

    @Transactional
    public ConsentAcknowledgmentRecord recordAcknowledgment(
            @NotNull AgencyMembership actorMembership,
            @Valid RecordConsentAcknowledgmentCommand command) {
        return saveAcknowledgment(actorMembership, null, command);
    }

    @Transactional
    public ConsentAcknowledgmentRecord saveAcknowledgment(
            @NotNull AgencyMembership actorMembership,
            UUID acknowledgmentRecordId,
            @Valid RecordConsentAcknowledgmentCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_ACKNOWLEDGMENTS, "manage consent acknowledgments");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolvePatientScopeBranch(actorMembership, AgencyPermission.MANAGE_PATIENT_ACKNOWLEDGMENTS, command.branchId(), "manage consent acknowledgments");
        AgencyMembership capturedBy = resolveMembership(actorMembership.getAgencyId(), command.capturedByMembershipId());
        ConsentAcknowledgmentRecord record = acknowledgmentRecordId == null
                ? ConsentAcknowledgmentRecord.record(
                        patient,
                        branch,
                        command.acknowledgmentType(),
                        command.effectiveAt(),
                        command.expiresAt(),
                        capturedBy,
                        command.captureMethod(),
                        command.supportingArtifactType(),
                        command.supportingArtifactId())
                : consentAcknowledgmentRecordRepository.findByIdAndAgency_Id(acknowledgmentRecordId, actorMembership.getAgencyId())
                        .map(existing -> {
                            existing.updateDetails(
                                    branch,
                                    command.acknowledgmentType(),
                                    command.effectiveAt(),
                                    command.expiresAt(),
                                    capturedBy,
                                    command.captureMethod(),
                                    command.supportingArtifactType(),
                                    command.supportingArtifactId());
                            return existing;
                        })
                        .orElseThrow(() -> new ComplianceEntityNotFoundException("ConsentAcknowledgmentRecord", acknowledgmentRecordId));
        record = consentAcknowledgmentRecordRepository.saveAndFlush(record);
        complianceAuditService.recordAcknowledgmentRecorded(
                actorMembership,
                record.getId(),
                record.getBranchId(),
                "{\"acknowledgmentType\":\"" + record.getAcknowledgmentType() + "\"}");
        return record;
    }

    @Transactional
    public ConsentAcknowledgmentRecord revokeAcknowledgment(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID acknowledgmentRecordId,
            OffsetDateTime revokedAt) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_ACKNOWLEDGMENTS, "revoke consent acknowledgments");
        ConsentAcknowledgmentRecord record = consentAcknowledgmentRecordRepository.findByIdAndAgency_Id(acknowledgmentRecordId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("ConsentAcknowledgmentRecord", acknowledgmentRecordId));
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_PATIENT_ACKNOWLEDGMENTS, record.getBranchId(), "revoke consent acknowledgments");
        record.revoke(evaluatedAt(revokedAt));
        ConsentAcknowledgmentRecord saved = consentAcknowledgmentRecordRepository.saveAndFlush(record);
        complianceAuditService.recordAcknowledgmentRevoked(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"acknowledgmentType\":\"" + saved.getAcknowledgmentType() + "\"}");
        return saved;
    }

    @Transactional
    public CertificationPeriodRecord saveCertificationPeriod(
            @NotNull AgencyMembership actorMembership,
            @Valid SaveCertificationPeriodCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CERTIFICATION_PERIODS, "manage certification periods");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolvePatientScopeBranch(actorMembership, AgencyPermission.MANAGE_CERTIFICATION_PERIODS, command.branchId(), "manage certification periods");
        PatientPayerLink payerLink = resolvePatientPayerLink(actorMembership.getAgencyId(), patient.getId(), command.patientPayerLinkId());
        CertificationPeriodRecord record = command.certificationPeriodId() == null
                ? CertificationPeriodRecord.create(
                        patient,
                        branch,
                        payerLink,
                        command.programContext(),
                        command.startDate(),
                        command.endDate(),
                        command.closed(),
                        command.source())
                : certificationPeriodRecordRepository.findByIdAndAgency_Id(command.certificationPeriodId(), actorMembership.getAgencyId())
                        .map(existing -> {
                            requireBranchAccess(actorMembership, AgencyPermission.MANAGE_CERTIFICATION_PERIODS, existing.getBranchId(), "manage certification periods");
                            existing.updateDetails(
                                    branch,
                                    payerLink,
                                    command.programContext(),
                                    command.startDate(),
                                    command.endDate(),
                                    command.closed(),
                                    command.source());
                            return existing;
                        })
                        .orElseThrow(() -> new ComplianceEntityNotFoundException("CertificationPeriodRecord", command.certificationPeriodId()));
        CertificationPeriodRecord saved = certificationPeriodRecordRepository.saveAndFlush(record);
        complianceAuditService.recordCertificationPeriodSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"recordState\":\"" + saved.getRecordState().name() + "\",\"endDate\":\"" + saved.getEndDate() + "\"}");
        return saved;
    }

    @Transactional
    public PatientRiskReminder saveRiskReminder(
            @NotNull AgencyMembership actorMembership,
            @Valid SaveRiskReminderCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS, "manage patient risk reminders");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolvePatientScopeBranch(actorMembership, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS, command.branchId(), "manage patient risk reminders");
        VisitOccurrence visitOccurrence = resolveVisitOccurrence(actorMembership.getAgencyId(), patient.getId(), command.visitOccurrenceId());
        VisitDocumentationRecord documentationRecord = resolveDocumentationRecord(actorMembership.getAgencyId(), patient.getId(), command.documentationRecordId());
        PatientRiskReminder reminder = command.reminderId() == null
                ? PatientRiskReminder.create(
                        patient,
                        branch,
                        visitOccurrence,
                        documentationRecord,
                        command.riskType(),
                        command.severityLabel(),
                        command.summary(),
                        command.effectiveAt(),
                        command.expiresAt(),
                        command.sourceContextType(),
                        command.sourceRecordId())
                : patientRiskReminderRepository.findByIdAndAgency_Id(command.reminderId(), actorMembership.getAgencyId())
                        .map(existing -> {
                            requireBranchAccess(actorMembership, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS, existing.getBranchId(), "manage patient risk reminders");
                            existing.updateDetails(
                                    branch,
                                    visitOccurrence,
                                    documentationRecord,
                                    command.severityLabel(),
                                    command.summary(),
                                    command.effectiveAt(),
                                    command.expiresAt(),
                                    command.sourceContextType(),
                                    command.sourceRecordId());
                            return existing;
                        })
                        .orElseThrow(() -> new ComplianceEntityNotFoundException("PatientRiskReminder", command.reminderId()));
        PatientRiskReminder saved = patientRiskReminderRepository.saveAndFlush(reminder);
        complianceAuditService.recordRiskReminderSaved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"riskType\":\"" + saved.getRiskType() + "\",\"status\":\"" + saved.statusAt(saved.getEffectiveAt()).name() + "\"}");
        return saved;
    }

    @Transactional
    public PatientRiskReminder resolveRiskReminder(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID reminderId,
            OffsetDateTime resolvedAt) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS, "resolve patient risk reminders");
        PatientRiskReminder reminder = patientRiskReminderRepository.findByIdAndAgency_Id(reminderId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("PatientRiskReminder", reminderId));
        requireBranchAccess(actorMembership, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS, reminder.getBranchId(), "resolve patient risk reminders");
        reminder.resolve(evaluatedAt(resolvedAt));
        PatientRiskReminder saved = patientRiskReminderRepository.saveAndFlush(reminder);
        complianceAuditService.recordRiskReminderResolved(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"riskType\":\"" + saved.getRiskType() + "\"}");
        return saved;
    }

    @Transactional
    public ComplianceStatusProjection recalculateStatusProjection(
            @NotNull AgencyMembership actorMembership,
            @Valid RecalculateStatusProjectionCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS, "recalculate compliance status");
        Patient patient = resolvePatient(actorMembership.getAgencyId(), command.patientId());
        Branch branch = resolvePatientScopeBranch(actorMembership, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS, command.branchId(), "recalculate compliance status");
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());

        List<ComplianceChecklistResult> allResults = complianceChecklistResultRepository.findAllByPatient_IdOrderByEvaluatedAtDesc(patient.getId());
        Collection<ComplianceChecklistResult> checklistResults = latestResultsForCategory(
                allResults, ComplianceEvaluationCategory.CHECKLIST_ITEM, command.branchId(), command.serviceLineId(), evaluatedAt);
        Collection<ComplianceChecklistResult> documentationResults = latestResultsForCategory(
                allResults, ComplianceEvaluationCategory.REQUIRED_DOCUMENTATION, command.branchId(), command.serviceLineId(), evaluatedAt);

        int checklistPassCount = countChecklistResults(checklistResults, ComplianceChecklistResultStatus.PASS);
        int checklistWarningCount = countChecklistResults(checklistResults, ComplianceChecklistResultStatus.WARNING);
        int checklistFailCount = countChecklistResults(checklistResults, ComplianceChecklistResultStatus.FAIL);
        int documentationSatisfiedCount = countChecklistResults(documentationResults, ComplianceChecklistResultStatus.PASS);
        int documentationWarningCount = countChecklistResults(documentationResults, ComplianceChecklistResultStatus.WARNING);
        int documentationUnsatisfiedCount = countChecklistResults(documentationResults, ComplianceChecklistResultStatus.FAIL);

        Map<String, ConsentAcknowledgmentRecord> acknowledgmentsByType = latestAcknowledgments(patient.getId());
        int missingAcknowledgmentCount = 0;
        int expiredAcknowledgmentCount = 0;
        for (String requiredType : command.requiredAcknowledgmentTypes()) {
            ConsentAcknowledgmentRecord record = acknowledgmentsByType.get(requiredType.trim().toUpperCase());
            ConsentAcknowledgmentStatus status = record == null ? ConsentAcknowledgmentStatus.MISSING : record.statusAt(evaluatedAt);
            if (status == ConsentAcknowledgmentStatus.EXPIRED) {
                expiredAcknowledgmentCount++;
            } else if (status != ConsentAcknowledgmentStatus.ACTIVE) {
                missingAcknowledgmentCount++;
            }
        }

        CertificationPeriodStatus certificationPeriodStatus = latestCertificationStatus(
                patient.getId(),
                command.branchId(),
                evaluatedAt.toLocalDate(),
                command.certificationExpiryWarningDays());
        int activeRiskReminderCount = activeRiskReminderCount(patient.getId(), command.branchId(), evaluatedAt);
        ComplianceReadinessStatus readinessStatus = deriveReadiness(
                checklistPassCount,
                checklistWarningCount,
                checklistFailCount,
                documentationSatisfiedCount,
                documentationWarningCount,
                documentationUnsatisfiedCount,
                missingAcknowledgmentCount,
                expiredAcknowledgmentCount,
                certificationPeriodStatus,
                activeRiskReminderCount,
                command.requiredAcknowledgmentTypes().isEmpty(),
                checklistResults.isEmpty() && documentationResults.isEmpty());

        ComplianceStatusProjection projection = complianceStatusProjectionRepository.findAllByPatient_IdOrderByEvaluatedAtDesc(patient.getId()).stream()
                .filter(candidate -> Objects.equals(candidate.getBranchId(), command.branchId()))
                .filter(candidate -> Objects.equals(candidate.getServiceLineId(), command.serviceLineId()))
                .findFirst()
                .orElse(null);
        if (projection == null) {
            projection = ComplianceStatusProjection.create(
                    patient,
                    branch,
                    serviceLine,
                    checklistPassCount,
                    checklistWarningCount,
                    checklistFailCount,
                    documentationSatisfiedCount,
                    documentationWarningCount,
                    documentationUnsatisfiedCount,
                    missingAcknowledgmentCount,
                    expiredAcknowledgmentCount,
                    certificationPeriodStatus,
                    activeRiskReminderCount,
                    readinessStatus,
                    evaluatedAt);
        }
        projection.recalculate(
                branch,
                serviceLine,
                checklistPassCount,
                checklistWarningCount,
                checklistFailCount,
                documentationSatisfiedCount,
                documentationWarningCount,
                documentationUnsatisfiedCount,
                missingAcknowledgmentCount,
                expiredAcknowledgmentCount,
                certificationPeriodStatus,
                activeRiskReminderCount,
                readinessStatus,
                evaluatedAt);
        ComplianceStatusProjection saved = complianceStatusProjectionRepository.saveAndFlush(projection);
        complianceAuditService.recordStatusProjectionRecalculated(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"readinessStatus\":\"" + saved.getReadinessStatus().name() + "\"}");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ComplianceDashboardAggregate> aggregateDashboard(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            ComplianceReadinessStatus readinessStatus) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD, "view compliance dashboard");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD, branchId, "view compliance dashboard");
        }
        Map<UUID, ComplianceStatusProjection> latestByScope = new LinkedHashMap<>();
        for (ComplianceStatusProjection projection : complianceStatusProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(actorMembership.getAgencyId())) {
            if (!hasBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD, projection.getBranchId())) {
                continue;
            }
            if (branchId != null && !Objects.equals(branchId, projection.getBranchId())) {
                continue;
            }
            if (readinessStatus != null && projection.getReadinessStatus() != readinessStatus) {
                continue;
            }
            latestByScope.putIfAbsent(scopeKey(projection.getPatientId(), projection.getBranchId(), projection.getServiceLineId()), projection);
        }

        Map<UUID, DashboardAccumulator> byBranch = new LinkedHashMap<>();
        for (ComplianceStatusProjection projection : latestByScope.values()) {
            UUID projectionBranchId = projection.getBranchId();
            DashboardAccumulator accumulator = byBranch.computeIfAbsent(
                    projectionBranchId,
                    ignored -> new DashboardAccumulator(projectionBranchId, resolveBranchName(projectionBranchId)));
            accumulator.totalPatients++;
            switch (projection.getReadinessStatus()) {
                case READY -> accumulator.readyCount++;
                case WARNING -> accumulator.warningCount++;
                case NON_COMPLIANT -> accumulator.nonCompliantCount++;
                case UNKNOWN -> accumulator.unknownCount++;
            }
            accumulator.activeRiskReminderCount += projection.getActiveRiskReminderCount();
            accumulator.acknowledgmentGapCount += projection.getMissingAcknowledgmentCount() + projection.getExpiredAcknowledgmentCount();
        }
        return byBranch.values().stream().map(DashboardAccumulator::toView).toList();
    }

    @Transactional(readOnly = true)
    public PatientComplianceWorkspaceView getPatientWorkspace(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view patient compliance workspace");
        resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view patient compliance workspace");
        }
        ComplianceStatusProjection projection = complianceStatusProjectionRepository.findAllByPatient_IdOrderByEvaluatedAtDesc(patientId).stream()
                .filter(candidate -> Objects.equals(candidate.getBranchId(), branchId))
                .filter(candidate -> Objects.equals(candidate.getServiceLineId(), serviceLineId))
                .findFirst()
                .orElse(null);
        List<ComplianceChecklistResult> results = complianceChecklistResultRepository.findAllByPatient_IdOrderByEvaluatedAtDesc(patientId).stream()
                .filter(candidate -> branchId == null || Objects.equals(candidate.getBranchId(), branchId))
                .filter(candidate -> serviceLineId == null || Objects.equals(candidate.getServiceLineId(), serviceLineId))
                .toList();
        List<ConsentAcknowledgmentRecord> acknowledgments = consentAcknowledgmentRecordRepository.findAllByPatient_IdOrderByEffectiveAtDesc(patientId).stream()
                .filter(candidate -> branchId == null || Objects.equals(candidate.getBranchId(), branchId))
                .toList();
        List<CertificationPeriodRecord> certificationPeriods = certificationPeriodRecordRepository.findAllByPatient_IdOrderByStartDateDesc(patientId).stream()
                .filter(candidate -> branchId == null || Objects.equals(candidate.getBranchId(), branchId))
                .toList();
        List<PatientRiskReminder> reminders = patientRiskReminderRepository.findAllByPatient_IdOrderByEffectiveAtDesc(patientId).stream()
                .filter(candidate -> branchId == null || Objects.equals(candidate.getBranchId(), branchId))
                .toList();
        return new PatientComplianceWorkspaceView(projection, results, acknowledgments, certificationPeriods, reminders);
    }

    @Transactional(readOnly = true)
    public List<ComplianceChecklistResult> listChecklistResults(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view patient compliance results");
        resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view patient compliance results");
        }
        return complianceChecklistResultRepository.findAllByPatient_IdOrderByEvaluatedAtDesc(patientId).stream()
                .filter(result -> branchId == null || Objects.equals(branchId, result.getBranchId()))
                .filter(result -> serviceLineId == null || Objects.equals(serviceLineId, result.getServiceLineId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentAcknowledgmentRecord> listAcknowledgments(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view patient acknowledgments");
        resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view patient acknowledgments");
        }
        return consentAcknowledgmentRecordRepository.findAllByPatient_IdOrderByEffectiveAtDesc(patientId).stream()
                .filter(record -> branchId == null || Objects.equals(branchId, record.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificationPeriodRecord> listCertificationPeriods(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view patient certification periods");
        resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view patient certification periods");
        }
        return certificationPeriodRecordRepository.findAllByPatient_IdOrderByStartDateDesc(patientId).stream()
                .filter(record -> branchId == null || Objects.equals(branchId, record.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientRiskReminder> listRiskReminders(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view patient risk reminders");
        resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view patient risk reminders");
        }
        return patientRiskReminderRepository.findAllByPatient_IdOrderByEffectiveAtDesc(patientId).stream()
                .filter(record -> branchId == null || Objects.equals(branchId, record.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplianceDashboardPatientSummary> listDashboardPatientSummaries(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            ComplianceReadinessStatus readinessStatus,
            CertificationPeriodStatus certificationPeriodStatus,
            String riskSeverity) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD, "view compliance dashboard");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD, branchId, "view compliance dashboard");
        }
        Map<String, ComplianceStatusProjection> latestByPatientScope = new LinkedHashMap<>();
        for (ComplianceStatusProjection projection : complianceStatusProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(actorMembership.getAgencyId())) {
            if (!hasBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD, projection.getBranchId())) {
                continue;
            }
            if (branchId != null && !Objects.equals(branchId, projection.getBranchId())) {
                continue;
            }
            if (readinessStatus != null && projection.getReadinessStatus() != readinessStatus) {
                continue;
            }
            if (certificationPeriodStatus != null && projection.getCertificationPeriodStatus() != certificationPeriodStatus) {
                continue;
            }
            String key = projection.getPatientId() + "|" + Objects.toString(projection.getBranchId(), "null") + "|" + Objects.toString(projection.getServiceLineId(), "null");
            latestByPatientScope.putIfAbsent(key, projection);
        }
        String normalizedRiskSeverity = riskSeverity == null || riskSeverity.isBlank() ? null : riskSeverity.trim();
        return latestByPatientScope.values().stream()
                .filter(projection -> normalizedRiskSeverity == null || listRiskReminders(actorMembership, projection.getPatientId(), projection.getBranchId()).stream()
                        .anyMatch(reminder -> normalizedRiskSeverity.equalsIgnoreCase(reminder.getSeverityLabel())))
                .map(this::toDashboardPatientSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplianceHistoryView getComplianceHistory(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view patient compliance history");
        resolvePatient(actorMembership.getAgencyId(), patientId);
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, branchId, "view patient compliance history");
        }
        List<ConsentAcknowledgmentRecord> acknowledgments = listAcknowledgments(actorMembership, patientId, branchId);
        List<CertificationPeriodRecord> certificationPeriods = listCertificationPeriods(actorMembership, patientId, branchId);
        List<PatientRiskReminder> reminders = listRiskReminders(actorMembership, patientId, branchId);
        List<ComplianceChecklistResult> results = listChecklistResults(actorMembership, patientId, branchId, null);
        List<AuditEvent> auditContext = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> branchId == null || Objects.equals(branchId, event.getBranchId()))
                .filter(event -> matchesComplianceHistoryTarget(event.getTargetId(), acknowledgments, certificationPeriods, reminders, results))
                .toList();
        List<ComplianceHistoryEntry> timeline = new ArrayList<>();
        acknowledgments.forEach(item -> timeline.add(new ComplianceHistoryEntry("ACKNOWLEDGMENT", item.getId(), item.getEffectiveAt(), item.getAcknowledgmentType())));
        certificationPeriods.forEach(item -> timeline.add(new ComplianceHistoryEntry(
                "CERTIFICATION_PERIOD",
                item.getId(),
                item.getCreatedAt() == null ? null : OffsetDateTime.ofInstant(item.getCreatedAt(), ZoneOffset.UTC),
                item.getProgramContext())));
        reminders.forEach(item -> timeline.add(new ComplianceHistoryEntry("RISK_REMINDER", item.getId(), item.getEffectiveAt(), item.getRiskType())));
        results.forEach(item -> timeline.add(new ComplianceHistoryEntry("RESULT", item.getId(), item.getEvaluatedAt(), item.getResultCode())));
        timeline.sort((left, right) -> {
            OffsetDateTime leftAt = left.occurredAt();
            OffsetDateTime rightAt = right.occurredAt();
            if (leftAt == null && rightAt == null) {
                return 0;
            }
            if (leftAt == null) {
                return 1;
            }
            if (rightAt == null) {
                return -1;
            }
            return rightAt.compareTo(leftAt);
        });
        return new ComplianceHistoryView(timeline, acknowledgments, certificationPeriods, reminders, auditContext);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getChecklistDefinitionHistory(@NotNull AgencyMembership actorMembership, @NotNull UUID checklistDefinitionId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view compliance checklist history");
        checklistDefinitionRepository.findByIdAndAgency_Id(checklistDefinitionId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("ComplianceChecklistDefinition", checklistDefinitionId));
        return auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> Objects.equals(checklistDefinitionId, event.getTargetId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getDocumentationRequirementHistory(@NotNull AgencyMembership actorMembership, @NotNull UUID requirementId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE, "view compliance requirement history");
        documentationRequirementRepository.findByIdAndAgency_Id(requirementId, actorMembership.getAgencyId())
                .orElseThrow(() -> new ComplianceEntityNotFoundException("RequiredDocumentationRequirement", requirementId));
        return auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> Objects.equals(requirementId, event.getTargetId()))
                .toList();
    }

    private Map<String, ConsentAcknowledgmentRecord> latestAcknowledgments(UUID patientId) {
        Map<String, ConsentAcknowledgmentRecord> byType = new LinkedHashMap<>();
        for (ConsentAcknowledgmentRecord record : consentAcknowledgmentRecordRepository.findAllByPatient_IdOrderByEffectiveAtDesc(patientId)) {
            byType.putIfAbsent(record.getAcknowledgmentType(), record);
        }
        return byType;
    }

    private CertificationPeriodStatus latestCertificationStatus(UUID patientId, UUID branchId, LocalDate asOfDate, int expiryWarningDays) {
        return certificationPeriodRecordRepository.findAllByPatient_IdOrderByStartDateDesc(patientId).stream()
                .filter(record -> branchId == null || Objects.equals(record.getBranchId(), branchId))
                .findFirst()
                .map(record -> record.projectStatus(asOfDate, expiryWarningDays))
                .orElse(CertificationPeriodStatus.MISSING);
    }

    private int activeRiskReminderCount(UUID patientId, UUID branchId, OffsetDateTime evaluatedAt) {
        return (int) patientRiskReminderRepository.findAllByPatient_IdOrderByEffectiveAtDesc(patientId).stream()
                .filter(reminder -> branchId == null || Objects.equals(reminder.getBranchId(), branchId))
                .filter(reminder -> reminder.statusAt(evaluatedAt) == PatientRiskReminderStatus.ACTIVE)
                .count();
    }

    private ComplianceReadinessStatus deriveReadiness(
            int checklistPassCount,
            int checklistWarningCount,
            int checklistFailCount,
            int documentationSatisfiedCount,
            int documentationWarningCount,
            int documentationUnsatisfiedCount,
            int missingAcknowledgmentCount,
            int expiredAcknowledgmentCount,
            CertificationPeriodStatus certificationPeriodStatus,
            int activeRiskReminderCount,
            boolean noRequiredAcknowledgmentsConfigured,
            boolean noResultSignalsPresent) {
        if (checklistFailCount > 0
                || documentationUnsatisfiedCount > 0
                || missingAcknowledgmentCount > 0
                || expiredAcknowledgmentCount > 0
                || certificationPeriodStatus == CertificationPeriodStatus.EXPIRED
                || certificationPeriodStatus == CertificationPeriodStatus.MISSING) {
            return ComplianceReadinessStatus.NON_COMPLIANT;
        }
        if (checklistWarningCount > 0
                || documentationWarningCount > 0
                || certificationPeriodStatus == CertificationPeriodStatus.UPCOMING_EXPIRY
                || activeRiskReminderCount > 0) {
            return ComplianceReadinessStatus.WARNING;
        }
        if (noResultSignalsPresent && noRequiredAcknowledgmentsConfigured && certificationPeriodStatus == CertificationPeriodStatus.MISSING) {
            return ComplianceReadinessStatus.UNKNOWN;
        }
        if (checklistPassCount > 0 || documentationSatisfiedCount > 0 || certificationPeriodStatus == CertificationPeriodStatus.CURRENT) {
            return ComplianceReadinessStatus.READY;
        }
        return ComplianceReadinessStatus.UNKNOWN;
    }

    private Collection<ComplianceChecklistResult> latestResultsForCategory(
            List<ComplianceChecklistResult> allResults,
            ComplianceEvaluationCategory category,
            UUID branchId,
            UUID serviceLineId,
            OffsetDateTime evaluatedAt) {
        Map<String, ComplianceChecklistResult> latest = new LinkedHashMap<>();
        for (ComplianceChecklistResult result : allResults) {
            if (result.getEvaluationCategory() != category) {
                continue;
            }
            if (result.getEvaluatedAt().isAfter(evaluatedAt)) {
                continue;
            }
            if (branchId != null && !Objects.equals(branchId, result.getBranchId())) {
                continue;
            }
            if (serviceLineId != null && !Objects.equals(serviceLineId, result.getServiceLineId())) {
                continue;
            }
            String key = category == ComplianceEvaluationCategory.CHECKLIST_ITEM
                    ? Objects.toString(result.getChecklistDefinition() == null ? null : result.getChecklistDefinition().getId(), result.getResultCode())
                    : Objects.toString(result.getDocumentationRequirement() == null ? null : result.getDocumentationRequirement().getId(), result.getResultCode());
            latest.putIfAbsent(key, result);
        }
        return latest.values();
    }

    private int countChecklistResults(Collection<ComplianceChecklistResult> results, ComplianceChecklistResultStatus status) {
        return (int) results.stream().filter(result -> result.getResultStatus() == status).count();
    }

    private VisitDocumentationRecord resolveDocumentationCandidate(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            RequiredDocumentationRequirement requirement) {
        return visitDocumentationRecordRepository.findAllByAgency_IdAndPatient_IdOrderByLastSavedAtDesc(patient.getAgencyId(), patient.getId()).stream()
                .filter(record -> SATISFYING_DOCUMENTATION_STATUSES.contains(record.getStatus()))
                .filter(record -> branch == null || Objects.equals(record.getBranchId(), branch.getId()))
                .filter(record -> serviceLine == null
                        || (record.getVisitOccurrence() != null
                        && record.getVisitOccurrence().getServiceLine() != null
                        && Objects.equals(record.getVisitOccurrence().getServiceLine().getId(), serviceLine.getId())))
                .filter(record -> requirement.getServiceLineId() == null
                        || (record.getVisitOccurrence() != null
                        && record.getVisitOccurrence().getServiceLine() != null
                        && Objects.equals(record.getVisitOccurrence().getServiceLine().getId(), requirement.getServiceLineId())))
                .findFirst()
                .orElse(null);
    }

    private boolean hasPresentSignature(VisitDocumentationRecord documentationRecord) {
        if (documentationRecord == null || documentationRecord.getVisitOccurrenceId() == null) {
            return false;
        }
        return evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(documentationRecord.getVisitOccurrenceId())
                .map(EvvVerificationSession::getId)
                .map(signatureVerificationLinkRepository::findAllByVerificationSession_IdOrderByRecordedAtAsc)
                .stream()
                .flatMap(List::stream)
                .anyMatch(link -> link.getVerificationStatus() == SignatureVerificationStatus.PRESENT);
    }

    private Patient resolvePatient(UUID agencyId, UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(patient -> Objects.equals(patient.getAgencyId(), agencyId))
                .orElseThrow(() -> new ComplianceEntityNotFoundException("Patient", patientId));
    }

    private Branch resolveManagedBranch(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (branchId == null) {
            if (actorMembership.getRole() != AgencyRole.AGENCY_OWNER) {
                throw new UnauthorizedComplianceActorException(actorMembership.getId(), action);
            }
            return null;
        }
        Branch branch = resolveBranch(actorMembership.getAgencyId(), branchId);
        requireBranchAccess(actorMembership, permission, branchId, action);
        return branch;
    }

    private Branch resolvePatientScopeBranch(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (branchId == null) {
            if (actorMembership.getRole() != AgencyRole.AGENCY_OWNER && actorMembership.getRole() != AgencyRole.READ_ONLY_AUDITOR) {
                throw new UnauthorizedComplianceActorException(actorMembership.getId(), action);
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
                .orElseThrow(() -> new ComplianceEntityNotFoundException("Branch", branchId));
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> Objects.equals(serviceLine.getAgencyId(), agencyId))
                .orElseThrow(() -> new ComplianceEntityNotFoundException("ServiceLine", serviceLineId));
    }

    private AgencyMembership resolveMembership(UUID agencyId, UUID membershipId) {
        if (membershipId == null) {
            return null;
        }
        return agencyMembershipRepository.findById(membershipId)
                .filter(membership -> Objects.equals(membership.getAgencyId(), agencyId))
                .orElseThrow(() -> new ComplianceEntityNotFoundException("AgencyMembership", membershipId));
    }

    private PatientPayerLink resolvePatientPayerLink(UUID agencyId, UUID patientId, UUID patientPayerLinkId) {
        if (patientPayerLinkId == null) {
            return null;
        }
        return patientPayerLinkRepository.findById(patientPayerLinkId)
                .filter(link -> Objects.equals(link.getAgencyId(), agencyId))
                .filter(link -> Objects.equals(link.getPatient().getId(), patientId))
                .orElseThrow(() -> new ComplianceEntityNotFoundException("PatientPayerLink", patientPayerLinkId));
    }

    private VisitOccurrence resolveVisitOccurrence(UUID agencyId, UUID patientId, UUID visitOccurrenceId) {
        if (visitOccurrenceId == null) {
            return null;
        }
        return visitOccurrenceRepository.findById(visitOccurrenceId)
                .filter(visit -> Objects.equals(visit.getAgencyId(), agencyId))
                .filter(visit -> Objects.equals(visit.getPatient().getId(), patientId))
                .orElseThrow(() -> new ComplianceEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
    }

    private VisitDocumentationRecord resolveDocumentationRecord(UUID agencyId, UUID patientId, UUID documentationRecordId) {
        if (documentationRecordId == null) {
            return null;
        }
        return visitDocumentationRecordRepository.findById(documentationRecordId)
                .filter(record -> Objects.equals(record.getAgencyId(), agencyId))
                .filter(record -> Objects.equals(record.getPatientId(), patientId))
                .orElseThrow(() -> new ComplianceEntityNotFoundException("VisitDocumentationRecord", documentationRecordId));
    }

    private String resolveBranchName(UUID branchId) {
        return branchId == null
                ? "Unscoped"
                : branchRepository.findById(branchId).map(Branch::getName).orElse("Unknown Branch");
    }

    private UUID scopeKey(UUID patientId, UUID branchId, UUID serviceLineId) {
        return UUID.nameUUIDFromBytes((Objects.toString(patientId, "null")
                + "|"
                + Objects.toString(branchId, "null")
                + "|"
                + Objects.toString(serviceLineId, "null")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission, String action) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                permission,
                membershipId -> new UnauthorizedComplianceActorException(membershipId, action));
    }

    private void requireBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (!hasBranchAccess(actorMembership, permission, branchId)) {
            throw new UnauthorizedComplianceActorException(actorMembership.getId(), action);
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

    private OffsetDateTime evaluatedAt(OffsetDateTime value) {
        return value == null ? OffsetDateTime.now() : value;
    }

    private ComplianceDashboardPatientSummary toDashboardPatientSummary(ComplianceStatusProjection projection) {
        Patient patient = resolvePatient(projection.getAgencyId(), projection.getPatientId());
        return new ComplianceDashboardPatientSummary(
                projection.getPatientId(),
                projection.getBranchId(),
                resolveBranchName(projection.getBranchId()),
                patient.getFirstName(),
                patient.getLastName(),
                projection.getReadinessStatus(),
                projection.getCertificationPeriodStatus(),
                projection.getActiveRiskReminderCount(),
                projection.getChecklistFailCount() + projection.getDocumentationUnsatisfiedCount(),
                projection.getMissingAcknowledgmentCount() + projection.getExpiredAcknowledgmentCount(),
                projection.getEvaluatedAt());
    }

    private boolean matchesComplianceHistoryTarget(
            UUID targetId,
            List<ConsentAcknowledgmentRecord> acknowledgments,
            List<CertificationPeriodRecord> certificationPeriods,
            List<PatientRiskReminder> reminders,
            List<ComplianceChecklistResult> results) {
        return acknowledgments.stream().anyMatch(item -> Objects.equals(item.getId(), targetId))
                || certificationPeriods.stream().anyMatch(item -> Objects.equals(item.getId(), targetId))
                || reminders.stream().anyMatch(item -> Objects.equals(item.getId(), targetId))
                || results.stream().anyMatch(item -> Objects.equals(item.getId(), targetId));
    }

    public record CreateChecklistDefinitionCommand(
            UUID branchId,
            UUID serviceLineId,
            @NotBlank String itemCode,
            @NotBlank String description,
            String severityLabel,
            Integer weightScore,
            boolean active) {
    }

    public record CreateDocumentationRequirementCommand(
            UUID branchId,
            UUID serviceLineId,
            @NotBlank String sourceCategory,
            @NotBlank String requirementCode,
            @NotBlank String description,
            boolean patientApplicable,
            boolean episodeApplicable,
            Integer dueDays,
            Integer recencyDays,
            boolean requiresSignatureVerification,
            boolean requiresAttachmentEvidence,
            boolean active) {
    }

    public record RecalculateChecklistResultCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            @NotNull UUID checklistDefinitionId,
            @NotNull ComplianceChecklistResultStatus resultStatus,
            String evidenceSourceType,
            UUID evidenceSourceId,
            String evidenceSummary,
            String evaluationOrigin,
            LocalDate contextPeriodStart,
            LocalDate contextPeriodEnd,
            OffsetDateTime evaluatedAt) {
    }

    public record EvaluateDocumentationRequirementCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            @NotNull UUID requirementId,
            OffsetDateTime referenceAt,
            LocalDate contextPeriodStart,
            LocalDate contextPeriodEnd,
            String evaluationOrigin,
            OffsetDateTime evaluatedAt) {
    }

    public record RecordConsentAcknowledgmentCommand(
            @NotNull UUID patientId,
            UUID branchId,
            @NotBlank String acknowledgmentType,
            @NotNull OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            UUID capturedByMembershipId,
            String captureMethod,
            String supportingArtifactType,
            UUID supportingArtifactId) {
    }

    public record SaveCertificationPeriodCommand(
            UUID certificationPeriodId,
            @NotNull UUID patientId,
            UUID branchId,
            UUID patientPayerLinkId,
            String programContext,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            boolean closed,
            String source) {
    }

    public record SaveRiskReminderCommand(
            UUID reminderId,
            @NotNull UUID patientId,
            UUID branchId,
            UUID visitOccurrenceId,
            UUID documentationRecordId,
            @NotBlank String riskType,
            String severityLabel,
            @NotBlank String summary,
            @NotNull OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            String sourceContextType,
            UUID sourceRecordId) {
    }

    public record RecalculateStatusProjectionCommand(
            @NotNull UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            @NotNull Set<String> requiredAcknowledgmentTypes,
            int certificationExpiryWarningDays,
            OffsetDateTime evaluatedAt) {
    }

    public record ComplianceDashboardAggregate(
            UUID branchId,
            String branchName,
            int totalPatients,
            int readyCount,
            int warningCount,
            int nonCompliantCount,
            int unknownCount,
            int activeRiskReminderCount,
            int acknowledgmentGapCount) {
    }

    public record PatientComplianceWorkspaceView(
            ComplianceStatusProjection projection,
            List<ComplianceChecklistResult> results,
            List<ConsentAcknowledgmentRecord> acknowledgments,
            List<CertificationPeriodRecord> certificationPeriods,
            List<PatientRiskReminder> reminders) {
    }

    public record ComplianceDashboardPatientSummary(
            UUID patientId,
            UUID branchId,
            String branchName,
            String firstName,
            String lastName,
            ComplianceReadinessStatus readinessStatus,
            CertificationPeriodStatus certificationPeriodStatus,
            int activeRiskReminderCount,
            int gapCount,
            int acknowledgmentGapCount,
            OffsetDateTime evaluatedAt) {
    }

    public record ComplianceHistoryEntry(
            String entryType,
            UUID entityId,
            OffsetDateTime occurredAt,
            String summary) {
    }

    public record ComplianceHistoryView(
            List<ComplianceHistoryEntry> timeline,
            List<ConsentAcknowledgmentRecord> acknowledgments,
            List<CertificationPeriodRecord> certificationPeriods,
            List<PatientRiskReminder> reminders,
            List<AuditEvent> auditContext) {
    }

    private static final class DashboardAccumulator {
        private final UUID branchId;
        private final String branchName;
        private int totalPatients;
        private int readyCount;
        private int warningCount;
        private int nonCompliantCount;
        private int unknownCount;
        private int activeRiskReminderCount;
        private int acknowledgmentGapCount;

        private DashboardAccumulator(UUID branchId, String branchName) {
            this.branchId = branchId;
            this.branchName = branchName;
        }

        private ComplianceDashboardAggregate toView() {
            return new ComplianceDashboardAggregate(
                    branchId,
                    branchName,
                    totalPatients,
                    readyCount,
                    warningCount,
                    nonCompliantCount,
                    unknownCount,
                    activeRiskReminderCount,
                    acknowledgmentGapCount);
        }
    }
}
