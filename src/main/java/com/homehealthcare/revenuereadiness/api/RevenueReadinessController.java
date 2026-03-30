package com.homehealthcare.revenuereadiness.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.ExportPreview;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.GenerateExportCommand;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.RecalculateVisitValidationCommand;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.RefreshAuthorizationUsageCommand;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.RefreshVisitProjectionCommand;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.RevenueHistoryView;
import com.homehealthcare.revenuereadiness.domain.AuthorizationUsageSnapshot;
import com.homehealthcare.revenuereadiness.domain.InvoiceExportRow;
import com.homehealthcare.revenuereadiness.domain.PayerServiceSummaryProjection;
import com.homehealthcare.revenuereadiness.domain.PayrollExportRow;
import com.homehealthcare.revenuereadiness.domain.RevenueExceptionFlag;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjection;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionType;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/revenue-readiness")
class RevenueReadinessController {

    private final ConfigurationActorResolver actorResolver;
    private final RevenueReadinessService revenueReadinessService;

    RevenueReadinessController(ConfigurationActorResolver actorResolver, RevenueReadinessService revenueReadinessService) {
        this.actorResolver = actorResolver;
        this.revenueReadinessService = revenueReadinessService;
    }

    @GetMapping
    PagedResponse<RevenueReadinessSummaryResponse> listReadiness(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "readinessStatus", required = false) RevenueReadinessStatus readinessStatus,
            @RequestParam(name = "exceptionType", required = false) RevenueExceptionType exceptionType,
            @RequestParam(name = "payer", required = false) String payer,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                revenueReadinessService.listReadiness(
                                actorResolver.requireActorMembership(),
                                branchId,
                                readinessStatus,
                                exceptionType,
                                payer,
                                serviceLineId,
                                from,
                                to)
                        .stream()
                        .map(RevenueReadinessController::toSummaryResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/{visitOccurrenceId}")
    RevenueReadinessDetailResponse getReadinessDetail(@PathVariable UUID visitOccurrenceId) {
        RevenueReadinessProjection projection = revenueReadinessService.findReadinessByVisit(
                        actorResolver.requireActorMembership(),
                        visitOccurrenceId)
                .orElseGet(() -> revenueReadinessService.recalculateRevenueReadiness(
                        actorResolver.requireActorMembership(),
                        new RefreshVisitProjectionCommand(visitOccurrenceId, null)));
        return toDetailResponse(projection);
    }

    @PostMapping("/{visitOccurrenceId}/recalculate")
    RevenueReadinessDetailResponse recalculateReadiness(
            @PathVariable UUID visitOccurrenceId,
            @Valid @RequestBody RecalculateReadinessRequest request) {
        return toDetailResponse(revenueReadinessService.recalculateRevenueReadiness(
                actorResolver.requireActorMembership(),
                new RefreshVisitProjectionCommand(visitOccurrenceId, request.evaluatedAt())));
    }

    @GetMapping("/exceptions")
    PagedResponse<RevenueExceptionFlagResponse> listExceptionFlags(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "exceptionType", required = false) RevenueExceptionType exceptionType,
            @RequestParam(name = "payer", required = false) String payer,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                revenueReadinessService.listExceptionFlags(
                                actorResolver.requireActorMembership(),
                                branchId,
                                exceptionType,
                                payer,
                                serviceLineId,
                                from,
                                to)
                        .stream()
                        .map(RevenueReadinessController::toExceptionFlagResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/{visitOccurrenceId}/payer-service-summary")
    PayerServiceSummaryResponse getPayerServiceSummary(@PathVariable UUID visitOccurrenceId) {
        return toPayerServiceSummaryResponse(revenueReadinessService.getPayerServiceSummary(
                actorResolver.requireActorMembership(),
                visitOccurrenceId));
    }

    @GetMapping("/authorizations/{authorizationId}/usage-summary")
    AuthorizationUsageSummaryResponse getAuthorizationUsageSummary(@PathVariable UUID authorizationId) {
        return toAuthorizationUsageSummaryResponse(revenueReadinessService.getAuthorizationUsage(
                actorResolver.requireActorMembership(),
                authorizationId));
    }

    @PostMapping("/authorizations/{authorizationId}/usage-summary/recalculate")
    AuthorizationUsageSummaryResponse recalculateAuthorizationUsage(
            @PathVariable UUID authorizationId,
            @Valid @RequestBody RecalculateReadinessRequest request) {
        return toAuthorizationUsageSummaryResponse(revenueReadinessService.refreshAuthorizationUsage(
                actorResolver.requireActorMembership(),
                new RefreshAuthorizationUsageCommand(authorizationId, request.evaluatedAt())));
    }

    @GetMapping("/payroll-exports/preview")
    ExportPreviewResponse previewPayrollExport(@RequestParam("visitOccurrenceId") UUID visitOccurrenceId) {
        return toExportPreviewResponse(revenueReadinessService.previewPayrollExport(
                actorResolver.requireActorMembership(),
                visitOccurrenceId));
    }

    @PostMapping("/payroll-exports")
    PayrollExportRowResponse generatePayrollExport(@Valid @RequestBody GenerateExportRequest request) {
        return toPayrollExportRowResponse(revenueReadinessService.generatePayrollExportRow(
                actorResolver.requireActorMembership(),
                new GenerateExportCommand(request.visitOccurrenceId(), request.allowBlocked(), request.generatedAt())));
    }

    @GetMapping("/invoice-exports/preview")
    ExportPreviewResponse previewInvoiceExport(@RequestParam("visitOccurrenceId") UUID visitOccurrenceId) {
        return toExportPreviewResponse(revenueReadinessService.previewInvoiceExport(
                actorResolver.requireActorMembership(),
                visitOccurrenceId));
    }

    @PostMapping("/invoice-exports")
    InvoiceExportRowResponse generateInvoiceExport(@Valid @RequestBody GenerateExportRequest request) {
        return toInvoiceExportRowResponse(revenueReadinessService.generateInvoiceExportRow(
                actorResolver.requireActorMembership(),
                new GenerateExportCommand(request.visitOccurrenceId(), request.allowBlocked(), request.generatedAt())));
    }

    @GetMapping("/{visitOccurrenceId}/history")
    RevenueHistoryResponse getRevenueHistory(@PathVariable UUID visitOccurrenceId) {
        return toRevenueHistoryResponse(revenueReadinessService.getRevenueHistory(
                actorResolver.requireActorMembership(),
                visitOccurrenceId));
    }

    @GetMapping("/export-history")
    List<AuditEventResponse> listExportHistory() {
        return revenueReadinessService.listExportHistory(actorResolver.requireActorMembership()).stream()
                .map(RevenueReadinessController::toAuditEventResponse)
                .toList();
    }

    @GetMapping("/exception-history")
    List<AuditEventResponse> listExceptionHistory() {
        return revenueReadinessService.listExceptionHistory(actorResolver.requireActorMembership()).stream()
                .map(RevenueReadinessController::toAuditEventResponse)
                .toList();
    }

    @GetMapping("/authorizations/{authorizationId}/usage-history")
    List<AuditEventResponse> listAuthorizationUsageHistory(@PathVariable UUID authorizationId) {
        return revenueReadinessService.listAuthorizationUsageHistory(actorResolver.requireActorMembership(), authorizationId).stream()
                .map(RevenueReadinessController::toAuditEventResponse)
                .toList();
    }

    private static RevenueReadinessSummaryResponse toSummaryResponse(RevenueReadinessProjection projection) {
        return new RevenueReadinessSummaryResponse(
                projection.getVisitOccurrenceId(),
                projection.getPatient().getId(),
                projection.getBranchId(),
                projection.getServiceLine() == null ? null : projection.getServiceLine().getId(),
                projection.getReadinessStatus(),
                projection.getExceptionCount(),
                projection.getWarningCount(),
                projection.getPayerServiceSummary() == null ? null : projection.getPayerServiceSummary().getPayerName(),
                projection.getEvaluatedAt());
    }

    private static RevenueReadinessDetailResponse toDetailResponse(RevenueReadinessProjection projection) {
        return new RevenueReadinessDetailResponse(
                toSummaryResponse(projection),
                projection.getCompletionValidation() == null ? null : new ValidationResultResponse(
                        projection.getCompletionValidation().getOutcome().name(),
                        projection.getCompletionValidation().getReasonCode(),
                        projection.getCompletionValidation().getSummary(),
                        projection.getCompletionValidation().getEvaluatedAt()),
                projection.getSignatureValidation() == null ? null : new ValidationResultResponse(
                        projection.getSignatureValidation().getOutcome().name(),
                        projection.getSignatureValidation().getReasonCode(),
                        projection.getSignatureValidation().getSummary(),
                        projection.getSignatureValidation().getEvaluatedAt()),
                projection.getAuthorizationUsageSnapshot() == null ? null : toAuthorizationUsageSummaryResponse(projection.getAuthorizationUsageSnapshot()),
                projection.getPayerServiceSummary() == null ? null : toPayerServiceSummaryResponse(projection.getPayerServiceSummary()),
                projection.getExportLifecycleStatus().name());
    }

    private static RevenueExceptionFlagResponse toExceptionFlagResponse(RevenueExceptionFlag flag) {
        return new RevenueExceptionFlagResponse(
                flag.getId(),
                flag.getTargetType().name(),
                flag.getTargetId(),
                flag.getExceptionType().name(),
                flag.getSeverity().name(),
                flag.getReasonCode(),
                flag.getSummary(),
                flag.getDetectedAt(),
                flag.getClearedAt(),
                flag.getBranchId());
    }

    private static PayerServiceSummaryResponse toPayerServiceSummaryResponse(PayerServiceSummaryProjection projection) {
        return new PayerServiceSummaryResponse(
                projection.getVisitOccurrenceId(),
                projection.getPatient().getId(),
                projection.getBranchId(),
                projection.getServiceLine() == null ? null : projection.getServiceLine().getId(),
                projection.getPayerName(),
                projection.getPayerExternalId(),
                projection.getMemberPolicyNumber(),
                projection.getAuthorization() == null ? null : projection.getAuthorization().getId(),
                projection.getAuthorizationNumber(),
                projection.getServiceLineCode(),
                projection.getServiceLineName(),
                projection.isPrimaryPayer(),
                projection.getSummarizedAt());
    }

    private static AuthorizationUsageSummaryResponse toAuthorizationUsageSummaryResponse(AuthorizationUsageSnapshot snapshot) {
        return new AuthorizationUsageSummaryResponse(
                snapshot.getAuthorizationId(),
                snapshot.getPatient().getId(),
                snapshot.getBranchId(),
                snapshot.getServiceLine() == null ? null : snapshot.getServiceLine().getId(),
                snapshot.getAuthorizedUnits(),
                snapshot.getUsedUnits(),
                snapshot.getRemainingUnits(),
                snapshot.getUsagePosture().name(),
                snapshot.getCountedVisitCount(),
                snapshot.getEvaluatedAt());
    }

    private static ExportPreviewResponse toExportPreviewResponse(ExportPreview preview) {
        return new ExportPreviewResponse(
                preview.visitOccurrenceId(),
                preview.readinessStatus().name(),
                preview.payerName(),
                preview.caregiverProfileId(),
                preview.unitsOrMinutes(),
                preview.exportable(),
                preview.blockedReason());
    }

    private static PayrollExportRowResponse toPayrollExportRowResponse(PayrollExportRow row) {
        return new PayrollExportRowResponse(
                row.getId(),
                row.getVisitOccurrence().getId(),
                row.getPatient() == null ? null : row.getPatient().getId(),
                row.getCaregiverProfile() == null ? null : row.getCaregiverProfile().getId(),
                row.getBranch() == null ? null : row.getBranch().getId(),
                row.getServiceLine() == null ? null : row.getServiceLine().getId(),
                row.getScheduledStartAt(),
                row.getScheduledEndAt(),
                row.getPerformedStartAt(),
                row.getPerformedEndAt(),
                row.getDurationMinutes(),
                row.getReadinessStatus().name(),
                row.getExportLifecycleStatus().name(),
                row.getGeneratedAt());
    }

    private static InvoiceExportRowResponse toInvoiceExportRowResponse(InvoiceExportRow row) {
        return new InvoiceExportRowResponse(
                row.getId(),
                row.getVisitOccurrence().getId(),
                row.getPatient() == null ? null : row.getPatient().getId(),
                row.getCaregiverProfile() == null ? null : row.getCaregiverProfile().getId(),
                row.getBranch() == null ? null : row.getBranch().getId(),
                row.getServiceLine() == null ? null : row.getServiceLine().getId(),
                row.getPayerName(),
                row.getAuthorization() == null ? null : row.getAuthorization().getId(),
                row.getAuthorizationNumber(),
                row.getScheduledStartAt(),
                row.getScheduledEndAt(),
                row.getPerformedStartAt(),
                row.getPerformedEndAt(),
                row.getBillableUnits(),
                row.getReadinessStatus().name(),
                row.getExportLifecycleStatus().name(),
                row.getGeneratedAt());
    }

    private static RevenueHistoryResponse toRevenueHistoryResponse(RevenueHistoryView view) {
        return new RevenueHistoryResponse(
                view.projection() == null ? null : toDetailResponse(view.projection()),
                view.exceptionFlags().stream().map(RevenueReadinessController::toExceptionFlagResponse).toList(),
                view.payrollExports().stream().map(RevenueReadinessController::toPayrollExportRowResponse).toList(),
                view.invoiceExports().stream().map(RevenueReadinessController::toInvoiceExportRowResponse).toList(),
                view.auditEvents().stream().map(RevenueReadinessController::toAuditEventResponse).toList());
    }

    private static AuditEventResponse toAuditEventResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActionType(),
                event.getTargetType(),
                event.getTargetId(),
                event.getBranchId(),
                event.getOccurredAt().toString(),
                event.getMetadataJson());
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        List<T> content = items.subList(fromIndex, toIndex);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / (double) size);
        return new PagedResponse<>(content, page, size, items.size(), totalPages);
    }

    record RecalculateReadinessRequest(OffsetDateTime evaluatedAt) {
    }

    record GenerateExportRequest(
            @NotNull UUID visitOccurrenceId,
            boolean allowBlocked,
            OffsetDateTime generatedAt) {
    }

    record RevenueReadinessSummaryResponse(
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            RevenueReadinessStatus readinessStatus,
            int exceptionCount,
            int warningCount,
            String payerName,
            OffsetDateTime evaluatedAt) {
    }

    record ValidationResultResponse(
            String outcome,
            String reasonCode,
            String summary,
            OffsetDateTime evaluatedAt) {
    }

    record RevenueReadinessDetailResponse(
            RevenueReadinessSummaryResponse summary,
            ValidationResultResponse completionValidation,
            ValidationResultResponse signatureValidation,
            AuthorizationUsageSummaryResponse authorizationUsage,
            PayerServiceSummaryResponse payerServiceSummary,
            String exportLifecycleStatus) {
    }

    record RevenueExceptionFlagResponse(
            UUID id,
            String targetType,
            UUID targetId,
            String exceptionType,
            String severity,
            String reasonCode,
            String summary,
            OffsetDateTime detectedAt,
            OffsetDateTime clearedAt,
            UUID branchId) {
    }

    record PayerServiceSummaryResponse(
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            UUID authorizationId,
            String authorizationNumber,
            String serviceLineCode,
            String serviceLineName,
            boolean primaryPayer,
            OffsetDateTime summarizedAt) {
    }

    record AuthorizationUsageSummaryResponse(
            UUID authorizationId,
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            Integer authorizedUnits,
            int usedUnits,
            Integer remainingUnits,
            String usagePosture,
            int countedVisitCount,
            OffsetDateTime evaluatedAt) {
    }

    record ExportPreviewResponse(
            UUID visitOccurrenceId,
            String readinessStatus,
            String payerName,
            UUID caregiverProfileId,
            int unitsOrMinutes,
            boolean exportable,
            String blockedReason) {
    }

    record PayrollExportRowResponse(
            UUID id,
            UUID visitOccurrenceId,
            UUID patientId,
            UUID caregiverProfileId,
            UUID branchId,
            UUID serviceLineId,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            OffsetDateTime performedStartAt,
            OffsetDateTime performedEndAt,
            int durationMinutes,
            String readinessStatus,
            String exportLifecycleStatus,
            OffsetDateTime generatedAt) {
    }

    record InvoiceExportRowResponse(
            UUID id,
            UUID visitOccurrenceId,
            UUID patientId,
            UUID caregiverProfileId,
            UUID branchId,
            UUID serviceLineId,
            String payerName,
            UUID authorizationId,
            String authorizationNumber,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            OffsetDateTime performedStartAt,
            OffsetDateTime performedEndAt,
            int billableUnits,
            String readinessStatus,
            String exportLifecycleStatus,
            OffsetDateTime generatedAt) {
    }

    record RevenueHistoryResponse(
            RevenueReadinessDetailResponse projection,
            List<RevenueExceptionFlagResponse> exceptionFlags,
            List<PayrollExportRowResponse> payrollExports,
            List<InvoiceExportRowResponse> invoiceExports,
            List<AuditEventResponse> auditEvents) {
    }

    record AuditEventResponse(
            UUID id,
            String actionType,
            String targetType,
            UUID targetId,
            UUID branchId,
            String occurredAt,
            String metadataJson) {
    }
}
