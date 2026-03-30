package com.homehealthcare.compliance.api;

import com.homehealthcare.compliance.application.ComplianceWorkspaceService;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.ComplianceDashboardAggregate;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.ComplianceDashboardPatientSummary;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.ComplianceHistoryEntry;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.ComplianceHistoryView;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.CreateChecklistDefinitionCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.CreateDocumentationRequirementCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.PatientComplianceWorkspaceView;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.RecalculateStatusProjectionCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.RecordConsentAcknowledgmentCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.SaveCertificationPeriodCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.SaveRiskReminderCommand;
import com.homehealthcare.compliance.domain.CertificationPeriodRecord;
import com.homehealthcare.compliance.domain.ComplianceChecklistDefinition;
import com.homehealthcare.compliance.domain.ComplianceChecklistResult;
import com.homehealthcare.compliance.domain.ComplianceStatusProjection;
import com.homehealthcare.compliance.domain.ConsentAcknowledgmentRecord;
import com.homehealthcare.compliance.domain.PatientRiskReminder;
import com.homehealthcare.compliance.domain.RequiredDocumentationRequirement;
import com.homehealthcare.compliance.foundation.CertificationPeriodStatus;
import com.homehealthcare.compliance.foundation.ComplianceChecklistResultStatus;
import com.homehealthcare.compliance.foundation.ComplianceEvaluationCategory;
import com.homehealthcare.compliance.foundation.ComplianceReadinessStatus;
import com.homehealthcare.compliance.foundation.ConsentAcknowledgmentStatus;
import com.homehealthcare.compliance.foundation.PatientRiskReminderStatus;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
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
@RequestMapping("/api/compliance")
class ComplianceController {

    private final ConfigurationActorResolver actorResolver;
    private final ComplianceWorkspaceService complianceWorkspaceService;

    ComplianceController(ConfigurationActorResolver actorResolver, ComplianceWorkspaceService complianceWorkspaceService) {
        this.actorResolver = actorResolver;
        this.complianceWorkspaceService = complianceWorkspaceService;
    }

    @GetMapping("/dashboard")
    List<ComplianceDashboardAggregateResponse> getDashboardAggregates(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "readinessStatus", required = false) ComplianceReadinessStatus readinessStatus) {
        return complianceWorkspaceService.aggregateDashboard(
                        actorResolver.requireActorMembership(),
                        branchId,
                        readinessStatus)
                .stream()
                .map(ComplianceController::toDashboardAggregateResponse)
                .toList();
    }

    @GetMapping("/dashboard/patients")
    PagedResponse<ComplianceDashboardPatientSummaryResponse> getDashboardPatients(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "readinessStatus", required = false) ComplianceReadinessStatus readinessStatus,
            @RequestParam(name = "certificationPeriodStatus", required = false) CertificationPeriodStatus certificationPeriodStatus,
            @RequestParam(name = "riskSeverity", required = false) String riskSeverity,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                complianceWorkspaceService.listDashboardPatientSummaries(
                                actorResolver.requireActorMembership(),
                                branchId,
                                readinessStatus,
                                certificationPeriodStatus,
                                riskSeverity)
                        .stream()
                        .map(ComplianceController::toDashboardPatientSummaryResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/patients/{patientId}")
    PatientComplianceWorkspaceResponse getPatientWorkspace(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId) {
        return toPatientComplianceWorkspaceResponse(complianceWorkspaceService.getPatientWorkspace(
                actorResolver.requireActorMembership(),
                patientId,
                branchId,
                serviceLineId));
    }

    @GetMapping("/patients/{patientId}/checklist-results")
    List<ComplianceChecklistResultResponse> getChecklistResults(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId) {
        return complianceWorkspaceService.listChecklistResults(
                        actorResolver.requireActorMembership(),
                        patientId,
                        branchId,
                        serviceLineId)
                .stream()
                .filter(result -> result.getEvaluationCategory() == ComplianceEvaluationCategory.CHECKLIST_ITEM)
                .map(ComplianceController::toChecklistResultResponse)
                .toList();
    }

    @GetMapping("/patients/{patientId}/documentation-results")
    List<ComplianceChecklistResultResponse> getDocumentationResults(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId) {
        return complianceWorkspaceService.listChecklistResults(
                        actorResolver.requireActorMembership(),
                        patientId,
                        branchId,
                        serviceLineId)
                .stream()
                .filter(result -> result.getEvaluationCategory() == ComplianceEvaluationCategory.REQUIRED_DOCUMENTATION)
                .map(ComplianceController::toChecklistResultResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/status-projection/recalculate")
    ComplianceStatusProjectionResponse recalculateStatusProjection(
            @PathVariable UUID patientId,
            @Valid @RequestBody RecalculateStatusProjectionRequest request) {
        return toStatusProjectionResponse(complianceWorkspaceService.recalculateStatusProjection(
                actorResolver.requireActorMembership(),
                new RecalculateStatusProjectionCommand(
                        patientId,
                        request.branchId(),
                        request.serviceLineId(),
                        Set.copyOf(request.requiredAcknowledgmentTypes()),
                        request.certificationExpiryWarningDays(),
                        request.evaluatedAt())));
    }

    @GetMapping("/patients/{patientId}/acknowledgments")
    List<ConsentAcknowledgmentResponse> listAcknowledgments(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return complianceWorkspaceService.listAcknowledgments(actorResolver.requireActorMembership(), patientId, branchId).stream()
                .map(ComplianceController::toConsentAcknowledgmentResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/acknowledgments")
    ConsentAcknowledgmentResponse createAcknowledgment(
            @PathVariable UUID patientId,
            @Valid @RequestBody SaveAcknowledgmentRequest request) {
        return toConsentAcknowledgmentResponse(complianceWorkspaceService.recordAcknowledgment(
                actorResolver.requireActorMembership(),
                new RecordConsentAcknowledgmentCommand(
                        patientId,
                        request.branchId(),
                        request.acknowledgmentType(),
                        request.effectiveAt(),
                        request.expiresAt(),
                        request.capturedByMembershipId(),
                        request.captureMethod(),
                        request.supportingArtifactType(),
                        request.supportingArtifactId())));
    }

    @PutMapping("/patients/{patientId}/acknowledgments/{acknowledgmentId}")
    ConsentAcknowledgmentResponse updateAcknowledgment(
            @PathVariable UUID patientId,
            @PathVariable UUID acknowledgmentId,
            @Valid @RequestBody SaveAcknowledgmentRequest request) {
        return toConsentAcknowledgmentResponse(complianceWorkspaceService.saveAcknowledgment(
                actorResolver.requireActorMembership(),
                acknowledgmentId,
                new RecordConsentAcknowledgmentCommand(
                        patientId,
                        request.branchId(),
                        request.acknowledgmentType(),
                        request.effectiveAt(),
                        request.expiresAt(),
                        request.capturedByMembershipId(),
                        request.captureMethod(),
                        request.supportingArtifactType(),
                        request.supportingArtifactId())));
    }

    @DeleteMapping("/patients/{patientId}/acknowledgments/{acknowledgmentId}")
    ConsentAcknowledgmentResponse revokeAcknowledgment(
            @PathVariable UUID patientId,
            @PathVariable UUID acknowledgmentId,
            @RequestParam(name = "revokedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime revokedAt) {
        return toConsentAcknowledgmentResponse(complianceWorkspaceService.revokeAcknowledgment(
                actorResolver.requireActorMembership(),
                acknowledgmentId,
                revokedAt));
    }

    @GetMapping("/patients/{patientId}/certification-periods")
    List<CertificationPeriodResponse> listCertificationPeriods(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return complianceWorkspaceService.listCertificationPeriods(actorResolver.requireActorMembership(), patientId, branchId).stream()
                .map(ComplianceController::toCertificationPeriodResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/certification-periods")
    CertificationPeriodResponse createCertificationPeriod(
            @PathVariable UUID patientId,
            @Valid @RequestBody SaveCertificationPeriodRequest request) {
        return toCertificationPeriodResponse(complianceWorkspaceService.saveCertificationPeriod(
                actorResolver.requireActorMembership(),
                new SaveCertificationPeriodCommand(
                        null,
                        patientId,
                        request.branchId(),
                        request.patientPayerLinkId(),
                        request.programContext(),
                        request.startDate(),
                        request.endDate(),
                        request.closed(),
                        request.source())));
    }

    @PutMapping("/patients/{patientId}/certification-periods/{certificationPeriodId}")
    CertificationPeriodResponse updateCertificationPeriod(
            @PathVariable UUID patientId,
            @PathVariable UUID certificationPeriodId,
            @Valid @RequestBody SaveCertificationPeriodRequest request) {
        return toCertificationPeriodResponse(complianceWorkspaceService.saveCertificationPeriod(
                actorResolver.requireActorMembership(),
                new SaveCertificationPeriodCommand(
                        certificationPeriodId,
                        patientId,
                        request.branchId(),
                        request.patientPayerLinkId(),
                        request.programContext(),
                        request.startDate(),
                        request.endDate(),
                        request.closed(),
                        request.source())));
    }

    @PostMapping("/patients/{patientId}/certification-periods/{certificationPeriodId}/close")
    CertificationPeriodResponse closeCertificationPeriod(
            @PathVariable UUID patientId,
            @PathVariable UUID certificationPeriodId,
            @Valid @RequestBody CloseCertificationPeriodRequest request) {
        return toCertificationPeriodResponse(complianceWorkspaceService.saveCertificationPeriod(
                actorResolver.requireActorMembership(),
                new SaveCertificationPeriodCommand(
                        certificationPeriodId,
                        patientId,
                        request.branchId(),
                        request.patientPayerLinkId(),
                        request.programContext(),
                        request.startDate(),
                        request.endDate(),
                        true,
                        request.source())));
    }

    @GetMapping("/patients/{patientId}/risk-reminders")
    List<PatientRiskReminderResponse> listRiskReminders(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return complianceWorkspaceService.listRiskReminders(actorResolver.requireActorMembership(), patientId, branchId).stream()
                .map(ComplianceController::toPatientRiskReminderResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/risk-reminders")
    PatientRiskReminderResponse createRiskReminder(
            @PathVariable UUID patientId,
            @Valid @RequestBody SaveRiskReminderRequest request) {
        return toPatientRiskReminderResponse(complianceWorkspaceService.saveRiskReminder(
                actorResolver.requireActorMembership(),
                new SaveRiskReminderCommand(
                        null,
                        patientId,
                        request.branchId(),
                        request.visitOccurrenceId(),
                        request.documentationRecordId(),
                        request.riskType(),
                        request.severityLabel(),
                        request.summary(),
                        request.effectiveAt(),
                        request.expiresAt(),
                        request.sourceContextType(),
                        request.sourceRecordId())));
    }

    @PutMapping("/patients/{patientId}/risk-reminders/{reminderId}")
    PatientRiskReminderResponse updateRiskReminder(
            @PathVariable UUID patientId,
            @PathVariable UUID reminderId,
            @Valid @RequestBody SaveRiskReminderRequest request) {
        return toPatientRiskReminderResponse(complianceWorkspaceService.saveRiskReminder(
                actorResolver.requireActorMembership(),
                new SaveRiskReminderCommand(
                        reminderId,
                        patientId,
                        request.branchId(),
                        request.visitOccurrenceId(),
                        request.documentationRecordId(),
                        request.riskType(),
                        request.severityLabel(),
                        request.summary(),
                        request.effectiveAt(),
                        request.expiresAt(),
                        request.sourceContextType(),
                        request.sourceRecordId())));
    }

    @PostMapping("/patients/{patientId}/risk-reminders/{reminderId}/resolve")
    PatientRiskReminderResponse resolveRiskReminder(
            @PathVariable UUID patientId,
            @PathVariable UUID reminderId,
            @RequestParam(name = "resolvedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime resolvedAt) {
        return toPatientRiskReminderResponse(complianceWorkspaceService.resolveRiskReminder(
                actorResolver.requireActorMembership(),
                reminderId,
                resolvedAt));
    }

    @GetMapping("/checklist-definitions")
    List<ComplianceChecklistDefinitionResponse> listChecklistDefinitions(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "active", required = false) Boolean active) {
        return complianceWorkspaceService.listChecklistDefinitions(actorResolver.requireActorMembership(), branchId, serviceLineId, active).stream()
                .map(ComplianceController::toChecklistDefinitionResponse)
                .toList();
    }

    @PostMapping("/checklist-definitions")
    ComplianceChecklistDefinitionResponse createChecklistDefinition(@Valid @RequestBody SaveChecklistDefinitionRequest request) {
        return toChecklistDefinitionResponse(complianceWorkspaceService.createChecklistDefinition(
                actorResolver.requireActorMembership(),
                new CreateChecklistDefinitionCommand(
                        request.branchId(),
                        request.serviceLineId(),
                        request.itemCode(),
                        request.description(),
                        request.severityLabel(),
                        request.weightScore(),
                        request.active())));
    }

    @PutMapping("/checklist-definitions/{checklistDefinitionId}")
    ComplianceChecklistDefinitionResponse updateChecklistDefinition(
            @PathVariable UUID checklistDefinitionId,
            @Valid @RequestBody SaveChecklistDefinitionRequest request) {
        return toChecklistDefinitionResponse(complianceWorkspaceService.updateChecklistDefinition(
                actorResolver.requireActorMembership(),
                checklistDefinitionId,
                new CreateChecklistDefinitionCommand(
                        request.branchId(),
                        request.serviceLineId(),
                        request.itemCode(),
                        request.description(),
                        request.severityLabel(),
                        request.weightScore(),
                        request.active())));
    }

    @DeleteMapping("/checklist-definitions/{checklistDefinitionId}")
    ComplianceChecklistDefinitionResponse deactivateChecklistDefinition(@PathVariable UUID checklistDefinitionId) {
        return toChecklistDefinitionResponse(complianceWorkspaceService.deactivateChecklistDefinition(
                actorResolver.requireActorMembership(),
                checklistDefinitionId));
    }

    @GetMapping("/checklist-definitions/{checklistDefinitionId}/history")
    List<AuditEventResponse> getChecklistDefinitionHistory(@PathVariable UUID checklistDefinitionId) {
        return complianceWorkspaceService.getChecklistDefinitionHistory(actorResolver.requireActorMembership(), checklistDefinitionId).stream()
                .map(ComplianceController::toAuditEventResponse)
                .toList();
    }

    @GetMapping("/documentation-requirements")
    List<RequiredDocumentationRequirementResponse> listDocumentationRequirements(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "active", required = false) Boolean active) {
        return complianceWorkspaceService.listDocumentationRequirements(actorResolver.requireActorMembership(), branchId, serviceLineId, active).stream()
                .map(ComplianceController::toDocumentationRequirementResponse)
                .toList();
    }

    @PostMapping("/documentation-requirements")
    RequiredDocumentationRequirementResponse createDocumentationRequirement(@Valid @RequestBody SaveDocumentationRequirementRequest request) {
        return toDocumentationRequirementResponse(complianceWorkspaceService.createDocumentationRequirement(
                actorResolver.requireActorMembership(),
                new CreateDocumentationRequirementCommand(
                        request.branchId(),
                        request.serviceLineId(),
                        request.sourceCategory(),
                        request.requirementCode(),
                        request.description(),
                        request.patientApplicable(),
                        request.episodeApplicable(),
                        request.dueDays(),
                        request.recencyDays(),
                        request.requiresSignatureVerification(),
                        request.requiresAttachmentEvidence(),
                        request.active())));
    }

    @PutMapping("/documentation-requirements/{requirementId}")
    RequiredDocumentationRequirementResponse updateDocumentationRequirement(
            @PathVariable UUID requirementId,
            @Valid @RequestBody SaveDocumentationRequirementRequest request) {
        return toDocumentationRequirementResponse(complianceWorkspaceService.updateDocumentationRequirement(
                actorResolver.requireActorMembership(),
                requirementId,
                new CreateDocumentationRequirementCommand(
                        request.branchId(),
                        request.serviceLineId(),
                        request.sourceCategory(),
                        request.requirementCode(),
                        request.description(),
                        request.patientApplicable(),
                        request.episodeApplicable(),
                        request.dueDays(),
                        request.recencyDays(),
                        request.requiresSignatureVerification(),
                        request.requiresAttachmentEvidence(),
                        request.active())));
    }

    @DeleteMapping("/documentation-requirements/{requirementId}")
    RequiredDocumentationRequirementResponse deactivateDocumentationRequirement(@PathVariable UUID requirementId) {
        return toDocumentationRequirementResponse(complianceWorkspaceService.deactivateDocumentationRequirement(
                actorResolver.requireActorMembership(),
                requirementId));
    }

    @GetMapping("/documentation-requirements/{requirementId}/history")
    List<AuditEventResponse> getDocumentationRequirementHistory(@PathVariable UUID requirementId) {
        return complianceWorkspaceService.getDocumentationRequirementHistory(actorResolver.requireActorMembership(), requirementId).stream()
                .map(ComplianceController::toAuditEventResponse)
                .toList();
    }

    @GetMapping("/patients/{patientId}/history")
    ComplianceHistoryResponse getComplianceHistory(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return toComplianceHistoryResponse(complianceWorkspaceService.getComplianceHistory(
                actorResolver.requireActorMembership(),
                patientId,
                branchId));
    }

    @GetMapping("/patients/{patientId}/history/acknowledgments")
    List<ConsentAcknowledgmentResponse> getAcknowledgmentHistory(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return toComplianceHistoryResponse(complianceWorkspaceService.getComplianceHistory(
                actorResolver.requireActorMembership(),
                patientId,
                branchId)).acknowledgments();
    }

    @GetMapping("/patients/{patientId}/history/certification-periods")
    List<CertificationPeriodResponse> getCertificationHistory(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return toComplianceHistoryResponse(complianceWorkspaceService.getComplianceHistory(
                actorResolver.requireActorMembership(),
                patientId,
                branchId)).certificationPeriods();
    }

    @GetMapping("/patients/{patientId}/history/risk-reminders")
    List<PatientRiskReminderResponse> getRiskReminderHistory(
            @PathVariable UUID patientId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return toComplianceHistoryResponse(complianceWorkspaceService.getComplianceHistory(
                actorResolver.requireActorMembership(),
                patientId,
                branchId)).riskReminders();
    }

    private static ComplianceDashboardAggregateResponse toDashboardAggregateResponse(ComplianceDashboardAggregate aggregate) {
        return new ComplianceDashboardAggregateResponse(
                aggregate.branchId(),
                aggregate.branchName(),
                aggregate.totalPatients(),
                aggregate.readyCount(),
                aggregate.warningCount(),
                aggregate.nonCompliantCount(),
                aggregate.unknownCount(),
                aggregate.activeRiskReminderCount(),
                aggregate.acknowledgmentGapCount());
    }

    private static ComplianceDashboardPatientSummaryResponse toDashboardPatientSummaryResponse(ComplianceDashboardPatientSummary summary) {
        return new ComplianceDashboardPatientSummaryResponse(
                summary.patientId(),
                summary.branchId(),
                summary.branchName(),
                summary.firstName(),
                summary.lastName(),
                summary.readinessStatus(),
                summary.certificationPeriodStatus(),
                summary.activeRiskReminderCount(),
                summary.gapCount(),
                summary.acknowledgmentGapCount(),
                summary.evaluatedAt());
    }

    private static PatientComplianceWorkspaceResponse toPatientComplianceWorkspaceResponse(PatientComplianceWorkspaceView view) {
        return new PatientComplianceWorkspaceResponse(
                view.projection() == null ? null : toStatusProjectionResponse(view.projection()),
                view.results().stream().map(ComplianceController::toChecklistResultResponse).toList(),
                view.acknowledgments().stream().map(ComplianceController::toConsentAcknowledgmentResponse).toList(),
                view.certificationPeriods().stream().map(ComplianceController::toCertificationPeriodResponse).toList(),
                view.reminders().stream().map(ComplianceController::toPatientRiskReminderResponse).toList());
    }

    private static ComplianceStatusProjectionResponse toStatusProjectionResponse(ComplianceStatusProjection projection) {
        return new ComplianceStatusProjectionResponse(
                projection.getId(),
                projection.getPatientId(),
                projection.getBranchId(),
                projection.getServiceLineId(),
                projection.getChecklistPassCount(),
                projection.getChecklistWarningCount(),
                projection.getChecklistFailCount(),
                projection.getDocumentationSatisfiedCount(),
                projection.getDocumentationWarningCount(),
                projection.getDocumentationUnsatisfiedCount(),
                projection.getMissingAcknowledgmentCount(),
                projection.getExpiredAcknowledgmentCount(),
                projection.getCertificationPeriodStatus(),
                projection.getActiveRiskReminderCount(),
                projection.getReadinessStatus(),
                projection.getEvaluatedAt());
    }

    private static ComplianceChecklistResultResponse toChecklistResultResponse(ComplianceChecklistResult result) {
        return new ComplianceChecklistResultResponse(
                result.getId(),
                result.getPatientId(),
                result.getBranchId(),
                result.getServiceLineId(),
                result.getEvaluationCategory(),
                result.getChecklistDefinition() == null ? null : result.getChecklistDefinition().getId(),
                result.getDocumentationRequirement() == null ? null : result.getDocumentationRequirement().getId(),
                result.getResultCode(),
                result.getResultStatus(),
                result.getEvidenceSourceType(),
                result.getEvidenceSourceId(),
                result.getEvidenceSummary(),
                result.getEvaluationOrigin(),
                result.getContextPeriodStart(),
                result.getContextPeriodEnd(),
                result.getSatisfiedByRecordType(),
                result.getSatisfiedByRecordId(),
                result.getMissingReason(),
                result.getEvaluatedAt());
    }

    private static ConsentAcknowledgmentResponse toConsentAcknowledgmentResponse(ConsentAcknowledgmentRecord record) {
        return new ConsentAcknowledgmentResponse(
                record.getId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getAcknowledgmentType(),
                record.getEffectiveAt(),
                record.getExpiresAt(),
                record.getCapturedByMembership() == null ? null : record.getCapturedByMembership().getId(),
                record.getCaptureMethod(),
                record.getSupportingArtifactType(),
                record.getSupportingArtifactId(),
                record.getStatus(),
                record.getRevokedAt());
    }

    private static CertificationPeriodResponse toCertificationPeriodResponse(CertificationPeriodRecord record) {
        return new CertificationPeriodResponse(
                record.getId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getPatientPayerLink() == null ? null : record.getPatientPayerLink().getId(),
                record.getProgramContext(),
                record.getStartDate(),
                record.getEndDate(),
                record.getRecordState(),
                record.getSource());
    }

    private static PatientRiskReminderResponse toPatientRiskReminderResponse(PatientRiskReminder reminder) {
        return new PatientRiskReminderResponse(
                reminder.getId(),
                reminder.getPatientId(),
                reminder.getBranchId(),
                reminder.getVisitOccurrence() == null ? null : reminder.getVisitOccurrence().getId(),
                reminder.getDocumentationRecord() == null ? null : reminder.getDocumentationRecord().getId(),
                reminder.getRiskType(),
                reminder.getSeverityLabel(),
                reminder.getSummary(),
                reminder.getEffectiveAt(),
                reminder.getExpiresAt(),
                reminder.getSourceContextType(),
                reminder.getSourceRecordId(),
                reminder.getStatus(),
                reminder.getResolvedAt());
    }

    private static ComplianceChecklistDefinitionResponse toChecklistDefinitionResponse(ComplianceChecklistDefinition definition) {
        return new ComplianceChecklistDefinitionResponse(
                definition.getId(),
                definition.getBranchId(),
                definition.getServiceLineId(),
                definition.getItemCode(),
                definition.getDescription(),
                definition.getSeverityLabel(),
                definition.getWeightScore(),
                definition.isActive());
    }

    private static RequiredDocumentationRequirementResponse toDocumentationRequirementResponse(RequiredDocumentationRequirement requirement) {
        return new RequiredDocumentationRequirementResponse(
                requirement.getId(),
                requirement.getBranchId(),
                requirement.getServiceLineId(),
                requirement.getSourceCategory(),
                requirement.getRequirementCode(),
                requirement.getDescription(),
                requirement.isPatientApplicable(),
                requirement.isEpisodeApplicable(),
                requirement.getDueDays(),
                requirement.getRecencyDays(),
                requirement.isRequiresSignatureVerification(),
                requirement.isRequiresAttachmentEvidence(),
                requirement.isActive());
    }

    private static ComplianceHistoryResponse toComplianceHistoryResponse(ComplianceHistoryView view) {
        return new ComplianceHistoryResponse(
                view.timeline().stream().map(ComplianceController::toComplianceHistoryEntryResponse).toList(),
                view.acknowledgments().stream().map(ComplianceController::toConsentAcknowledgmentResponse).toList(),
                view.certificationPeriods().stream().map(ComplianceController::toCertificationPeriodResponse).toList(),
                view.reminders().stream().map(ComplianceController::toPatientRiskReminderResponse).toList(),
                view.auditContext().stream().map(ComplianceController::toAuditEventResponse).toList());
    }

    private static ComplianceHistoryEntryResponse toComplianceHistoryEntryResponse(ComplianceHistoryEntry entry) {
        return new ComplianceHistoryEntryResponse(entry.entryType(), entry.entityId(), entry.occurredAt(), entry.summary());
    }

    private static AuditEventResponse toAuditEventResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActionType(),
                event.getTargetType(),
                event.getTargetId(),
                event.getOccurredAt() == null ? null : OffsetDateTime.ofInstant(event.getOccurredAt(), ZoneOffset.UTC),
                event.getBranchId());
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / (double) size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    record RecalculateStatusProjectionRequest(
            UUID branchId,
            UUID serviceLineId,
            @NotNull List<@NotBlank String> requiredAcknowledgmentTypes,
            int certificationExpiryWarningDays,
            OffsetDateTime evaluatedAt) {
    }

    record SaveAcknowledgmentRequest(
            UUID branchId,
            @NotBlank String acknowledgmentType,
            @NotNull OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            UUID capturedByMembershipId,
            String captureMethod,
            String supportingArtifactType,
            UUID supportingArtifactId) {
    }

    record SaveCertificationPeriodRequest(
            UUID branchId,
            UUID patientPayerLinkId,
            String programContext,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            boolean closed,
            String source) {
    }

    record CloseCertificationPeriodRequest(
            UUID branchId,
            UUID patientPayerLinkId,
            String programContext,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String source) {
    }

    record SaveRiskReminderRequest(
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

    record SaveChecklistDefinitionRequest(
            UUID branchId,
            UUID serviceLineId,
            @NotBlank String itemCode,
            @NotBlank String description,
            String severityLabel,
            Integer weightScore,
            boolean active) {
    }

    record SaveDocumentationRequirementRequest(
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

    record ComplianceDashboardAggregateResponse(
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

    record ComplianceDashboardPatientSummaryResponse(
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

    record PatientComplianceWorkspaceResponse(
            ComplianceStatusProjectionResponse projection,
            List<ComplianceChecklistResultResponse> results,
            List<ConsentAcknowledgmentResponse> acknowledgments,
            List<CertificationPeriodResponse> certificationPeriods,
            List<PatientRiskReminderResponse> reminders) {
    }

    record ComplianceStatusProjectionResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
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
            ComplianceReadinessStatus readinessStatus,
            OffsetDateTime evaluatedAt) {
    }

    record ComplianceChecklistResultResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            ComplianceEvaluationCategory evaluationCategory,
            UUID checklistDefinitionId,
            UUID documentationRequirementId,
            String resultCode,
            ComplianceChecklistResultStatus resultStatus,
            String evidenceSourceType,
            UUID evidenceSourceId,
            String evidenceSummary,
            String evaluationOrigin,
            LocalDate contextPeriodStart,
            LocalDate contextPeriodEnd,
            String satisfiedByRecordType,
            UUID satisfiedByRecordId,
            String missingReason,
            OffsetDateTime evaluatedAt) {
    }

    record ConsentAcknowledgmentResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            String acknowledgmentType,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            UUID capturedByMembershipId,
            String captureMethod,
            String supportingArtifactType,
            UUID supportingArtifactId,
            ConsentAcknowledgmentStatus status,
            OffsetDateTime revokedAt) {
    }

    record CertificationPeriodResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID patientPayerLinkId,
            String programContext,
            LocalDate startDate,
            LocalDate endDate,
            Enum<?> recordState,
            String source) {
    }

    record PatientRiskReminderResponse(
            UUID id,
            UUID patientId,
            UUID branchId,
            UUID visitOccurrenceId,
            UUID documentationRecordId,
            String riskType,
            String severityLabel,
            String summary,
            OffsetDateTime effectiveAt,
            OffsetDateTime expiresAt,
            String sourceContextType,
            UUID sourceRecordId,
            PatientRiskReminderStatus status,
            OffsetDateTime resolvedAt) {
    }

    record ComplianceChecklistDefinitionResponse(
            UUID id,
            UUID branchId,
            UUID serviceLineId,
            String itemCode,
            String description,
            String severityLabel,
            Integer weightScore,
            boolean active) {
    }

    record RequiredDocumentationRequirementResponse(
            UUID id,
            UUID branchId,
            UUID serviceLineId,
            String sourceCategory,
            String requirementCode,
            String description,
            boolean patientApplicable,
            boolean episodeApplicable,
            Integer dueDays,
            Integer recencyDays,
            boolean requiresSignatureVerification,
            boolean requiresAttachmentEvidence,
            boolean active) {
    }

    record ComplianceHistoryResponse(
            List<ComplianceHistoryEntryResponse> timeline,
            List<ConsentAcknowledgmentResponse> acknowledgments,
            List<CertificationPeriodResponse> certificationPeriods,
            List<PatientRiskReminderResponse> riskReminders,
            List<AuditEventResponse> auditContext) {
    }

    record ComplianceHistoryEntryResponse(
            String entryType,
            UUID entityId,
            OffsetDateTime occurredAt,
            String summary) {
    }

    record AuditEventResponse(
            UUID id,
            String actionType,
            String targetType,
            UUID targetId,
            OffsetDateTime occurredAt,
            UUID branchId) {
    }
}
