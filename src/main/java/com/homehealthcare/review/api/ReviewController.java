package com.homehealthcare.review.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.review.application.ReviewWorkspaceService;
import com.homehealthcare.review.application.ReviewWorkspaceService.AssignReviewWorkCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.CompletenessEvaluation;
import com.homehealthcare.review.application.ReviewWorkspaceService.CreateReviewWorkItemCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.RecordReviewDecisionCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.RequestSignoffCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.ReviewExceptionQueueItemView;
import com.homehealthcare.review.application.ReviewWorkspaceService.ReviewHistoryView;
import com.homehealthcare.review.application.ReviewWorkspaceService.ReviewQueueItemView;
import com.homehealthcare.review.application.ReviewWorkspaceService.ReviewWorkItemDetailView;
import com.homehealthcare.review.domain.CompletenessCheckResult;
import com.homehealthcare.review.domain.MissingFieldResult;
import com.homehealthcare.review.domain.ReturnForFixEvent;
import com.homehealthcare.review.domain.ReviewAssignment;
import com.homehealthcare.review.domain.ReviewDecision;
import com.homehealthcare.review.domain.ReviewExceptionRecord;
import com.homehealthcare.review.domain.ReviewFinding;
import com.homehealthcare.review.domain.ReviewWorkItem;
import com.homehealthcare.review.domain.SignoffRequest;
import com.homehealthcare.review.foundation.ReviewDecisionType;
import com.homehealthcare.review.foundation.ReviewExceptionType;
import com.homehealthcare.review.foundation.ReviewFindingSeverity;
import com.homehealthcare.review.foundation.ReviewFindingStatus;
import com.homehealthcare.review.foundation.ReviewLifecycleStatus;
import com.homehealthcare.review.foundation.ReviewPriority;
import com.homehealthcare.review.foundation.ReviewSourceType;
import com.homehealthcare.review.foundation.SignoffRequestStatus;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
class ReviewController {

    private final ConfigurationActorResolver actorResolver;
    private final ReviewWorkspaceService reviewWorkspaceService;

    ReviewController(ConfigurationActorResolver actorResolver, ReviewWorkspaceService reviewWorkspaceService) {
        this.actorResolver = actorResolver;
        this.reviewWorkspaceService = reviewWorkspaceService;
    }

    @GetMapping("/work-items")
    PagedResponse<ReviewQueueItemResponse> listReviewQueue(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "assignedReviewerMembershipId", required = false) UUID assignedReviewerMembershipId,
            @RequestParam(name = "status", required = false) ReviewLifecycleStatus status,
            @RequestParam(name = "sourceType", required = false) ReviewSourceType sourceType,
            @RequestParam(name = "dueFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dueFrom,
            @RequestParam(name = "dueTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dueTo,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                reviewWorkspaceService.listReviewQueue(
                                actorResolver.requireActorMembership(),
                                branchId,
                                assignedReviewerMembershipId,
                                status,
                                sourceType,
                                dueFrom,
                                dueTo)
                        .stream()
                        .map(ReviewController::toReviewQueueItemResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/exception-queue")
    PagedResponse<ReviewExceptionQueueItemResponse> listExceptionQueue(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "assignedReviewerMembershipId", required = false) UUID assignedReviewerMembershipId,
            @RequestParam(name = "status", required = false) ReviewLifecycleStatus status,
            @RequestParam(name = "sourceType", required = false) ReviewSourceType sourceType,
            @RequestParam(name = "exceptionType", required = false) ReviewExceptionType exceptionType,
            @RequestParam(name = "exceptionSeverity", required = false) ReviewFindingSeverity exceptionSeverity,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                reviewWorkspaceService.listExceptionQueue(
                                actorResolver.requireActorMembership(),
                                branchId,
                                assignedReviewerMembershipId,
                                status,
                                sourceType,
                                exceptionType,
                                exceptionSeverity)
                        .stream()
                        .map(ReviewController::toReviewExceptionQueueItemResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/work-items/{workItemId}")
    ReviewWorkItemDetailResponse getWorkItemDetail(@PathVariable UUID workItemId) {
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @PostMapping("/work-items")
    ReviewWorkItemDetailResponse createWorkItem(@Valid @RequestBody CreateReviewWorkItemRequest request) {
        ReviewWorkItem workItem = reviewWorkspaceService.createWorkItem(
                actorResolver.requireActorMembership(),
                new CreateReviewWorkItemCommand(
                        request.sourceType(),
                        request.sourceRecordId(),
                        request.priority(),
                        request.enteredQueueAt(),
                        request.dueAt(),
                        request.exceptionDriven(),
                        request.exceptionType(),
                        request.exceptionSeverity()));
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItem.getId()));
    }

    @PostMapping("/work-items/{workItemId}/assignments")
    ReviewWorkItemDetailResponse assignWorkItem(@PathVariable UUID workItemId, @Valid @RequestBody AssignReviewWorkRequest request) {
        reviewWorkspaceService.assignWorkItem(
                actorResolver.requireActorMembership(),
                workItemId,
                new AssignReviewWorkCommand(
                        request.reviewerMembershipId(),
                        request.assignedAt(),
                        request.assignmentNote()));
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @DeleteMapping("/work-items/{workItemId}/assignments/active")
    ReviewWorkItemDetailResponse releaseActiveAssignment(
            @PathVariable UUID workItemId,
            @RequestParam(name = "releasedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime releasedAt) {
        reviewWorkspaceService.releaseActiveAssignment(actorResolver.requireActorMembership(), workItemId, releasedAt);
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @PostMapping("/work-items/{workItemId}/decisions")
    ReviewWorkItemDetailResponse recordDecision(@PathVariable UUID workItemId, @Valid @RequestBody RecordReviewDecisionRequest request) {
        reviewWorkspaceService.recordDecision(
                actorResolver.requireActorMembership(),
                workItemId,
                new RecordReviewDecisionCommand(
                        request.decisionType(),
                        request.decidedAt(),
                        request.reasonCode(),
                        request.reviewerNotes(),
                        request.returnReason(),
                        request.requiredCorrections()));
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @PostMapping("/work-items/{workItemId}/resubmissions")
    ReviewWorkItemDetailResponse markResubmitted(
            @PathVariable UUID workItemId,
            @RequestParam(name = "resubmittedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime resubmittedAt) {
        reviewWorkspaceService.markResubmitted(actorResolver.requireActorMembership(), workItemId, resubmittedAt);
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @PostMapping("/work-items/{workItemId}/signoff-requests")
    ReviewWorkItemDetailResponse requestSignoff(@PathVariable UUID workItemId, @Valid @RequestBody RequestSignoffApiRequest request) {
        reviewWorkspaceService.requestSignoff(
                actorResolver.requireActorMembership(),
                workItemId,
                new RequestSignoffCommand(
                        request.requestedFromMembershipId(),
                        request.requestedFromRole(),
                        request.requestedAt(),
                        request.signoffNote()));
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @PostMapping("/work-items/{workItemId}/signoff-completion")
    ReviewWorkItemDetailResponse completeSignoff(
            @PathVariable UUID workItemId,
            @Valid @RequestBody CompleteSignoffRequest request) {
        reviewWorkspaceService.completeSignoff(
                actorResolver.requireActorMembership(),
                workItemId,
                request.completedAt(),
                request.approved(),
                request.signoffNote());
        return toReviewWorkItemDetailResponse(reviewWorkspaceService.getWorkItemDetail(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @GetMapping("/work-items/{workItemId}/completeness")
    CompletenessEvaluationResponse getLatestCompleteness(@PathVariable UUID workItemId) {
        return toCompletenessEvaluationResponse(reviewWorkspaceService.getLatestCompleteness(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @PostMapping("/work-items/{workItemId}/completeness/recalculate")
    CompletenessEvaluationResponse recalculateCompleteness(
            @PathVariable UUID workItemId,
            @RequestParam(name = "evaluatedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime evaluatedAt) {
        return toCompletenessEvaluationResponse(reviewWorkspaceService.recalculateCompleteness(
                actorResolver.requireActorMembership(),
                workItemId,
                evaluatedAt));
    }

    @GetMapping("/work-items/{workItemId}/missing-fields")
    List<MissingFieldResultResponse> getMissingFields(@PathVariable UUID workItemId) {
        return toCompletenessEvaluationResponse(reviewWorkspaceService.getLatestCompleteness(
                actorResolver.requireActorMembership(),
                workItemId)).missingFieldResults();
    }

    @GetMapping("/work-items/{workItemId}/history")
    ReviewHistoryResponse getHistory(@PathVariable UUID workItemId) {
        return toReviewHistoryResponse(reviewWorkspaceService.getReviewHistory(
                actorResolver.requireActorMembership(),
                workItemId));
    }

    @GetMapping("/work-items/{workItemId}/decisions")
    List<ReviewDecisionResponse> getDecisionHistory(@PathVariable UUID workItemId) {
        return toReviewHistoryResponse(reviewWorkspaceService.getReviewHistory(
                actorResolver.requireActorMembership(),
                workItemId)).decisions();
    }

    @GetMapping("/work-items/{workItemId}/assignments")
    List<ReviewAssignmentResponse> getAssignmentHistory(@PathVariable UUID workItemId) {
        return toReviewHistoryResponse(reviewWorkspaceService.getReviewHistory(
                actorResolver.requireActorMembership(),
                workItemId)).assignments();
    }

    @GetMapping("/work-items/{workItemId}/signoff-requests")
    List<SignoffRequestResponse> getSignoffHistory(@PathVariable UUID workItemId) {
        return toReviewHistoryResponse(reviewWorkspaceService.getReviewHistory(
                actorResolver.requireActorMembership(),
                workItemId)).signoffRequests();
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static ReviewQueueItemResponse toReviewQueueItemResponse(ReviewQueueItemView view) {
        return new ReviewQueueItemResponse(
                toReviewWorkItemResponse(view.workItem()),
                toReviewAssignmentResponse(view.activeAssignment()),
                view.failCount(),
                view.warningCount(),
                view.openExceptionCount());
    }

    private static ReviewExceptionQueueItemResponse toReviewExceptionQueueItemResponse(ReviewExceptionQueueItemView view) {
        return new ReviewExceptionQueueItemResponse(
                toReviewQueueItemResponse(view.workItem()),
                view.exceptions().stream().map(ReviewController::toReviewExceptionResponse).toList());
    }

    private static ReviewWorkItemDetailResponse toReviewWorkItemDetailResponse(ReviewWorkItemDetailView view) {
        return new ReviewWorkItemDetailResponse(
                toReviewWorkItemResponse(view.workItem()),
                toReviewAssignmentResponse(view.activeAssignment()),
                view.latestFindings().stream().map(ReviewController::toReviewFindingResponse).toList(),
                view.latestMissingFieldResults().stream().map(ReviewController::toMissingFieldResultResponse).toList(),
                view.exceptions().stream().map(ReviewController::toReviewExceptionResponse).toList(),
                view.decisions().stream().map(ReviewController::toReviewDecisionResponse).toList(),
                view.signoffRequests().stream().map(ReviewController::toSignoffRequestResponse).toList());
    }

    private static CompletenessEvaluationResponse toCompletenessEvaluationResponse(CompletenessEvaluation evaluation) {
        return new CompletenessEvaluationResponse(
                toCompletenessCheckResultResponse(evaluation.result()),
                evaluation.findings().stream().map(ReviewController::toReviewFindingResponse).toList(),
                evaluation.missingFieldResults().stream().map(ReviewController::toMissingFieldResultResponse).toList());
    }

    private static ReviewHistoryResponse toReviewHistoryResponse(ReviewHistoryView history) {
        return new ReviewHistoryResponse(
                toReviewWorkItemResponse(history.workItem()),
                history.assignments().stream().map(ReviewController::toReviewAssignmentResponse).toList(),
                history.decisions().stream().map(ReviewController::toReviewDecisionResponse).toList(),
                history.returnForFixEvents().stream().map(ReviewController::toReturnForFixResponse).toList(),
                history.signoffRequests().stream().map(ReviewController::toSignoffRequestResponse).toList(),
                history.sourceAuditContext().stream().map(ReviewController::toAuditEventResponse).toList());
    }

    private static ReviewWorkItemResponse toReviewWorkItemResponse(ReviewWorkItem workItem) {
        if (workItem == null) {
            return null;
        }
        return new ReviewWorkItemResponse(
                workItem.getId(),
                workItem.getSourceType(),
                workItem.getSourceRecordId(),
                workItem.getBranchId(),
                workItem.getPatientId(),
                workItem.getVisitOccurrenceId(),
                workItem.getDocumentationRecordId(),
                workItem.getStatus(),
                workItem.getPriority(),
                workItem.isExceptionDriven(),
                workItem.getEnteredQueueAt(),
                workItem.getDueAt(),
                workItem.getLastActivityAt(),
                workItem.getResolvedAt());
    }

    private static ReviewAssignmentResponse toReviewAssignmentResponse(ReviewAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new ReviewAssignmentResponse(
                assignment.getId(),
                assignment.getWorkItemId(),
                assignment.getReviewerMembershipId(),
                assignment.getAssignedByMembership().getId(),
                assignment.getAssignedAt(),
                assignment.getReleasedAt(),
                assignment.getAssignmentNote());
    }

    private static ReviewDecisionResponse toReviewDecisionResponse(ReviewDecision decision) {
        return new ReviewDecisionResponse(
                decision.getId(),
                decision.getWorkItem().getId(),
                decision.getDecisionType(),
                decision.getDecidedByMembership().getId(),
                decision.getDecidedAt(),
                decision.getReasonCode(),
                decision.getReviewerNotes());
    }

    private static ReviewExceptionResponse toReviewExceptionResponse(ReviewExceptionRecord exceptionRecord) {
        return new ReviewExceptionResponse(
                exceptionRecord.getId(),
                exceptionRecord.getExceptionType(),
                exceptionRecord.getSeverity(),
                exceptionRecord.getDetectedAt(),
                exceptionRecord.getResolvedAt(),
                exceptionRecord.getResolutionNote());
    }

    private static ReviewFindingResponse toReviewFindingResponse(ReviewFinding finding) {
        return new ReviewFindingResponse(
                finding.getId(),
                finding.getFindingKind(),
                finding.getRuleCode(),
                finding.getSeverity(),
                finding.getFindingStatus(),
                finding.getFieldPath(),
                finding.getLogicalSection(),
                finding.getExplanation(),
                finding.getEvaluatedAt());
    }

    private static MissingFieldResultResponse toMissingFieldResultResponse(MissingFieldResult result) {
        return new MissingFieldResultResponse(
                result.getId(),
                result.getRuleCode(),
                result.getSeverity(),
                result.getFindingStatus(),
                result.getFieldPath(),
                result.getLogicalSection(),
                result.getExplanation());
    }

    private static CompletenessCheckResultResponse toCompletenessCheckResultResponse(CompletenessCheckResult result) {
        return new CompletenessCheckResultResponse(
                result.getId(),
                result.getEvaluatedSourceType(),
                result.getEvaluatedSourceId(),
                result.getRunNumber(),
                result.getPassCount(),
                result.getWarningCount(),
                result.getFailCount(),
                result.getEvaluatedAt());
    }

    private static ReturnForFixResponse toReturnForFixResponse(ReturnForFixEvent event) {
        return new ReturnForFixResponse(
                event.getId(),
                event.getTargetSourceType(),
                event.getTargetSourceRecordId(),
                event.getReturnReason(),
                event.getRequiredCorrections(),
                event.getReturnedByMembership().getId(),
                event.getReturnedAt(),
                event.getResubmittedAt(),
                event.getResolvedAt());
    }

    private static SignoffRequestResponse toSignoffRequestResponse(SignoffRequest signoffRequest) {
        return new SignoffRequestResponse(
                signoffRequest.getId(),
                signoffRequest.getWorkItem().getId(),
                signoffRequest.getRequestedFromMembership() == null ? null : signoffRequest.getRequestedFromMembership().getId(),
                signoffRequest.getRequestedFromRole(),
                signoffRequest.getRequestedByMembership().getId(),
                signoffRequest.getRequestedAt(),
                signoffRequest.getStatus(),
                signoffRequest.getSignoffNote(),
                signoffRequest.getCompletedByMembership() == null ? null : signoffRequest.getCompletedByMembership().getId(),
                signoffRequest.getCompletedAt());
    }

    private static AuditEventResponse toAuditEventResponse(AuditEvent auditEvent) {
        return new AuditEventResponse(
                auditEvent.getId(),
                auditEvent.getOccurredAt(),
                auditEvent.getActionType(),
                auditEvent.getTargetType(),
                auditEvent.getTargetId(),
                auditEvent.getActorEmail(),
                auditEvent.getBranchId(),
                auditEvent.getMetadataJson());
    }

    record CreateReviewWorkItemRequest(
            @NotNull ReviewSourceType sourceType,
            @NotNull UUID sourceRecordId,
            ReviewPriority priority,
            OffsetDateTime enteredQueueAt,
            OffsetDateTime dueAt,
            boolean exceptionDriven,
            ReviewExceptionType exceptionType,
            ReviewFindingSeverity exceptionSeverity) {
    }

    record AssignReviewWorkRequest(
            @NotNull UUID reviewerMembershipId,
            @NotNull OffsetDateTime assignedAt,
            String assignmentNote) {
    }

    record RecordReviewDecisionRequest(
            @NotNull ReviewDecisionType decisionType,
            @NotNull OffsetDateTime decidedAt,
            String reasonCode,
            String reviewerNotes,
            String returnReason,
            String requiredCorrections) {
    }

    record RequestSignoffApiRequest(
            UUID requestedFromMembershipId,
            AgencyRole requestedFromRole,
            @NotNull OffsetDateTime requestedAt,
            String signoffNote) {
    }

    record CompleteSignoffRequest(
            @NotNull OffsetDateTime completedAt,
            boolean approved,
            String signoffNote) {
    }

    record ReviewQueueItemResponse(
            ReviewWorkItemResponse workItem,
            ReviewAssignmentResponse activeAssignment,
            long failCount,
            long warningCount,
            long openExceptionCount) {
    }

    record ReviewExceptionQueueItemResponse(
            ReviewQueueItemResponse workItem,
            List<ReviewExceptionResponse> exceptions) {
    }

    record ReviewWorkItemDetailResponse(
            ReviewWorkItemResponse workItem,
            ReviewAssignmentResponse activeAssignment,
            List<ReviewFindingResponse> latestFindings,
            List<MissingFieldResultResponse> latestMissingFieldResults,
            List<ReviewExceptionResponse> exceptions,
            List<ReviewDecisionResponse> decisions,
            List<SignoffRequestResponse> signoffRequests) {
    }

    record ReviewWorkItemResponse(
            UUID id,
            ReviewSourceType sourceType,
            UUID sourceRecordId,
            UUID branchId,
            UUID patientId,
            UUID visitOccurrenceId,
            UUID documentationRecordId,
            ReviewLifecycleStatus status,
            ReviewPriority priority,
            boolean exceptionDriven,
            OffsetDateTime enteredQueueAt,
            OffsetDateTime dueAt,
            OffsetDateTime lastActivityAt,
            OffsetDateTime resolvedAt) {
    }

    record ReviewAssignmentResponse(
            UUID id,
            UUID workItemId,
            UUID reviewerMembershipId,
            UUID assignedByMembershipId,
            OffsetDateTime assignedAt,
            OffsetDateTime releasedAt,
            String assignmentNote) {
    }

    record ReviewDecisionResponse(
            UUID id,
            UUID workItemId,
            ReviewDecisionType decisionType,
            UUID decidedByMembershipId,
            OffsetDateTime decidedAt,
            String reasonCode,
            String reviewerNotes) {
    }

    record ReviewExceptionResponse(
            UUID id,
            ReviewExceptionType exceptionType,
            ReviewFindingSeverity severity,
            OffsetDateTime detectedAt,
            OffsetDateTime resolvedAt,
            String resolutionNote) {
    }

    record ReviewFindingResponse(
            UUID id,
            com.homehealthcare.review.foundation.ReviewFindingKind findingKind,
            String ruleCode,
            ReviewFindingSeverity severity,
            ReviewFindingStatus findingStatus,
            String fieldPath,
            String logicalSection,
            String explanation,
            OffsetDateTime evaluatedAt) {
    }

    record MissingFieldResultResponse(
            UUID id,
            String ruleCode,
            ReviewFindingSeverity severity,
            ReviewFindingStatus findingStatus,
            String fieldPath,
            String logicalSection,
            String explanation) {
    }

    record CompletenessCheckResultResponse(
            UUID id,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            int runNumber,
            int passCount,
            int warningCount,
            int failCount,
            OffsetDateTime evaluatedAt) {
    }

    record CompletenessEvaluationResponse(
            CompletenessCheckResultResponse result,
            List<ReviewFindingResponse> findings,
            List<MissingFieldResultResponse> missingFieldResults) {
    }

    record ReturnForFixResponse(
            UUID id,
            ReviewSourceType targetSourceType,
            UUID targetSourceRecordId,
            String returnReason,
            String requiredCorrections,
            UUID returnedByMembershipId,
            OffsetDateTime returnedAt,
            OffsetDateTime resubmittedAt,
            OffsetDateTime resolvedAt) {
    }

    record SignoffRequestResponse(
            UUID id,
            UUID workItemId,
            UUID requestedFromMembershipId,
            AgencyRole requestedFromRole,
            UUID requestedByMembershipId,
            OffsetDateTime requestedAt,
            SignoffRequestStatus status,
            String signoffNote,
            UUID completedByMembershipId,
            OffsetDateTime completedAt) {
    }

    record ReviewHistoryResponse(
            ReviewWorkItemResponse workItem,
            List<ReviewAssignmentResponse> assignments,
            List<ReviewDecisionResponse> decisions,
            List<ReturnForFixResponse> returnForFixEvents,
            List<SignoffRequestResponse> signoffRequests,
            List<AuditEventResponse> sourceAuditContext) {
    }

    record AuditEventResponse(
            UUID id,
            java.time.Instant occurredAt,
            String actionType,
            String targetType,
            UUID targetId,
            String actorEmail,
            UUID branchId,
            String metadataJson) {
    }
}
