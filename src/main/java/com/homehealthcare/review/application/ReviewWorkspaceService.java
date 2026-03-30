package com.homehealthcare.review.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLinkRepository;
import com.homehealthcare.documentation.domain.DocumentationFieldResponse;
import com.homehealthcare.documentation.domain.DocumentationFieldResponseRepository;
import com.homehealthcare.documentation.domain.DocumentationTaskResponse;
import com.homehealthcare.documentation.domain.DocumentationTaskResponseRepository;
import com.homehealthcare.documentation.domain.DocumentationTemplateField;
import com.homehealthcare.documentation.domain.DocumentationTemplateFieldRepository;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.domain.VisitDocumentationRecordRepository;
import com.homehealthcare.documentation.foundation.DocumentationResponseState;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.MissedVisitRecord;
import com.homehealthcare.evv.domain.MissedVisitRecordRepository;
import com.homehealthcare.evv.domain.MissedVisitStatus;
import com.homehealthcare.evv.domain.SignatureVerificationLink;
import com.homehealthcare.evv.domain.SignatureVerificationLinkRepository;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.evv.domain.VisitExceptionRecord;
import com.homehealthcare.evv.domain.VisitExceptionRecordRepository;
import com.homehealthcare.evv.domain.VisitExceptionStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.review.domain.CompletenessCheckResult;
import com.homehealthcare.review.domain.CompletenessCheckResultRepository;
import com.homehealthcare.review.domain.MissingFieldResult;
import com.homehealthcare.review.domain.MissingFieldResultRepository;
import com.homehealthcare.review.domain.ReturnForFixEvent;
import com.homehealthcare.review.domain.ReturnForFixEventRepository;
import com.homehealthcare.review.domain.ReviewAssignment;
import com.homehealthcare.review.domain.ReviewAssignmentRepository;
import com.homehealthcare.review.domain.ReviewDecision;
import com.homehealthcare.review.domain.ReviewDecisionRepository;
import com.homehealthcare.review.domain.ReviewExceptionRecord;
import com.homehealthcare.review.domain.ReviewExceptionRecordRepository;
import com.homehealthcare.review.domain.ReviewFinding;
import com.homehealthcare.review.domain.ReviewFindingRepository;
import com.homehealthcare.review.domain.ReviewWorkItem;
import com.homehealthcare.review.domain.ReviewWorkItemRepository;
import com.homehealthcare.review.domain.SignoffRequest;
import com.homehealthcare.review.domain.SignoffRequestRepository;
import com.homehealthcare.review.foundation.ReviewAuditService;
import com.homehealthcare.review.foundation.ReviewDecisionType;
import com.homehealthcare.review.foundation.ReviewExceptionType;
import com.homehealthcare.review.foundation.ReviewFindingKind;
import com.homehealthcare.review.foundation.ReviewFindingSeverity;
import com.homehealthcare.review.foundation.ReviewFindingStatus;
import com.homehealthcare.review.foundation.ReviewLifecycleStatus;
import com.homehealthcare.review.foundation.ReviewPriority;
import com.homehealthcare.review.foundation.ReviewSourceType;
import com.homehealthcare.review.foundation.SignoffRequestStatus;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ReviewWorkspaceService {

    private static final Set<ReviewLifecycleStatus> ACTIVE_QUEUE_STATUSES = EnumSet.of(
            ReviewLifecycleStatus.PENDING_REVIEW,
            ReviewLifecycleStatus.ASSIGNED,
            ReviewLifecycleStatus.IN_REVIEW,
            ReviewLifecycleStatus.RETURNED_FOR_FIX,
            ReviewLifecycleStatus.RESUBMITTED,
            ReviewLifecycleStatus.SIGNOFF_REQUESTED);

    private static final Set<VisitExceptionStatus> OPEN_EVV_EXCEPTION_STATUSES = EnumSet.of(
            VisitExceptionStatus.OPEN,
            VisitExceptionStatus.ACKNOWLEDGED,
            VisitExceptionStatus.ESCALATED);

    private static final Set<MissedVisitStatus> OPEN_MISSED_VISIT_STATUSES = EnumSet.of(
            MissedVisitStatus.REPORTED,
            MissedVisitStatus.NOTIFIED,
            MissedVisitStatus.ESCALATED);

    private final ReviewWorkItemRepository reviewWorkItemRepository;
    private final CompletenessCheckResultRepository completenessCheckResultRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final MissingFieldResultRepository missingFieldResultRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ReviewDecisionRepository reviewDecisionRepository;
    private final ReturnForFixEventRepository returnForFixEventRepository;
    private final ReviewExceptionRecordRepository reviewExceptionRecordRepository;
    private final SignoffRequestRepository signoffRequestRepository;
    private final VisitDocumentationRecordRepository visitDocumentationRecordRepository;
    private final DocumentationTemplateFieldRepository documentationTemplateFieldRepository;
    private final DocumentationFieldResponseRepository documentationFieldResponseRepository;
    private final DocumentationTaskResponseRepository documentationTaskResponseRepository;
    private final DocumentationAttachmentLinkRepository documentationAttachmentLinkRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final SignatureVerificationLinkRepository signatureVerificationLinkRepository;
    private final VisitExceptionRecordRepository visitExceptionRecordRepository;
    private final MissedVisitRecordRepository missedVisitRecordRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AuditEventRepository auditEventRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ReviewAuditService reviewAuditService;

    @Transactional(readOnly = true)
    public List<ReviewQueueItemView> listReviewQueue(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            UUID assignedReviewerMembershipId,
            ReviewLifecycleStatus status,
            ReviewSourceType sourceType,
            OffsetDateTime dueFrom,
            OffsetDateTime dueTo) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, "view review queue");
        return reviewWorkItemRepository.findAllByAgency_IdOrderByEnteredQueueAtDesc(actorMembership.getAgencyId()).stream()
                .filter(workItem -> !workItem.isExceptionDriven())
                .filter(workItem -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, workItem.getBranchId()))
                .filter(workItem -> branchId == null || Objects.equals(workItem.getBranchId(), branchId))
                .filter(workItem -> status == null || workItem.getStatus() == status)
                .filter(workItem -> sourceType == null || workItem.getSourceType() == sourceType)
                .filter(workItem -> dueFrom == null || (workItem.getDueAt() != null && !workItem.getDueAt().isBefore(dueFrom)))
                .filter(workItem -> dueTo == null || (workItem.getDueAt() != null && !workItem.getDueAt().isAfter(dueTo)))
                .map(this::toQueueView)
                .filter(view -> assignedReviewerMembershipId == null
                        || (view.activeAssignment() != null
                        && Objects.equals(view.activeAssignment().getReviewerMembershipId(), assignedReviewerMembershipId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewExceptionQueueItemView> listExceptionQueue(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            UUID assignedReviewerMembershipId,
            ReviewLifecycleStatus status,
            ReviewSourceType sourceType,
            ReviewExceptionType exceptionType,
            ReviewFindingSeverity exceptionSeverity) {
        requirePermission(actorMembership, AgencyPermission.VIEW_EXCEPTION_QUEUE, "view exception queue");
        return reviewWorkItemRepository.findAllByAgency_IdOrderByEnteredQueueAtDesc(actorMembership.getAgencyId()).stream()
                .filter(ReviewWorkItem::isExceptionDriven)
                .filter(workItem -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_EXCEPTION_QUEUE, workItem.getBranchId()))
                .filter(workItem -> branchId == null || Objects.equals(workItem.getBranchId(), branchId))
                .filter(workItem -> status == null || workItem.getStatus() == status)
                .filter(workItem -> sourceType == null || workItem.getSourceType() == sourceType)
                .map(workItem -> new ReviewExceptionQueueItemView(
                        toQueueView(workItem),
                        reviewExceptionRecordRepository.findAllByWorkItem_IdOrderByDetectedAtAsc(workItem.getId()).stream()
                                .filter(exception -> exceptionType == null || exception.getExceptionType() == exceptionType)
                                .filter(exception -> exceptionSeverity == null || exception.getSeverity() == exceptionSeverity)
                                .toList()))
                .filter(view -> !view.exceptions().isEmpty())
                .filter(view -> assignedReviewerMembershipId == null
                        || (view.workItem().activeAssignment() != null
                        && Objects.equals(view.workItem().activeAssignment().getReviewerMembershipId(), assignedReviewerMembershipId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewWorkItemDetailView getWorkItemDetail(@NotNull AgencyMembership actorMembership, @NotNull UUID workItemId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, "view review work item detail");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, workItem.getBranchId(), "view review work item detail");
        return buildDetail(workItem);
    }

    @Transactional(readOnly = true)
    public CompletenessEvaluation getLatestCompleteness(@NotNull AgencyMembership actorMembership, @NotNull UUID workItemId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, "view completeness results");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, workItem.getBranchId(), "view completeness results");
        CompletenessCheckResult result = completenessCheckResultRepository.findAllByWorkItem_IdOrderByRunNumberDesc(workItemId).stream()
                .findFirst()
                .orElseThrow(() -> new ReviewEntityNotFoundException("No completeness result exists for this review work item."));
        return new CompletenessEvaluation(
                result,
                reviewFindingRepository.findAllByCompletenessCheckResult_IdOrderByRuleCodeAsc(result.getId()),
                missingFieldResultRepository.findAllByCompletenessCheckResult_IdOrderByFieldPathAsc(result.getId()));
    }

    @Transactional(readOnly = true)
    public ReviewHistoryView getReviewHistory(@NotNull AgencyMembership actorMembership, @NotNull UUID workItemId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVIEW_AUDIT_CONTEXT, "view review history");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVIEW_AUDIT_CONTEXT, workItem.getBranchId(), "view review history");
        List<ReviewAssignment> assignments = reviewAssignmentRepository.findAllByWorkItem_IdOrderByAssignedAtAsc(workItemId);
        List<ReviewDecision> decisions = reviewDecisionRepository.findAllByWorkItem_IdOrderByDecidedAtAsc(workItemId);
        List<ReturnForFixEvent> returnEvents = returnForFixEventRepository.findAllByWorkItem_IdOrderByReturnedAtAsc(workItemId);
        List<SignoffRequest> signoffRequests = signoffRequestRepository.findAllByWorkItem_IdOrderByRequestedAtAsc(workItemId);

        Set<UUID> targetIds = new java.util.LinkedHashSet<>();
        targetIds.add(workItem.getId());
        assignments.stream().map(ReviewAssignment::getId).forEach(targetIds::add);
        decisions.stream().map(ReviewDecision::getId).forEach(targetIds::add);
        returnEvents.stream().map(ReturnForFixEvent::getId).forEach(targetIds::add);
        signoffRequests.stream().map(SignoffRequest::getId).forEach(targetIds::add);
        targetIds.add(workItem.getSourceRecordId());

        List<AuditEvent> sourceAuditContext = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> targetIds.contains(event.getTargetId()))
                .toList();
        return new ReviewHistoryView(workItem, assignments, decisions, returnEvents, signoffRequests, sourceAuditContext);
    }

    @Transactional
    public ReviewWorkItem createWorkItem(@NotNull AgencyMembership actorMembership, @Valid CreateReviewWorkItemCommand command) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_REVIEW_WORK, "create review work items");
        SourceLinkage linkage = resolveSourceLinkage(actorMembership.getAgencyId(), command.sourceType(), command.sourceRecordId());
        requireBranchAccess(actorMembership, AgencyPermission.ASSIGN_REVIEW_WORK, linkage.branchId(), "create review work items");
        if (reviewWorkItemRepository.existsByAgency_IdAndSourceTypeAndSourceRecordIdAndStatusIn(
                actorMembership.getAgencyId(),
                command.sourceType(),
                command.sourceRecordId(),
                ACTIVE_QUEUE_STATUSES)) {
            throw new ReviewConflictException("An active review work item already exists for the requested source record.");
        }
        OffsetDateTime queuedAt = command.enteredQueueAt() == null ? OffsetDateTime.now() : command.enteredQueueAt();
        ReviewWorkItem workItem = reviewWorkItemRepository.saveAndFlush(ReviewWorkItem.queue(
                command.sourceType(),
                command.sourceRecordId(),
                linkage.branch(),
                linkage.patient(),
                linkage.visitOccurrence(),
                linkage.documentationRecord(),
                command.priority(),
                command.exceptionDriven(),
                queuedAt,
                command.dueAt()));
        if (command.exceptionDriven() || command.exceptionType() != null) {
            ReviewExceptionRecord exceptionRecord = reviewExceptionRecordRepository.saveAndFlush(ReviewExceptionRecord.create(
                    workItem,
                    command.sourceType(),
                    command.sourceRecordId(),
                    command.exceptionType() == null ? defaultExceptionType(command.sourceType()) : command.exceptionType(),
                    command.exceptionSeverity() == null ? ReviewFindingSeverity.WARNING : command.exceptionSeverity(),
                    queuedAt));
            reviewAuditService.recordReviewItemCreated(actorMembership, workItem.getId(), workItem.getBranchId(),
                    "{\"status\":\"" + workItem.getStatus().name() + "\",\"exceptionType\":\"" + exceptionRecord.getExceptionType().name() + "\"}");
        } else {
            reviewAuditService.recordReviewItemCreated(actorMembership, workItem.getId(), workItem.getBranchId(),
                    "{\"status\":\"" + workItem.getStatus().name() + "\"}");
        }
        return workItem;
    }

    @Transactional
    public CompletenessEvaluation recalculateCompleteness(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID workItemId,
            OffsetDateTime evaluatedAt) {
        requirePermission(actorMembership, AgencyPermission.PERFORM_REVIEW_DECISIONS, "recalculate completeness");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.PERFORM_REVIEW_DECISIONS, workItem.getBranchId(), "recalculate completeness");
        if (workItem.getSourceType() != ReviewSourceType.VISIT_DOCUMENTATION_RECORD || workItem.getDocumentationRecord() == null) {
            throw new ReviewConflictException("Completeness recalculation is currently supported only for documentation-sourced review items.");
        }

        VisitDocumentationRecord record = workItem.getDocumentationRecord();
        OffsetDateTime when = evaluatedAt == null ? OffsetDateTime.now() : evaluatedAt;
        int nextRun = completenessCheckResultRepository.findAllByWorkItem_IdOrderByRunNumberDesc(workItemId).stream()
                .findFirst()
                .map(CompletenessCheckResult::getRunNumber)
                .orElse(0) + 1;

        List<ReviewFinding> findings = new ArrayList<>();
        List<MissingFieldResult> missingFields = new ArrayList<>();
        int passCount = 0;
        int warningCount = 0;
        int failCount = 0;

        List<DocumentationTemplateField> requiredFields = documentationTemplateFieldRepository
                .findAllByDocumentationTemplate_IdOrderBySortOrderAscLabelAsc(record.getSelectedTemplateId())
                .stream()
                .filter(DocumentationTemplateField::isRequiredField)
                .toList();
        List<DocumentationFieldResponse> fieldResponses = documentationFieldResponseRepository
                .findAllByDocumentationRecord_IdOrderByFieldKeyAsc(record.getId());

        for (DocumentationTemplateField field : requiredFields) {
            Optional<DocumentationFieldResponse> response = fieldResponses.stream()
                    .filter(candidate -> Objects.equals(candidate.getTemplateFieldId(), field.getId()))
                    .findFirst();
            boolean complete = response.isPresent()
                    && response.get().getCompletionState() == DocumentationResponseState.COMPLETED
                    && ((response.get().getDisplayValue() != null && !response.get().getDisplayValue().isBlank())
                    || (response.get().getNormalizedValue() != null && !response.get().getNormalizedValue().isBlank()));
            if (complete) {
                passCount++;
            } else {
                ReviewFinding finding = ReviewFinding.create(
                        workItem,
                        null,
                        ReviewFindingKind.MISSING_FIELD,
                        ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                        record.getId(),
                        "REQUIRED_FIELD_MISSING",
                        ReviewFindingSeverity.ERROR,
                        ReviewFindingStatus.FAIL,
                        field.getFieldKey(),
                        field.getSection() == null ? null : field.getSection().getSectionKey(),
                        "Required documentation field '%s' is incomplete.".formatted(field.getLabel()),
                        when);
                findings.add(finding);
                failCount++;
            }
        }

        List<DocumentationTaskResponse> taskResponses = documentationTaskResponseRepository
                .findAllByDocumentationRecord_IdOrderBySortOrderAscTaskTitleAsc(record.getId());
        long incompleteRequiredTasks = taskResponses.stream()
                .filter(DocumentationTaskResponse::isCompletionRequired)
                .filter(task -> task.getCompletionState() != DocumentationResponseState.COMPLETED)
                .count();
        if (incompleteRequiredTasks > 0) {
            findings.add(ReviewFinding.create(
                    workItem,
                    null,
                    ReviewFindingKind.COMPLETENESS,
                    ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                    record.getId(),
                    "REQUIRED_TASK_INCOMPLETE",
                    ReviewFindingSeverity.ERROR,
                    ReviewFindingStatus.FAIL,
                    null,
                    "tasks",
                    "One or more required documentation tasks remain incomplete.",
                    when));
            failCount++;
        } else {
            passCount++;
        }

        if (record.getSelectedTemplate().isRequiresSignatureVerification()) {
            boolean signaturePresent = evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(record.getVisitOccurrenceId())
                    .map(session -> signatureVerificationLinkRepository.findAllByVerificationSession_IdOrderByRecordedAtAsc(session.getId()))
                    .stream()
                    .flatMap(List::stream)
                    .anyMatch(link -> link.getVerificationStatus() == SignatureVerificationStatus.PRESENT);
            if (!signaturePresent) {
                findings.add(ReviewFinding.create(
                        workItem,
                        null,
                        ReviewFindingKind.COMPLETENESS,
                        ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                        record.getId(),
                        "SIGNATURE_MISSING",
                        ReviewFindingSeverity.CRITICAL,
                        ReviewFindingStatus.FAIL,
                        null,
                        "signature",
                        "Required signature verification is missing for this documentation record.",
                        when));
                failCount++;
            } else {
                passCount++;
            }
        }

        if (documentationAttachmentLinkRepository.findAllByDocumentationRecord_IdOrderByLinkedAtAsc(record.getId()).isEmpty()) {
            findings.add(ReviewFinding.create(
                    workItem,
                    null,
                    ReviewFindingKind.COMPLETENESS,
                    ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                    record.getId(),
                    "ATTACHMENT_LINKAGE_MISSING",
                    ReviewFindingSeverity.WARNING,
                    ReviewFindingStatus.WARNING,
                    null,
                    "attachments",
                    "No supporting attachment is linked to this documentation record.",
                    when));
            warningCount++;
        } else {
            passCount++;
        }

        long openVisitExceptions = visitExceptionRecordRepository.countByVerificationSession_IdAndStatusIn(
                evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(record.getVisitOccurrenceId())
                        .map(session -> session.getId())
                        .orElse(UUID.randomUUID()),
                OPEN_EVV_EXCEPTION_STATUSES);
        if (openVisitExceptions > 0 || missedVisitRecordRepository.existsByVisitOccurrence_IdAndStatusIn(record.getVisitOccurrenceId(), OPEN_MISSED_VISIT_STATUSES)) {
            findings.add(ReviewFinding.create(
                    workItem,
                    null,
                    ReviewFindingKind.COMPLETENESS,
                    ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                    record.getId(),
                    "OPEN_VISIT_EXCEPTION",
                    ReviewFindingSeverity.WARNING,
                    ReviewFindingStatus.WARNING,
                    null,
                    "evv",
                    "Related EVV or missed-visit exceptions remain open for this visit.",
                    when));
            warningCount++;
        } else {
            passCount++;
        }

        CompletenessCheckResult result = completenessCheckResultRepository.saveAndFlush(CompletenessCheckResult.create(
                workItem,
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                record.getId(),
                nextRun,
                passCount,
                warningCount,
                failCount,
                when));

        List<ReviewFinding> persistedFindings = new ArrayList<>();
        for (ReviewFinding finding : findings) {
            ReviewFinding persisted = reviewFindingRepository.saveAndFlush(ReviewFinding.create(
                    workItem,
                    result,
                    finding.getFindingKind(),
                    finding.getEvaluatedSourceType(),
                    finding.getEvaluatedSourceId(),
                    finding.getRuleCode(),
                    finding.getSeverity(),
                    finding.getFindingStatus(),
                    finding.getFieldPath(),
                    finding.getLogicalSection(),
                    finding.getExplanation(),
                    when));
            persistedFindings.add(persisted);
            if (persisted.getFindingKind() == ReviewFindingKind.MISSING_FIELD) {
                missingFields.add(missingFieldResultRepository.saveAndFlush(MissingFieldResult.create(
                        workItem,
                        result,
                        persisted,
                        persisted.getEvaluatedSourceType(),
                        persisted.getEvaluatedSourceId(),
                        persisted.getRuleCode(),
                        persisted.getSeverity(),
                        persisted.getFindingStatus(),
                        Objects.requireNonNull(persisted.getFieldPath()),
                        persisted.getLogicalSection(),
                        persisted.getExplanation())));
            }
        }
        workItem.markInReview(when);
        reviewWorkItemRepository.saveAndFlush(workItem);
        reviewAuditService.recordCompletenessRecalculated(actorMembership, workItem.getId(), workItem.getBranchId(),
                "{\"runNumber\":" + result.getRunNumber() + ",\"failCount\":" + result.getFailCount() + ",\"warningCount\":" + result.getWarningCount() + "}");
        return new CompletenessEvaluation(result, persistedFindings, missingFields);
    }

    @Transactional
    public ReviewAssignment assignWorkItem(@NotNull AgencyMembership actorMembership, @NotNull UUID workItemId, @Valid AssignReviewWorkCommand command) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_REVIEW_WORK, "assign review work");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.ASSIGN_REVIEW_WORK, workItem.getBranchId(), "assign review work");
        AgencyMembership reviewer = resolveMembership(actorMembership.getAgencyId(), command.reviewerMembershipId());
        requireBranchAccess(reviewer, AgencyPermission.VIEW_REVIEW_WORKSPACE, workItem.getBranchId(), "receive review assignments");
        reviewAssignmentRepository.findFirstByWorkItem_IdAndReleasedAtIsNull(workItemId)
                .ifPresent(active -> {
                    active.release(command.assignedAt());
                    reviewAssignmentRepository.saveAndFlush(active);
                    reviewAuditService.recordReviewReassigned(actorMembership, active.getId(), workItem.getBranchId(),
                            "{\"reviewerMembershipId\":\"" + active.getReviewerMembershipId() + "\"}");
                });
        ReviewAssignment assignment = reviewAssignmentRepository.saveAndFlush(ReviewAssignment.assign(
                workItem, reviewer, actorMembership, command.assignedAt(), command.assignmentNote()));
        workItem.markAssigned(command.assignedAt());
        reviewWorkItemRepository.saveAndFlush(workItem);
        reviewAuditService.recordReviewAssigned(actorMembership, assignment.getId(), workItem.getBranchId(),
                "{\"reviewerMembershipId\":\"" + reviewer.getId() + "\"}");
        return assignment;
    }

    @Transactional
    public void releaseActiveAssignment(@NotNull AgencyMembership actorMembership, @NotNull UUID workItemId, OffsetDateTime releasedAt) {
        requirePermission(actorMembership, AgencyPermission.ASSIGN_REVIEW_WORK, "release review assignment");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.ASSIGN_REVIEW_WORK, workItem.getBranchId(), "release review assignment");
        ReviewAssignment assignment = reviewAssignmentRepository.findFirstByWorkItem_IdAndReleasedAtIsNull(workItemId)
                .orElseThrow(() -> new ReviewConflictException("No active review assignment exists for this work item."));
        OffsetDateTime when = releasedAt == null ? OffsetDateTime.now() : releasedAt;
        assignment.release(when);
        reviewAssignmentRepository.saveAndFlush(assignment);
        workItem.markInReview(when);
        reviewWorkItemRepository.saveAndFlush(workItem);
        reviewAuditService.recordReviewReassigned(actorMembership, assignment.getId(), workItem.getBranchId(),
                "{\"released\":true,\"reviewerMembershipId\":\"" + assignment.getReviewerMembershipId() + "\"}");
    }

    @Transactional
    public ReviewDecision recordDecision(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID workItemId,
            @Valid RecordReviewDecisionCommand command) {
        requirePermission(actorMembership, AgencyPermission.PERFORM_REVIEW_DECISIONS, "record review decisions");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.PERFORM_REVIEW_DECISIONS, workItem.getBranchId(), "record review decisions");
        assertActorOwnsOrCanAdminister(actorMembership, workItem);
        ReviewDecision decision = reviewDecisionRepository.saveAndFlush(ReviewDecision.record(
                workItem,
                command.decisionType(),
                actorMembership,
                command.decidedAt(),
                command.reasonCode(),
                command.reviewerNotes()));
        switch (command.decisionType()) {
            case APPROVE -> workItem.markApproved(command.decidedAt());
            case REJECT -> workItem.markRejected(command.decidedAt());
            case RETURN_FOR_FIX -> {
                workItem.markReturnedForFix(command.decidedAt());
                ReturnForFixEvent event = returnForFixEventRepository.saveAndFlush(ReturnForFixEvent.create(
                        workItem,
                        workItem.getSourceType(),
                        workItem.getSourceRecordId(),
                        command.returnReason(),
                        command.requiredCorrections(),
                        actorMembership,
                        command.decidedAt()));
                reviewAuditService.recordReturnedForFix(actorMembership, event.getId(), workItem.getBranchId(),
                        "{\"workItemId\":\"" + workItem.getId() + "\"}");
                if (!reviewFindingRepository.findAllByWorkItem_IdOrderByEvaluatedAtAscRuleCodeAsc(workItemId).isEmpty()) {
                    reviewExceptionRecordRepository.saveAndFlush(ReviewExceptionRecord.create(
                            workItem,
                            workItem.getSourceType(),
                            workItem.getSourceRecordId(),
                            ReviewExceptionType.RETURNED_WITH_OPEN_FINDING,
                            ReviewFindingSeverity.WARNING,
                            command.decidedAt()));
                }
            }
            case REQUEST_SIGNOFF -> workItem.markSignoffRequested(command.decidedAt());
        }
        reviewWorkItemRepository.saveAndFlush(workItem);
        reviewAuditService.recordReviewDecision(actorMembership, decision.getId(), workItem.getBranchId(),
                "{\"decisionType\":\"" + command.decisionType().name() + "\"}");
        return decision;
    }

    @Transactional
    public ReturnForFixEvent markResubmitted(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID workItemId,
            OffsetDateTime resubmittedAt) {
        requirePermission(actorMembership, AgencyPermission.PERFORM_REVIEW_DECISIONS, "mark review work resubmitted");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.PERFORM_REVIEW_DECISIONS, workItem.getBranchId(), "mark review work resubmitted");
        ReturnForFixEvent event = returnForFixEventRepository.findFirstByWorkItem_IdAndResolvedAtIsNullOrderByReturnedAtDesc(workItemId)
                .orElseThrow(() -> new ReviewConflictException("No active return-for-fix event exists for this work item."));
        OffsetDateTime when = resubmittedAt == null ? OffsetDateTime.now() : resubmittedAt;
        event.markResubmitted(when);
        event.markResolved(when);
        returnForFixEventRepository.saveAndFlush(event);
        workItem.markResubmitted(when);
        reviewWorkItemRepository.saveAndFlush(workItem);
        return event;
    }

    @Transactional
    public SignoffRequest requestSignoff(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID workItemId,
            @Valid RequestSignoffCommand command) {
        requirePermission(actorMembership, AgencyPermission.REQUEST_REVIEW_SIGNOFF, "request review signoff");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.REQUEST_REVIEW_SIGNOFF, workItem.getBranchId(), "request review signoff");
        AgencyMembership requestedFromMembership = command.requestedFromMembershipId() == null
                ? null
                : resolveMembership(actorMembership.getAgencyId(), command.requestedFromMembershipId());
        if (requestedFromMembership != null) {
            requireBranchAccess(requestedFromMembership, AgencyPermission.VIEW_REVIEW_WORKSPACE, workItem.getBranchId(), "receive signoff requests");
        }
        SignoffRequest signoffRequest = signoffRequestRepository.saveAndFlush(SignoffRequest.create(
                workItem,
                requestedFromMembership,
                command.requestedFromRole(),
                actorMembership,
                command.requestedAt(),
                command.signoffNote()));
        workItem.markSignoffRequested(command.requestedAt());
        reviewWorkItemRepository.saveAndFlush(workItem);
        reviewAuditService.recordSignoffRequested(actorMembership, signoffRequest.getId(), workItem.getBranchId(),
                "{\"requestedFromMembershipId\":\"" + (requestedFromMembership == null ? "" : requestedFromMembership.getId()) + "\"}");
        return signoffRequest;
    }

    @Transactional
    public SignoffRequest completeSignoff(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID workItemId,
            OffsetDateTime completedAt,
            boolean approved,
            String signoffNote) {
        requirePermission(actorMembership, AgencyPermission.REQUEST_REVIEW_SIGNOFF, approved ? "complete review signoff" : "decline review signoff");
        ReviewWorkItem workItem = resolveWorkItem(actorMembership.getAgencyId(), workItemId);
        requireBranchAccess(actorMembership, AgencyPermission.REQUEST_REVIEW_SIGNOFF, workItem.getBranchId(), approved ? "complete review signoff" : "decline review signoff");
        SignoffRequest request = signoffRequestRepository.findFirstByWorkItem_IdAndStatusOrderByRequestedAtDesc(workItemId, SignoffRequestStatus.PENDING)
                .orElseThrow(() -> new ReviewConflictException("No pending signoff request exists for this work item."));
        OffsetDateTime when = completedAt == null ? OffsetDateTime.now() : completedAt;
        if (approved) {
            request.complete(actorMembership, when, signoffNote);
            workItem.markSignoffCompleted(when);
        } else {
            request.decline(actorMembership, when, signoffNote);
            workItem.markInReview(when);
        }
        signoffRequestRepository.saveAndFlush(request);
        reviewWorkItemRepository.saveAndFlush(workItem);
        return request;
    }

    private SourceLinkage resolveSourceLinkage(UUID agencyId, ReviewSourceType sourceType, UUID sourceRecordId) {
        return switch (sourceType) {
            case VISIT_DOCUMENTATION_RECORD -> {
                VisitDocumentationRecord record = visitDocumentationRecordRepository.findByIdAndAgency_Id(sourceRecordId, agencyId)
                        .orElseThrow(() -> new ReviewEntityNotFoundException("Documentation record was not found."));
                if (record.getStatus() != DocumentationRecordStatus.SUBMITTED
                        && record.getStatus() != DocumentationRecordStatus.AMENDED
                        && record.getStatus() != DocumentationRecordStatus.IN_PROGRESS) {
                    throw new ReviewConflictException("Only in-progress, submitted, or amended documentation can enter the review queue.");
                }
                yield new SourceLinkage(record.getBranch(), record.getPatient(), record.getVisitOccurrence(), record);
            }
            case EVV_EXCEPTION_RECORD -> {
                VisitExceptionRecord exceptionRecord = visitExceptionRecordRepository.findByIdAndAgency_Id(sourceRecordId, agencyId)
                        .orElseThrow(() -> new ReviewEntityNotFoundException("EVV exception record was not found."));
                yield new SourceLinkage(exceptionRecord.getBranch(), exceptionRecord.getPatient(), exceptionRecord.getVisitOccurrence(), null);
            }
            case MISSED_VISIT_RECORD -> {
                MissedVisitRecord missedVisitRecord = missedVisitRecordRepository.findByIdAndAgency_Id(sourceRecordId, agencyId)
                        .orElseThrow(() -> new ReviewEntityNotFoundException("Missed visit record was not found."));
                yield new SourceLinkage(missedVisitRecord.getBranch(), missedVisitRecord.getPatient(), missedVisitRecord.getVisitOccurrence(), null);
            }
            case MOBILE_EXECUTION_SESSION -> throw new ReviewConflictException("Mobile execution session queue linkage is reserved for a later Epic 10 API slice.");
        };
    }

    private ReviewWorkItem resolveWorkItem(UUID agencyId, UUID workItemId) {
        return reviewWorkItemRepository.findByIdAndAgency_Id(workItemId, agencyId)
                .orElseThrow(() -> new ReviewEntityNotFoundException("Review work item was not found."));
    }

    private AgencyMembership resolveMembership(UUID agencyId, UUID membershipId) {
        return agencyMembershipRepository.findById(membershipId)
                .filter(membership -> Objects.equals(membership.getAgencyId(), agencyId))
                .orElseThrow(() -> new ReviewEntityNotFoundException("Agency membership was not found."));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission, String action) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, membershipId -> new UnauthorizedReviewActorException(membershipId, action));
    }

    private boolean hasBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId) {
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
            return true;
        }
        return permission.requiresAssignedBranchFor(actorMembership.getRole())
                && branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                        actorMembership.getId(), branchId, BranchAssignmentStatus.ACTIVE);
    }

    private void requireBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
            return;
        }
        if (!permission.requiresAssignedBranchFor(actorMembership.getRole())
                || !branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                        actorMembership.getId(), branchId, BranchAssignmentStatus.ACTIVE)) {
            throw new UnauthorizedReviewActorException(actorMembership.getId(), action);
        }
    }

    private void assertActorOwnsOrCanAdminister(AgencyMembership actorMembership, ReviewWorkItem workItem) {
        if (AgencyPermission.PERFORM_REVIEW_DECISIONS.isAgencyWideFor(actorMembership.getRole())) {
            return;
        }
        ReviewAssignment assignment = reviewAssignmentRepository.findFirstByWorkItem_IdAndReleasedAtIsNull(workItem.getId()).orElse(null);
        if (assignment == null || !Objects.equals(assignment.getReviewerMembershipId(), actorMembership.getId())) {
            throw new UnauthorizedReviewActorException(actorMembership.getId(), "act on review work not assigned to them");
        }
    }

    private ReviewExceptionType defaultExceptionType(ReviewSourceType sourceType) {
        return switch (sourceType) {
            case VISIT_DOCUMENTATION_RECORD -> ReviewExceptionType.MISSING_REQUIRED_DOCUMENTATION;
            case EVV_EXCEPTION_RECORD -> ReviewExceptionType.EVV_EXCEPTION_CARRYOVER;
            case MISSED_VISIT_RECORD -> ReviewExceptionType.EVV_EXCEPTION_CARRYOVER;
            case MOBILE_EXECUTION_SESSION -> ReviewExceptionType.REVIEWER_ESCALATION;
        };
    }

    public record CompletenessEvaluation(
            CompletenessCheckResult result,
            List<ReviewFinding> findings,
            List<MissingFieldResult> missingFieldResults) {
    }

    public record ReviewQueueItemView(
            ReviewWorkItem workItem,
            ReviewAssignment activeAssignment,
            long failCount,
            long warningCount,
            long openExceptionCount) {
    }

    public record ReviewExceptionQueueItemView(
            ReviewQueueItemView workItem,
            List<ReviewExceptionRecord> exceptions) {
    }

    public record ReviewWorkItemDetailView(
            ReviewWorkItem workItem,
            ReviewAssignment activeAssignment,
            List<ReviewFinding> latestFindings,
            List<MissingFieldResult> latestMissingFieldResults,
            List<ReviewExceptionRecord> exceptions,
            List<ReviewDecision> decisions,
            List<SignoffRequest> signoffRequests) {
    }

    public record ReviewHistoryView(
            ReviewWorkItem workItem,
            List<ReviewAssignment> assignments,
            List<ReviewDecision> decisions,
            List<ReturnForFixEvent> returnForFixEvents,
            List<SignoffRequest> signoffRequests,
            List<AuditEvent> sourceAuditContext) {
    }

    private record SourceLinkage(
            Branch branch,
            com.homehealthcare.patient.domain.Patient patient,
            com.homehealthcare.schedulingvisit.domain.VisitOccurrence visitOccurrence,
            VisitDocumentationRecord documentationRecord) {

        UUID branchId() {
            return branch == null ? null : branch.getId();
        }
    }

    private ReviewQueueItemView toQueueView(ReviewWorkItem workItem) {
        ReviewAssignment activeAssignment = reviewAssignmentRepository.findFirstByWorkItem_IdAndReleasedAtIsNull(workItem.getId()).orElse(null);
        List<ReviewFinding> findings = reviewFindingRepository.findAllByWorkItem_IdOrderByEvaluatedAtAscRuleCodeAsc(workItem.getId());
        long failCount = findings.stream().filter(finding -> finding.getFindingStatus() == ReviewFindingStatus.FAIL).count();
        long warningCount = findings.stream().filter(finding -> finding.getFindingStatus() == ReviewFindingStatus.WARNING).count();
        long openExceptionCount = reviewExceptionRecordRepository.findAllByWorkItem_IdOrderByDetectedAtAsc(workItem.getId()).stream()
                .filter(exception -> exception.getResolvedAt() == null)
                .count();
        return new ReviewQueueItemView(workItem, activeAssignment, failCount, warningCount, openExceptionCount);
    }

    private ReviewWorkItemDetailView buildDetail(ReviewWorkItem workItem) {
        ReviewAssignment activeAssignment = reviewAssignmentRepository.findFirstByWorkItem_IdAndReleasedAtIsNull(workItem.getId()).orElse(null);
        CompletenessCheckResult latestResult = completenessCheckResultRepository.findAllByWorkItem_IdOrderByRunNumberDesc(workItem.getId()).stream()
                .findFirst()
                .orElse(null);
        List<ReviewFinding> latestFindings = latestResult == null
                ? List.of()
                : reviewFindingRepository.findAllByCompletenessCheckResult_IdOrderByRuleCodeAsc(latestResult.getId());
        List<MissingFieldResult> latestMissingFields = latestResult == null
                ? List.of()
                : missingFieldResultRepository.findAllByCompletenessCheckResult_IdOrderByFieldPathAsc(latestResult.getId());
        return new ReviewWorkItemDetailView(
                workItem,
                activeAssignment,
                latestFindings,
                latestMissingFields,
                reviewExceptionRecordRepository.findAllByWorkItem_IdOrderByDetectedAtAsc(workItem.getId()),
                reviewDecisionRepository.findAllByWorkItem_IdOrderByDecidedAtAsc(workItem.getId()),
                signoffRequestRepository.findAllByWorkItem_IdOrderByRequestedAtAsc(workItem.getId()));
    }

    public record CreateReviewWorkItemCommand(
            @NotNull ReviewSourceType sourceType,
            @NotNull UUID sourceRecordId,
            ReviewPriority priority,
            OffsetDateTime enteredQueueAt,
            OffsetDateTime dueAt,
            boolean exceptionDriven,
            ReviewExceptionType exceptionType,
            ReviewFindingSeverity exceptionSeverity) {
    }

    public record AssignReviewWorkCommand(
            @NotNull UUID reviewerMembershipId,
            @NotNull OffsetDateTime assignedAt,
            String assignmentNote) {
    }

    public record RecordReviewDecisionCommand(
            @NotNull ReviewDecisionType decisionType,
            @NotNull OffsetDateTime decidedAt,
            String reasonCode,
            String reviewerNotes,
            String returnReason,
            String requiredCorrections) {
    }

    public record RequestSignoffCommand(
            UUID requestedFromMembershipId,
            AgencyRole requestedFromRole,
            @NotNull OffsetDateTime requestedAt,
            String signoffNote) {
    }
}
