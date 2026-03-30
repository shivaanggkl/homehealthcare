package com.homehealthcare.revenuereadiness.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.domain.VisitDocumentationRecordRepository;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationLink;
import com.homehealthcare.evv.domain.SignatureVerificationLinkRepository;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.evv.domain.VisitExceptionRecordRepository;
import com.homehealthcare.evv.domain.VisitExceptionStatus;
import com.homehealthcare.evv.foundation.EvvComplianceOutcome;
import com.homehealthcare.evv.foundation.EvvVerificationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSessionRepository;
import com.homehealthcare.mobile.foundation.MobileExecutionSessionStatus;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationStatus;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkRepository;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.revenuereadiness.domain.AuthorizationUsageSnapshot;
import com.homehealthcare.revenuereadiness.domain.AuthorizationUsageSnapshotRepository;
import com.homehealthcare.revenuereadiness.domain.InvoiceExportRow;
import com.homehealthcare.revenuereadiness.domain.InvoiceExportRowRepository;
import com.homehealthcare.revenuereadiness.domain.PayerServiceSummaryProjection;
import com.homehealthcare.revenuereadiness.domain.PayerServiceSummaryProjectionRepository;
import com.homehealthcare.revenuereadiness.domain.PayrollExportRow;
import com.homehealthcare.revenuereadiness.domain.PayrollExportRowRepository;
import com.homehealthcare.revenuereadiness.domain.RevenueExceptionFlag;
import com.homehealthcare.revenuereadiness.domain.RevenueExceptionFlagRepository;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjection;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjectionRepository;
import com.homehealthcare.revenuereadiness.domain.SignedVisitValidationResult;
import com.homehealthcare.revenuereadiness.domain.SignedVisitValidationResultRepository;
import com.homehealthcare.revenuereadiness.domain.VisitCompletionValidationResult;
import com.homehealthcare.revenuereadiness.domain.VisitCompletionValidationResultRepository;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionSeverity;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionTargetType;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionType;
import com.homehealthcare.revenuereadiness.foundation.RevenueExportLifecycleStatus;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessAuditService;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import com.homehealthcare.revenuereadiness.foundation.RevenueUsagePosture;
import com.homehealthcare.revenuereadiness.foundation.RevenueValidationOutcome;
import com.homehealthcare.review.domain.ReviewWorkItemRepository;
import com.homehealthcare.review.foundation.ReviewLifecycleStatus;
import com.homehealthcare.review.foundation.ReviewSourceType;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.serviceline.domain.ServiceLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
public class RevenueReadinessService {

    private static final Set<DocumentationRecordStatus> SATISFYING_DOCUMENTATION_STATUSES = Set.of(
            DocumentationRecordStatus.SUBMITTED,
            DocumentationRecordStatus.AMENDED,
            DocumentationRecordStatus.LOCKED);
    private static final Set<PatientPayerLinkStatus> ACTIVE_PAYER_STATUSES = Set.of(
            PatientPayerLinkStatus.ACTIVE,
            PatientPayerLinkStatus.PENDING);
    private static final Set<PatientEpisodeAuthorizationStatus> ACTIVE_AUTHORIZATION_STATUSES = Set.of(
            PatientEpisodeAuthorizationStatus.ACTIVE,
            PatientEpisodeAuthorizationStatus.PENDING);
    private static final Set<ReviewLifecycleStatus> RETURN_BLOCKING_STATUSES = Set.of(
            ReviewLifecycleStatus.RETURNED_FOR_FIX,
            ReviewLifecycleStatus.RESUBMITTED);
    private static final Set<VisitExceptionStatus> ACTIVE_VISIT_EXCEPTION_STATUSES = EnumSet.of(
            VisitExceptionStatus.OPEN,
            VisitExceptionStatus.ACKNOWLEDGED,
            VisitExceptionStatus.ESCALATED);

    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final VisitDocumentationRecordRepository visitDocumentationRecordRepository;
    private final MobileVisitExecutionSessionRepository mobileVisitExecutionSessionRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final SignatureVerificationLinkRepository signatureVerificationLinkRepository;
    private final VisitExceptionRecordRepository visitExceptionRecordRepository;
    private final ReviewWorkItemRepository reviewWorkItemRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final PatientPayerLinkRepository patientPayerLinkRepository;
    private final PatientEpisodeAuthorizationRepository patientEpisodeAuthorizationRepository;
    private final BranchRepository branchRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final VisitCompletionValidationResultRepository visitCompletionValidationResultRepository;
    private final SignedVisitValidationResultRepository signedVisitValidationResultRepository;
    private final RevenueExceptionFlagRepository revenueExceptionFlagRepository;
    private final AuthorizationUsageSnapshotRepository authorizationUsageSnapshotRepository;
    private final PayerServiceSummaryProjectionRepository payerServiceSummaryProjectionRepository;
    private final RevenueReadinessProjectionRepository revenueReadinessProjectionRepository;
    private final PayrollExportRowRepository payrollExportRowRepository;
    private final InvoiceExportRowRepository invoiceExportRowRepository;
    private final AuditEventRepository auditEventRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final RevenueReadinessAuditService revenueReadinessAuditService;

    @Transactional
    public VisitCompletionValidationResult recalculateVisitCompletionValidation(
            @NotNull AgencyMembership actorMembership,
            @Valid RecalculateVisitValidationCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECALCULATE_REVENUE_READINESS, "recalculate visit completion validation");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), command.visitOccurrenceId());
        requireBranchAccess(actorMembership, AgencyPermission.RECALCULATE_REVENUE_READINESS, visit.getBranchId(), "recalculate visit completion validation");

        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());
        MobileVisitExecutionSession executionSession = latestExecutionSession(visit.getId());
        boolean executionCompleted = executionSession != null && executionSession.getExecutionStatus() == MobileExecutionSessionStatus.COMPLETED;
        VisitDocumentationRecord documentationRecord = latestDocumentationRecord(visit.getId());
        boolean documentationSubmitted = documentationRecord != null && SATISFYING_DOCUMENTATION_STATUSES.contains(documentationRecord.getStatus());
        boolean unresolvedReviewReturn = documentationRecord != null
                && reviewWorkItemRepository.existsByAgency_IdAndSourceTypeAndSourceRecordIdAndStatusIn(
                        actorMembership.getAgencyId(),
                        ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                        documentationRecord.getId(),
                        RETURN_BLOCKING_STATUSES);
        EvvVerificationSession verificationSession = latestVerificationSession(visit.getId());
        boolean criticalExceptionOpen = verificationSession != null
                && visitExceptionRecordRepository.countByVerificationSession_IdAndStatusIn(
                        verificationSession.getId(),
                        ACTIVE_VISIT_EXCEPTION_STATUSES) > 0;

        RevenueValidationOutcome outcome;
        String reasonCode;
        String summary;
        if (!executionCompleted) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "EXECUTION_NOT_COMPLETED";
            summary = "Visit execution has not completed.";
        } else if (visit.getStatus() == com.homehealthcare.scheduling.foundation.SchedulingVisitStatus.CANCELLED) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "VISIT_CANCELLED";
            summary = "Cancelled visits cannot be treated as revenue ready.";
        } else if (!documentationSubmitted) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "DOCUMENTATION_NOT_SUBMITTED";
            summary = "Required visit documentation has not been submitted.";
        } else if (unresolvedReviewReturn) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "REVIEW_RETURN_OPEN";
            summary = "The visit documentation is still returned for correction.";
        } else if (criticalExceptionOpen) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "CRITICAL_EXCEPTION_OPEN";
            summary = "Open EVV visit exceptions still block revenue readiness.";
        } else if (visit.getStatus() == com.homehealthcare.scheduling.foundation.SchedulingVisitStatus.OPEN_SHIFT) {
            outcome = RevenueValidationOutcome.WARNING;
            reasonCode = "OPEN_SHIFT_STATE_REMAINS";
            summary = "The visit is complete, but the schedule still shows open-shift state.";
        } else {
            outcome = RevenueValidationOutcome.PASS;
            reasonCode = "COMPLETE";
            summary = "The visit is complete for revenue-readiness purposes.";
        }

        VisitCompletionValidationResult result = visitCompletionValidationResultRepository.findByVisitOccurrence_Id(visit.getId())
                .map(existing -> {
                    existing.recalculate(
                            documentationRecord,
                            outcome,
                            reasonCode,
                            summary,
                            executionCompleted,
                            documentationSubmitted,
                            unresolvedReviewReturn,
                            criticalExceptionOpen,
                            evaluatedAt);
                    return existing;
                })
                .orElseGet(() -> VisitCompletionValidationResult.create(
                        visit,
                        visit.getPatient(),
                        resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                        documentationRecord,
                        outcome,
                        reasonCode,
                        summary,
                        executionCompleted,
                        documentationSubmitted,
                        unresolvedReviewReturn,
                        criticalExceptionOpen,
                        evaluatedAt));
        return visitCompletionValidationResultRepository.saveAndFlush(result);
    }

    @Transactional
    public SignedVisitValidationResult recalculateSignedVisitValidation(
            @NotNull AgencyMembership actorMembership,
            @Valid RecalculateVisitValidationCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECALCULATE_REVENUE_READINESS, "recalculate signed-visit validation");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), command.visitOccurrenceId());
        requireBranchAccess(actorMembership, AgencyPermission.RECALCULATE_REVENUE_READINESS, visit.getBranchId(), "recalculate signed-visit validation");

        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());
        EvvVerificationSession verificationSession = latestVerificationSession(visit.getId());
        SignatureVerificationStatus caregiverSignature = SignatureVerificationStatus.MISSING;
        SignatureVerificationStatus patientSignature = SignatureVerificationStatus.MISSING;
        EvvVerificationStatus evvVerificationStatus = verificationSession == null ? null : verificationSession.getVerificationStatus();
        if (verificationSession != null) {
            for (SignatureVerificationLink link : signatureVerificationLinkRepository.findAllByVerificationSession_IdOrderByRecordedAtAsc(verificationSession.getId())) {
                if (link.getSignerRole() == SignatureSignerRole.CAREGIVER) {
                    caregiverSignature = link.getVerificationStatus();
                } else if (link.getSignerRole() == SignatureSignerRole.PATIENT || link.getSignerRole() == SignatureSignerRole.REPRESENTATIVE) {
                    patientSignature = link.getVerificationStatus();
                }
            }
        }
        SignatureVerificationStatus finalCaregiverSignature = caregiverSignature;
        SignatureVerificationStatus finalPatientSignature = patientSignature;

        RevenueValidationOutcome outcome;
        String reasonCode;
        String summary;
        if (verificationSession == null) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "EVV_SESSION_MISSING";
            summary = "No EVV verification session is available for the visit.";
        } else if (caregiverSignature != SignatureVerificationStatus.PRESENT) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "CAREGIVER_SIGNATURE_MISSING";
            summary = "A caregiver signature is required before the visit is revenue ready.";
        } else if (evvVerificationStatus == EvvVerificationStatus.EXCEPTION_OPEN
                || evvVerificationStatus == EvvVerificationStatus.MISSED_VISIT_REPORTED
                || evvVerificationStatus == EvvVerificationStatus.ESCALATED) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "EVV_VERIFICATION_BLOCKED";
            summary = "The EVV verification state still blocks the signed-visit gate.";
        } else if (patientSignature == SignatureVerificationStatus.MISSING) {
            outcome = RevenueValidationOutcome.FAIL;
            reasonCode = "PATIENT_SIGNATURE_MISSING";
            summary = "A patient or representative signature is still missing.";
        } else if (patientSignature == SignatureVerificationStatus.REFUSED || patientSignature == SignatureVerificationStatus.NOT_APPLICABLE) {
            outcome = RevenueValidationOutcome.WARNING;
            reasonCode = "PATIENT_SIGNATURE_EXCEPTION";
            summary = "Patient signature was not collected, but the visit retains supporting signature context.";
        } else if (verificationSession.getComplianceOutcome() == EvvComplianceOutcome.READY_WITH_WARNING
                || evvVerificationStatus == EvvVerificationStatus.VERIFIED_WITH_WARNING) {
            outcome = RevenueValidationOutcome.WARNING;
            reasonCode = "EVV_WARNING";
            summary = "The visit is signed, but EVV verification still carries a warning.";
        } else {
            outcome = RevenueValidationOutcome.PASS;
            reasonCode = "SIGNED";
            summary = "The visit has the signatures and EVV verification needed for revenue readiness.";
        }

        SignedVisitValidationResult result = signedVisitValidationResultRepository.findByVisitOccurrence_Id(visit.getId())
                .map(existing -> {
                    existing.recalculate(
                            verificationSession,
                            outcome,
                            reasonCode,
                            summary,
                            finalCaregiverSignature,
                            finalPatientSignature,
                            evvVerificationStatus,
                            evaluatedAt);
                    return existing;
                })
                .orElseGet(() -> SignedVisitValidationResult.create(
                        visit,
                        visit.getPatient(),
                        resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                        verificationSession,
                        outcome,
                        reasonCode,
                        summary,
                        finalCaregiverSignature,
                        finalPatientSignature,
                        evvVerificationStatus,
                        evaluatedAt));
        return signedVisitValidationResultRepository.saveAndFlush(result);
    }

    @Transactional
    public PayerServiceSummaryProjection refreshPayerServiceSummary(
            @NotNull AgencyMembership actorMembership,
            @Valid RefreshVisitProjectionCommand command) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES, "refresh payer and service summaries");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), command.visitOccurrenceId());
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES, visit.getBranchId(), "refresh payer and service summaries");

        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());
        PatientPayerLink payerLink = selectPayerLink(visit);
        PatientEpisodeAuthorization authorization = selectAuthorization(visit);
        ServiceLine serviceLine = visit.getServiceLine();
        Branch branch = resolveBranch(actorMembership.getAgencyId(), visit.getBranchId());

        PayerServiceSummaryProjection summary = payerServiceSummaryProjectionRepository.findByVisitOccurrence_Id(visit.getId())
                .map(existing -> {
                    existing.refresh(
                            branch,
                            serviceLine,
                            payerLink,
                            authorization,
                            payerLink == null ? null : payerLink.getPayerName(),
                            payerLink == null ? null : payerLink.getPayerExternalId(),
                            payerLink == null ? null : payerLink.getMemberPolicyNumber(),
                            authorization == null ? null : authorization.getAuthorizationNumber(),
                            serviceLine == null ? null : serviceLine.getCode(),
                            serviceLine == null ? null : serviceLine.getName(),
                            payerLink != null && payerLink.isPrimaryPayer(),
                            evaluatedAt);
                    return existing;
                })
                .orElseGet(() -> PayerServiceSummaryProjection.create(
                        visit,
                        visit.getPatient(),
                        branch,
                        serviceLine,
                        payerLink,
                        authorization,
                        payerLink == null ? null : payerLink.getPayerName(),
                        payerLink == null ? null : payerLink.getPayerExternalId(),
                        payerLink == null ? null : payerLink.getMemberPolicyNumber(),
                        authorization == null ? null : authorization.getAuthorizationNumber(),
                        serviceLine == null ? null : serviceLine.getCode(),
                        serviceLine == null ? null : serviceLine.getName(),
                        payerLink != null && payerLink.isPrimaryPayer(),
                        evaluatedAt));
        PayerServiceSummaryProjection saved = payerServiceSummaryProjectionRepository.saveAndFlush(summary);
        revenueReadinessAuditService.recordPayerServiceSummaryRefreshed(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"visitOccurrenceId\":\"" + visit.getId() + "\",\"authorizationId\":\"" + saved.getAuthorizationId() + "\"}");
        return saved;
    }

    @Transactional
    public AuthorizationUsageSnapshot refreshAuthorizationUsage(
            @NotNull AgencyMembership actorMembership,
            @Valid RefreshAuthorizationUsageCommand command) {
        requirePermission(actorMembership, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES, "refresh authorization usage");
        PatientEpisodeAuthorization authorization = resolveAuthorization(actorMembership.getAgencyId(), command.authorizationId());
        ServiceLine serviceLine = authorization.getServiceLine();
        UUID branchId = resolveBranchIdForAuthorization(authorization);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES, branchId, "refresh authorization usage");

        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());
        int usedUnits = countCompletedVisitsForAuthorization(authorization);
        Integer authorizedUnits = authorization.getAuthorizedUnits();
        Integer remainingUnits = authorizedUnits == null ? null : authorizedUnits - usedUnits;
        RevenueUsagePosture posture = deriveUsagePosture(authorization, usedUnits, remainingUnits);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), branchId);

        AuthorizationUsageSnapshot snapshot = authorizationUsageSnapshotRepository.findByAuthorization_Id(authorization.getId())
                .map(existing -> {
                    existing.refresh(
                            branch,
                            serviceLine,
                            authorization,
                            authorizedUnits,
                            usedUnits,
                            remainingUnits,
                            posture,
                            usedUnits,
                            evaluatedAt);
                    return existing;
                })
                .orElseGet(() -> AuthorizationUsageSnapshot.create(
                        authorization.getPatient(),
                        branch,
                        serviceLine,
                        authorization,
                        authorizedUnits,
                        usedUnits,
                        remainingUnits,
                        posture,
                        usedUnits,
                        evaluatedAt));
        AuthorizationUsageSnapshot saved = authorizationUsageSnapshotRepository.saveAndFlush(snapshot);
        revenueReadinessAuditService.recordAuthorizationUsageRefreshed(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"authorizationId\":\"" + authorization.getId() + "\",\"usedUnits\":" + usedUnits + "}");
        return saved;
    }

    @Transactional
    public RevenueReadinessProjection recalculateRevenueReadiness(
            @NotNull AgencyMembership actorMembership,
            @Valid RefreshVisitProjectionCommand command) {
        requirePermission(actorMembership, AgencyPermission.RECALCULATE_REVENUE_READINESS, "recalculate revenue readiness");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), command.visitOccurrenceId());
        requireBranchAccess(actorMembership, AgencyPermission.RECALCULATE_REVENUE_READINESS, visit.getBranchId(), "recalculate revenue readiness");

        VisitCompletionValidationResult completion = recalculateVisitCompletionValidation(actorMembership, new RecalculateVisitValidationCommand(
                visit.getId(),
                command.evaluatedAt()));
        SignedVisitValidationResult signature = recalculateSignedVisitValidation(actorMembership, new RecalculateVisitValidationCommand(
                visit.getId(),
                command.evaluatedAt()));
        PayerServiceSummaryProjection summary = refreshPayerServiceSummary(actorMembership, command);
        AuthorizationUsageSnapshot authorizationUsage = summary.getAuthorizationId() == null
                ? null
                : refreshAuthorizationUsage(actorMembership, new RefreshAuthorizationUsageCommand(summary.getAuthorizationId(), command.evaluatedAt()));

        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());
        List<RevenueExceptionFlag> activeFlags = reconcileExceptionFlags(actorMembership, visit, completion, signature, summary, authorizationUsage, evaluatedAt);
        long blockingFlags = activeFlags.stream().filter(flag -> flag.getSeverity() == RevenueExceptionSeverity.BLOCKING).count();
        int warningFlags = (int) activeFlags.stream().filter(flag -> flag.getSeverity() == RevenueExceptionSeverity.WARNING).count();

        RevenueReadinessStatus readinessStatus;
        if (completion.getOutcome() == RevenueValidationOutcome.FAIL || signature.getOutcome() == RevenueValidationOutcome.FAIL || blockingFlags > 0) {
            readinessStatus = RevenueReadinessStatus.BLOCKED;
        } else if (completion.getOutcome() == RevenueValidationOutcome.WARNING
                || signature.getOutcome() == RevenueValidationOutcome.WARNING
                || warningFlags > 0
                || (authorizationUsage != null && authorizationUsage.getUsagePosture() == RevenueUsagePosture.NEAR_LIMIT)) {
            readinessStatus = RevenueReadinessStatus.WARNING;
        } else {
            readinessStatus = RevenueReadinessStatus.READY;
        }

        RevenueReadinessProjection projection = revenueReadinessProjectionRepository.findByVisitOccurrence_Id(visit.getId())
                .map(existing -> {
                    existing.recalculate(
                            resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                            visit.getServiceLine(),
                            completion,
                            signature,
                            summary,
                            authorizationUsage,
                            readinessStatus,
                            activeFlags.size(),
                            warningFlags,
                            existing.getExportLifecycleStatus(),
                            evaluatedAt);
                    return existing;
                })
                .orElseGet(() -> RevenueReadinessProjection.create(
                        visit,
                        visit.getPatient(),
                        resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                        visit.getServiceLine(),
                        completion,
                        signature,
                        summary,
                        authorizationUsage,
                        readinessStatus,
                        activeFlags.size(),
                        warningFlags,
                        RevenueExportLifecycleStatus.NOT_REQUESTED,
                        evaluatedAt));
        RevenueReadinessProjection saved = revenueReadinessProjectionRepository.saveAndFlush(projection);
        revenueReadinessAuditService.recordReadinessRecalculated(
                actorMembership,
                saved.getId(),
                saved.getBranchId(),
                "{\"visitOccurrenceId\":\"" + visit.getId() + "\",\"readinessStatus\":\"" + saved.getReadinessStatus() + "\"}");
        return saved;
    }

    @Transactional
    public PayrollExportRow generatePayrollExportRow(
            @NotNull AgencyMembership actorMembership,
            @Valid GenerateExportCommand command) {
        requirePermission(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, "generate payroll exports");
        RevenueReadinessProjection projection = recalculateRevenueReadiness(actorMembership, new RefreshVisitProjectionCommand(
                command.visitOccurrenceId(),
                command.generatedAt()));
        if (projection.getReadinessStatus() == RevenueReadinessStatus.BLOCKED && !command.allowBlocked()) {
            throw new RevenueReadinessConflictException("Blocked visits cannot be exported without explicit override.");
        }
        VisitOccurrence visit = projection.getVisitOccurrence();
        requireBranchAccess(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, visit.getBranchId(), "generate payroll exports");
        MobileVisitExecutionSession execution = latestExecutionSession(visit.getId());
        CaregiverVisitAssignment assignment = latestActiveAssignment(visit.getId());
        OffsetDateTime generatedAt = evaluatedAt(command.generatedAt());
        int durationMinutes = computeDurationMinutes(visit, execution);
        PayrollExportRow row = payrollExportRowRepository.saveAndFlush(PayrollExportRow.create(
                visit,
                visit.getPatient(),
                assignment == null ? null : assignment.getCaregiverProfile(),
                resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                visit.getServiceLine(),
                visit.getPlannedStartAt(),
                visit.getPlannedEndAt(),
                execution == null ? null : execution.getStartedAt(),
                execution == null ? null : execution.getEndedAt(),
                durationMinutes,
                projection.getReadinessStatus(),
                RevenueExportLifecycleStatus.GENERATED,
                generatedAt));
        projection.markExportLifecycleStatus(RevenueExportLifecycleStatus.GENERATED, generatedAt);
        revenueReadinessProjectionRepository.saveAndFlush(projection);
        revenueReadinessAuditService.recordExportGenerated(
                actorMembership,
                com.homehealthcare.revenuereadiness.foundation.Epic14RevenueReadinessTargetType.PAYROLL_EXPORT_ROW,
                row.getId(),
                projection.getBranchId(),
                "{\"visitOccurrenceId\":\"" + visit.getId() + "\",\"durationMinutes\":" + durationMinutes + "}");
        return row;
    }

    @Transactional
    public InvoiceExportRow generateInvoiceExportRow(
            @NotNull AgencyMembership actorMembership,
            @Valid GenerateExportCommand command) {
        requirePermission(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, "generate invoice exports");
        RevenueReadinessProjection projection = recalculateRevenueReadiness(actorMembership, new RefreshVisitProjectionCommand(
                command.visitOccurrenceId(),
                command.generatedAt()));
        if (projection.getReadinessStatus() == RevenueReadinessStatus.BLOCKED && !command.allowBlocked()) {
            throw new RevenueReadinessConflictException("Blocked visits cannot be exported without explicit override.");
        }
        VisitOccurrence visit = projection.getVisitOccurrence();
        requireBranchAccess(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, visit.getBranchId(), "generate invoice exports");
        MobileVisitExecutionSession execution = latestExecutionSession(visit.getId());
        CaregiverVisitAssignment assignment = latestActiveAssignment(visit.getId());
        OffsetDateTime generatedAt = evaluatedAt(command.generatedAt());
        PayerServiceSummaryProjection summary = projection.getPayerServiceSummary();
        int billableUnits = 1;
        InvoiceExportRow row = invoiceExportRowRepository.saveAndFlush(InvoiceExportRow.create(
                visit,
                visit.getPatient(),
                assignment == null ? null : assignment.getCaregiverProfile(),
                resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                visit.getServiceLine(),
                summary == null ? null : summary.getPatientPayerLink(),
                summary == null ? null : summary.getAuthorization(),
                summary == null ? null : summary.getPayerName(),
                summary == null ? null : summary.getAuthorizationNumber(),
                visit.getPlannedStartAt(),
                visit.getPlannedEndAt(),
                execution == null ? null : execution.getStartedAt(),
                execution == null ? null : execution.getEndedAt(),
                billableUnits,
                projection.getReadinessStatus(),
                RevenueExportLifecycleStatus.GENERATED,
                generatedAt));
        projection.markExportLifecycleStatus(RevenueExportLifecycleStatus.GENERATED, generatedAt);
        revenueReadinessProjectionRepository.saveAndFlush(projection);
        revenueReadinessAuditService.recordExportGenerated(
                actorMembership,
                com.homehealthcare.revenuereadiness.foundation.Epic14RevenueReadinessTargetType.INVOICE_EXPORT_ROW,
                row.getId(),
                projection.getBranchId(),
                "{\"visitOccurrenceId\":\"" + visit.getId() + "\",\"billableUnits\":" + billableUnits + "}");
        return row;
    }

    @Transactional(readOnly = true)
    public Optional<RevenueReadinessProjection> findReadinessByVisit(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, "view revenue readiness");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, visit.getBranchId(), "view revenue readiness");
        return revenueReadinessProjectionRepository.findByVisitOccurrence_Id(visitOccurrenceId);
    }

    @Transactional(readOnly = true)
    public List<RevenueReadinessProjection> listReadiness(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            RevenueReadinessStatus readinessStatus,
            RevenueExceptionType exceptionType,
            String payer,
            UUID serviceLineId,
            OffsetDateTime from,
            OffsetDateTime to) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, "list revenue readiness");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, branchId, "list revenue readiness");
        }
        String normalizedPayer = normalizeOptional(payer);
        return revenueReadinessProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(projection -> branchId == null || Objects.equals(branchId, projection.getBranchId()))
                .filter(projection -> readinessStatus == null || projection.getReadinessStatus() == readinessStatus)
                .filter(projection -> serviceLineId == null || (projection.getServiceLine() != null
                        && Objects.equals(projection.getServiceLine().getId(), serviceLineId)))
                .filter(projection -> from == null || !projection.getVisitOccurrence().getPlannedStartAt().isBefore(from))
                .filter(projection -> to == null || !projection.getVisitOccurrence().getPlannedStartAt().isAfter(to))
                .filter(projection -> normalizedPayer == null || (projection.getPayerServiceSummary() != null
                        && projection.getPayerServiceSummary().getPayerName() != null
                        && projection.getPayerServiceSummary().getPayerName().equalsIgnoreCase(normalizedPayer)))
                .filter(projection -> exceptionType == null || revenueExceptionFlagRepository
                        .findAllByTargetTypeAndTargetIdOrderByDetectedAtDesc(RevenueExceptionTargetType.VISIT, projection.getVisitOccurrenceId()).stream()
                        .anyMatch(flag -> flag.isActive() && flag.getExceptionType() == exceptionType))
                .filter(projection -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, projection.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RevenueExceptionFlag> listExceptionFlags(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            RevenueExceptionType exceptionType,
            String payer,
            UUID serviceLineId,
            OffsetDateTime from,
            OffsetDateTime to) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, "list revenue exception flags");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, branchId, "list revenue exception flags");
        }
        String normalizedPayer = normalizeOptional(payer);
        return revenueExceptionFlagRepository.findAllByAgency_IdOrderByDetectedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(flag -> branchId == null || Objects.equals(branchId, flag.getBranchId()))
                .filter(flag -> exceptionType == null || flag.getExceptionType() == exceptionType)
                .filter(flag -> from == null || !flag.getDetectedAt().isBefore(from))
                .filter(flag -> to == null || !flag.getDetectedAt().isAfter(to))
                .filter(flag -> {
                    if (serviceLineId == null && normalizedPayer == null) {
                        return true;
                    }
                    if (flag.getTargetType() != RevenueExceptionTargetType.VISIT) {
                        return serviceLineId == null && normalizedPayer == null;
                    }
                    return payerServiceSummaryProjectionRepository.findByVisitOccurrence_Id(flag.getTargetId())
                            .map(summary -> (serviceLineId == null || (summary.getServiceLine() != null
                                    && Objects.equals(summary.getServiceLine().getId(), serviceLineId)))
                                    && (normalizedPayer == null || (summary.getPayerName() != null
                                    && summary.getPayerName().equalsIgnoreCase(normalizedPayer))))
                            .orElse(false);
                })
                .filter(flag -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, flag.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PayerServiceSummaryProjection getPayerServiceSummary(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES, "view payer and service summary");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES, visit.getBranchId(), "view payer and service summary");
        return payerServiceSummaryProjectionRepository.findByVisitOccurrence_Id(visitOccurrenceId)
                .orElseThrow(() -> new RevenueReadinessEntityNotFoundException("PayerServiceSummaryProjection", visitOccurrenceId));
    }

    @Transactional(readOnly = true)
    public AuthorizationUsageSnapshot getAuthorizationUsage(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID authorizationId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES, "view authorization usage");
        PatientEpisodeAuthorization authorization = resolveAuthorization(actorMembership.getAgencyId(), authorizationId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES, resolveBranchIdForAuthorization(authorization), "view authorization usage");
        return authorizationUsageSnapshotRepository.findByAuthorization_Id(authorizationId)
                .orElseThrow(() -> new RevenueReadinessEntityNotFoundException("AuthorizationUsageSnapshot", authorizationId));
    }

    @Transactional
    public ExportPreview previewPayrollExport(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, "preview payroll exports");
        RevenueReadinessProjection projection = recalculateRevenueReadiness(actorMembership, new RefreshVisitProjectionCommand(visitOccurrenceId, null));
        CaregiverVisitAssignment assignment = latestActiveAssignment(visitOccurrenceId);
        MobileVisitExecutionSession execution = latestExecutionSession(visitOccurrenceId);
        return new ExportPreview(
                projection.getVisitOccurrenceId(),
                projection.getReadinessStatus(),
                projection.getPayerServiceSummary() == null ? null : projection.getPayerServiceSummary().getPayerName(),
                assignment == null ? null : assignment.getCaregiverProfileId(),
                computeDurationMinutes(projection.getVisitOccurrence(), execution),
                projection.getReadinessStatus() != RevenueReadinessStatus.BLOCKED,
                projection.getReadinessStatus() == RevenueReadinessStatus.BLOCKED ? "Visit is blocked for payroll export." : null);
    }

    @Transactional
    public ExportPreview previewInvoiceExport(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, "preview invoice exports");
        RevenueReadinessProjection projection = recalculateRevenueReadiness(actorMembership, new RefreshVisitProjectionCommand(visitOccurrenceId, null));
        return new ExportPreview(
                projection.getVisitOccurrenceId(),
                projection.getReadinessStatus(),
                projection.getPayerServiceSummary() == null ? null : projection.getPayerServiceSummary().getPayerName(),
                null,
                1,
                projection.getReadinessStatus() != RevenueReadinessStatus.BLOCKED,
                projection.getReadinessStatus() == RevenueReadinessStatus.BLOCKED ? "Visit is blocked for invoice export." : null);
    }

    @Transactional(readOnly = true)
    public RevenueHistoryView getRevenueHistory(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, "view revenue readiness history");
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, visit.getBranchId(), "view revenue readiness history");
        List<RevenueExceptionFlag> flags = revenueExceptionFlagRepository.findAllByTargetTypeAndTargetIdOrderByDetectedAtDesc(
                RevenueExceptionTargetType.VISIT,
                visitOccurrenceId);
        List<AuditEvent> auditEvents = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> Objects.equals(event.getTargetId(), visitOccurrenceId)
                        || (event.getTargetType().equals(com.homehealthcare.revenuereadiness.foundation.Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION.name())
                        && revenueReadinessProjectionRepository.findByVisitOccurrence_Id(visitOccurrenceId)
                        .map(projection -> Objects.equals(event.getTargetId(), projection.getId()))
                        .orElse(false))
                        || flags.stream().anyMatch(flag -> Objects.equals(flag.getId(), event.getTargetId())))
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt))
                .toList();
        List<PayrollExportRow> payrollRows = payrollExportRowRepository.findAllByAgency_IdOrderByGeneratedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(row -> Objects.equals(row.getVisitOccurrence().getId(), visitOccurrenceId))
                .toList();
        List<InvoiceExportRow> invoiceRows = invoiceExportRowRepository.findAllByAgency_IdOrderByGeneratedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(row -> Objects.equals(row.getVisitOccurrence().getId(), visitOccurrenceId))
                .toList();
        return new RevenueHistoryView(
                revenueReadinessProjectionRepository.findByVisitOccurrence_Id(visitOccurrenceId).orElse(null),
                flags,
                payrollRows,
                invoiceRows,
                auditEvents);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listExportHistory(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, "view export history");
        return auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> event.getActionType().equals("REVENUE_EXPORT_GENERATED"))
                .filter(event -> hasBranchAccess(actorMembership, AgencyPermission.GENERATE_REVENUE_EXPORTS, event.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listExceptionHistory(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, "view revenue exception history");
        return auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> event.getActionType().equals("REVENUE_EXCEPTION_FLAG_UPDATED"))
                .filter(event -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE, event.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listAuthorizationUsageHistory(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID authorizationId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES, "view authorization usage history");
        PatientEpisodeAuthorization authorization = resolveAuthorization(actorMembership.getAgencyId(), authorizationId);
        UUID branchId = resolveBranchIdForAuthorization(authorization);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES, branchId, "view authorization usage history");
        return auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> event.getActionType().equals("REVENUE_AUTHORIZATION_USAGE_REFRESHED"))
                .filter(event -> authorizationUsageSnapshotRepository.findByAuthorization_Id(authorizationId)
                        .map(snapshot -> Objects.equals(event.getTargetId(), snapshot.getId()))
                        .orElse(false))
                .toList();
    }

    private List<RevenueExceptionFlag> reconcileExceptionFlags(
            AgencyMembership actorMembership,
            VisitOccurrence visit,
            VisitCompletionValidationResult completion,
            SignedVisitValidationResult signature,
            PayerServiceSummaryProjection summary,
            AuthorizationUsageSnapshot authorizationUsage,
            OffsetDateTime detectedAt) {
        List<RevenueExceptionFlag> activeFlags = new ArrayList<>();
        reconcileVisitFlag(actorMembership, visit, completion.getOutcome() == RevenueValidationOutcome.FAIL,
                RevenueExceptionType.INCOMPLETE_VISIT, RevenueExceptionSeverity.BLOCKING, completion.getReasonCode(), completion.getSummary(), detectedAt)
                .ifPresent(activeFlags::add);
        reconcileVisitFlag(actorMembership, visit, !completion.isDocumentationSubmitted(),
                RevenueExceptionType.MISSING_DOCUMENTATION, RevenueExceptionSeverity.BLOCKING, "DOCUMENTATION_NOT_SUBMITTED", "Required visit documentation is missing or not submitted.", detectedAt)
                .ifPresent(activeFlags::add);
        reconcileVisitFlag(actorMembership, visit, signature.getOutcome() == RevenueValidationOutcome.FAIL,
                RevenueExceptionType.MISSING_SIGNATURE, RevenueExceptionSeverity.BLOCKING, signature.getReasonCode(), signature.getSummary(), detectedAt)
                .ifPresent(activeFlags::add);

        boolean payerMismatch = summary.getPatientPayerLink() == null || visit.getServiceLine() == null;
        reconcileVisitFlag(actorMembership, visit, payerMismatch,
                RevenueExceptionType.PAYER_SERVICE_MISMATCH,
                RevenueExceptionSeverity.WARNING,
                payerMismatch && summary.getPatientPayerLink() == null ? "PAYER_MISSING" : "SERVICE_LINE_MISSING",
                payerMismatch && summary.getPatientPayerLink() == null
                        ? "No active payer linkage was found for the visit date."
                        : "The visit is missing service-line context required for invoicing.",
                detectedAt)
                .ifPresent(activeFlags::add);

        boolean authorizationProblem = authorizationUsage == null
                || authorizationUsage.getUsagePosture() == RevenueUsagePosture.MISSING_AUTHORIZATION
                || authorizationUsage.getUsagePosture() == RevenueUsagePosture.OVER_LIMIT;
        Optional<RevenueExceptionFlag> authorizationFlag = reconcileAuthorizationFlag(
                actorMembership,
                visit,
                authorizationUsage,
                authorizationProblem,
                detectedAt);
        authorizationFlag.ifPresent(activeFlags::add);

        return activeFlags;
    }

    private Optional<RevenueExceptionFlag> reconcileVisitFlag(
            AgencyMembership actorMembership,
            VisitOccurrence visit,
            boolean active,
            RevenueExceptionType exceptionType,
            RevenueExceptionSeverity severity,
            String reasonCode,
            String summary,
            OffsetDateTime detectedAt) {
        return reconcileFlag(
                actorMembership,
                RevenueExceptionTargetType.VISIT,
                visit.getId(),
                resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                active,
                exceptionType,
                severity,
                reasonCode,
                summary,
                detectedAt);
    }

    private Optional<RevenueExceptionFlag> reconcileAuthorizationFlag(
            AgencyMembership actorMembership,
            VisitOccurrence visit,
            AuthorizationUsageSnapshot authorizationUsage,
            boolean active,
            OffsetDateTime detectedAt) {
        UUID targetId = authorizationUsage != null && authorizationUsage.getAuthorizationId() != null
                ? authorizationUsage.getAuthorizationId()
                : visit.getId();
        RevenueExceptionTargetType targetType = authorizationUsage != null && authorizationUsage.getAuthorizationId() != null
                ? RevenueExceptionTargetType.AUTHORIZATION
                : RevenueExceptionTargetType.VISIT;
        String reasonCode = authorizationUsage == null || authorizationUsage.getUsagePosture() == RevenueUsagePosture.MISSING_AUTHORIZATION
                ? "AUTHORIZATION_MISSING"
                : "AUTHORIZATION_OVER_LIMIT";
        String summary = authorizationUsage == null || authorizationUsage.getUsagePosture() == RevenueUsagePosture.MISSING_AUTHORIZATION
                ? "No active authorization covers the visit date and service line."
                : "Authorization usage has exceeded the remaining authorized units.";
        return reconcileFlag(
                actorMembership,
                targetType,
                targetId,
                resolveBranch(actorMembership.getAgencyId(), visit.getBranchId()),
                active,
                RevenueExceptionType.AUTHORIZATION_ISSUE,
                RevenueExceptionSeverity.BLOCKING,
                reasonCode,
                summary,
                detectedAt);
    }

    private Optional<RevenueExceptionFlag> reconcileFlag(
            AgencyMembership actorMembership,
            RevenueExceptionTargetType targetType,
            UUID targetId,
            Branch branch,
            boolean active,
            RevenueExceptionType exceptionType,
            RevenueExceptionSeverity severity,
            String reasonCode,
            String summary,
            OffsetDateTime detectedAt) {
        Optional<RevenueExceptionFlag> existing = revenueExceptionFlagRepository.findFirstByTargetTypeAndTargetIdAndExceptionTypeAndClearedAtIsNull(
                targetType,
                targetId,
                exceptionType);
        if (active) {
            RevenueExceptionFlag saved = revenueExceptionFlagRepository.saveAndFlush(existing.map(flag -> {
                flag.refresh(severity, reasonCode, summary, detectedAt);
                return flag;
            }).orElseGet(() -> RevenueExceptionFlag.detect(
                    actorMembership.getAgency(),
                    branch,
                    targetType,
                    targetId,
                    exceptionType,
                    severity,
                    reasonCode,
                    summary,
                    detectedAt)));
            revenueReadinessAuditService.recordExceptionFlagUpdated(
                    actorMembership,
                    saved.getId(),
                    saved.getBranchId(),
                    "{\"exceptionType\":\"" + saved.getExceptionType() + "\",\"active\":true}");
            return Optional.of(saved);
        }
        existing.ifPresent(flag -> {
            flag.clear(detectedAt);
            RevenueExceptionFlag saved = revenueExceptionFlagRepository.saveAndFlush(flag);
            revenueReadinessAuditService.recordExceptionFlagUpdated(
                    actorMembership,
                    saved.getId(),
                    saved.getBranchId(),
                    "{\"exceptionType\":\"" + saved.getExceptionType() + "\",\"active\":false}");
        });
        return Optional.empty();
    }

    private RevenueUsagePosture deriveUsagePosture(
            PatientEpisodeAuthorization authorization,
            int usedUnits,
            Integer remainingUnits) {
        if (authorization == null) {
            return RevenueUsagePosture.MISSING_AUTHORIZATION;
        }
        if (authorization.getStatus() == PatientEpisodeAuthorizationStatus.CANCELLED
                || authorization.getStatus() == PatientEpisodeAuthorizationStatus.EXPIRED) {
            return RevenueUsagePosture.MISSING_AUTHORIZATION;
        }
        if (remainingUnits == null) {
            return RevenueUsagePosture.NOT_APPLICABLE;
        }
        if (remainingUnits < 0 || authorization.getStatus() == PatientEpisodeAuthorizationStatus.EXHAUSTED) {
            return RevenueUsagePosture.OVER_LIMIT;
        }
        if (remainingUnits <= 1 || usedUnits >= Math.max(authorization.getAuthorizedUnits() - 1, 0)) {
            return RevenueUsagePosture.NEAR_LIMIT;
        }
        return RevenueUsagePosture.WITHIN_LIMITS;
    }

    private int countCompletedVisitsForAuthorization(PatientEpisodeAuthorization authorization) {
        LocalDate start = authorization.getStartDate();
        LocalDate end = authorization.getEndDate();
        return (int) visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(authorization.getAgencyId()).stream()
                .filter(visit -> Objects.equals(visit.getPatient().getId(), authorization.getPatient().getId()))
                .filter(visit -> authorization.getServiceLine() == null
                        || (visit.getServiceLine() != null && Objects.equals(visit.getServiceLine().getId(), authorization.getServiceLine().getId())))
                .filter(visit -> {
                    LocalDate visitDate = visit.getPlannedStartAt().toLocalDate();
                    return !visitDate.isBefore(start) && !visitDate.isAfter(end);
                })
                .filter(visit -> {
                    MobileVisitExecutionSession execution = latestExecutionSession(visit.getId());
                    return execution != null && execution.getExecutionStatus() == MobileExecutionSessionStatus.COMPLETED;
                })
                .count();
    }

    private PatientPayerLink selectPayerLink(VisitOccurrence visit) {
        LocalDate visitDate = visit.getPlannedStartAt().toLocalDate();
        return patientPayerLinkRepository.findAllByPatient_IdOrderByEffectiveFromDesc(visit.getPatient().getId()).stream()
                .filter(link -> ACTIVE_PAYER_STATUSES.contains(link.getStatus()))
                .filter(link -> !link.getEffectiveFrom().isAfter(visitDate))
                .filter(link -> link.getEffectiveTo() == null || !link.getEffectiveTo().isBefore(visitDate))
                .sorted((left, right) -> {
                    if (left.isPrimaryPayer() != right.isPrimaryPayer()) {
                        return left.isPrimaryPayer() ? -1 : 1;
                    }
                    return right.getEffectiveFrom().compareTo(left.getEffectiveFrom());
                })
                .findFirst()
                .orElse(null);
    }

    private PatientEpisodeAuthorization selectAuthorization(VisitOccurrence visit) {
        if (visit.getServiceLine() == null) {
            return null;
        }
        LocalDate visitDate = visit.getPlannedStartAt().toLocalDate();
        return patientEpisodeAuthorizationRepository.findAllByPatient_IdOrderByStartDateDesc(visit.getPatient().getId()).stream()
                .filter(auth -> ACTIVE_AUTHORIZATION_STATUSES.contains(auth.getStatus()))
                .filter(auth -> auth.getServiceLine() != null
                        && visit.getServiceLine() != null
                        && Objects.equals(auth.getServiceLine().getId(), visit.getServiceLine().getId()))
                .filter(auth -> !auth.getStartDate().isAfter(visitDate) && !auth.getEndDate().isBefore(visitDate))
                .findFirst()
                .orElse(null);
    }

    private MobileVisitExecutionSession latestExecutionSession(UUID visitOccurrenceId) {
        List<MobileVisitExecutionSession> sessions = mobileVisitExecutionSessionRepository.findAllByVisitOccurrence_IdOrderByStartedAtAsc(visitOccurrenceId);
        return sessions.isEmpty() ? null : sessions.get(sessions.size() - 1);
    }

    private VisitDocumentationRecord latestDocumentationRecord(UUID visitOccurrenceId) {
        List<VisitDocumentationRecord> records = visitDocumentationRecordRepository.findAllByVisitOccurrence_IdOrderByLastSavedAtDesc(visitOccurrenceId);
        return records.isEmpty() ? null : records.get(0);
    }

    private EvvVerificationSession latestVerificationSession(UUID visitOccurrenceId) {
        return evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(visitOccurrenceId).orElse(null);
    }

    private CaregiverVisitAssignment latestActiveAssignment(UUID visitOccurrenceId) {
        return caregiverVisitAssignmentRepository.findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(
                        visitOccurrenceId,
                        CaregiverAssignmentStatus.ACTIVE)
                .orElse(null);
    }

    private int computeDurationMinutes(VisitOccurrence visit, MobileVisitExecutionSession execution) {
        OffsetDateTime start = execution != null && execution.getStartedAt() != null ? execution.getStartedAt() : visit.getPlannedStartAt();
        OffsetDateTime end = execution != null && execution.getEndedAt() != null ? execution.getEndedAt() : visit.getPlannedEndAt();
        return (int) Math.max(0, ChronoUnit.MINUTES.between(start, end));
    }

    private UUID resolveBranchIdForAuthorization(PatientEpisodeAuthorization authorization) {
        ServiceLine serviceLine = authorization.getServiceLine();
        if (serviceLine == null) {
            return null;
        }
        return visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(authorization.getAgencyId()).stream()
                .filter(visit -> Objects.equals(visit.getPatient().getId(), authorization.getPatient().getId()))
                .filter(visit -> visit.getServiceLine() != null && Objects.equals(visit.getServiceLine().getId(), serviceLine.getId()))
                .map(VisitOccurrence::getBranchId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private VisitOccurrence resolveVisit(UUID agencyId, UUID visitOccurrenceId) {
        return visitOccurrenceRepository.findByIdAndAgency_Id(visitOccurrenceId, agencyId)
                .orElseThrow(() -> new RevenueReadinessEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
    }

    private PatientEpisodeAuthorization resolveAuthorization(UUID agencyId, UUID authorizationId) {
        return patientEpisodeAuthorizationRepository.findById(authorizationId)
                .filter(authorization -> Objects.equals(authorization.getAgencyId(), agencyId))
                .orElseThrow(() -> new RevenueReadinessEntityNotFoundException("PatientEpisodeAuthorization", authorizationId));
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .filter(branch -> Objects.equals(branch.getAgencyId(), agencyId))
                .orElseThrow(() -> new RevenueReadinessEntityNotFoundException("Branch", branchId));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission, String action) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                permission,
                membershipId -> new UnauthorizedRevenueReadinessActorException(membershipId, action));
    }

    private void requireBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
            return;
        }
        if (!branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                actorMembership.getId(),
                branchId,
                BranchAssignmentStatus.ACTIVE)) {
            throw new UnauthorizedRevenueReadinessActorException(actorMembership.getId(), action);
        }
    }

    private boolean hasBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId) {
        if (!agencyAuthorizationGuard.hasPermission(actorMembership, permission)) {
            return false;
        }
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
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

    private String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    public record RecalculateVisitValidationCommand(
            @NotNull UUID visitOccurrenceId,
            OffsetDateTime evaluatedAt) {
    }

    public record RefreshVisitProjectionCommand(
            @NotNull UUID visitOccurrenceId,
            OffsetDateTime evaluatedAt) {
    }

    public record RefreshAuthorizationUsageCommand(
            @NotNull UUID authorizationId,
            OffsetDateTime evaluatedAt) {
    }

    public record GenerateExportCommand(
            @NotNull UUID visitOccurrenceId,
            boolean allowBlocked,
            OffsetDateTime generatedAt) {
    }

    public record ExportPreview(
            UUID visitOccurrenceId,
            RevenueReadinessStatus readinessStatus,
            String payerName,
            UUID caregiverProfileId,
            int unitsOrMinutes,
            boolean exportable,
            String blockedReason) {
    }

    public record RevenueHistoryView(
            RevenueReadinessProjection projection,
            List<RevenueExceptionFlag> exceptionFlags,
            List<PayrollExportRow> payrollExports,
            List<InvoiceExportRow> invoiceExports,
            List<AuditEvent> auditEvents) {
    }
}
