package com.homehealthcare.documentation.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.documentation.application.DocumentationService;
import com.homehealthcare.documentation.application.DocumentationService.CreateDocumentationRecordCommand;
import com.homehealthcare.documentation.application.DocumentationService.DocumentationAggregate;
import com.homehealthcare.documentation.application.DocumentationService.DocumentationSummary;
import com.homehealthcare.documentation.application.DocumentationService.FieldDefinitionCommand;
import com.homehealthcare.documentation.application.DocumentationService.FieldResponseCommand;
import com.homehealthcare.documentation.application.DocumentationService.ManageTaskLibraryItemCommand;
import com.homehealthcare.documentation.application.DocumentationService.ManageTemplateCommand;
import com.homehealthcare.documentation.application.DocumentationService.PrintableSummaryProjection;
import com.homehealthcare.documentation.application.DocumentationService.SaveDocumentationDraftCommand;
import com.homehealthcare.documentation.application.DocumentationService.SectionCommand;
import com.homehealthcare.documentation.application.DocumentationService.TaskResponseCommand;
import com.homehealthcare.documentation.application.DocumentationService.TemplateAggregate;
import com.homehealthcare.documentation.application.DocumentationService.TemplateTaskCommand;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLink;
import com.homehealthcare.documentation.domain.DocumentationFieldResponse;
import com.homehealthcare.documentation.domain.DocumentationTaskResponse;
import com.homehealthcare.documentation.domain.DocumentationTemplateField;
import com.homehealthcare.documentation.domain.DocumentationTemplateSection;
import com.homehealthcare.documentation.domain.DocumentationTemplateTask;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.foundation.DocumentationFieldType;
import com.homehealthcare.documentation.foundation.DocumentationResponseState;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api")
class DocumentationController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final DocumentationService documentationService;

    DocumentationController(
            ConfigurationActorResolver configurationActorResolver,
            DocumentationService documentationService) {
        this.configurationActorResolver = configurationActorResolver;
        this.documentationService = documentationService;
    }

    @GetMapping("/documentation/templates")
    PagedResponse<TemplateResponse> listTemplates(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "templateType", required = false) DocumentationTemplateType templateType,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "visitTypeId", required = false) UUID visitTypeId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        List<TemplateResponse> items = documentationService.listTemplates(
                        configurationActorResolver.requireActorMembership(),
                        search,
                        status,
                        templateType,
                        branchId,
                        visitTypeId)
                .stream()
                .map(DocumentationController::toTemplateResponse)
                .toList();
        return page(items, page, size);
    }

    @GetMapping("/documentation/templates/{templateId}")
    TemplateAggregateResponse getTemplate(@PathVariable UUID templateId) {
        return toTemplateAggregateResponse(documentationService.getTemplateAggregate(
                configurationActorResolver.requireActorMembership(),
                templateId));
    }

    @PostMapping("/documentation/templates")
    TemplateAggregateResponse createTemplate(@Valid @RequestBody ManageTemplateRequest request) {
        return toTemplateAggregateResponse(documentationService.createTemplate(
                configurationActorResolver.requireActorMembership(),
                toManageTemplateCommand(request)));
    }

    @PutMapping("/documentation/templates/{templateId}")
    TemplateAggregateResponse updateTemplate(@PathVariable UUID templateId, @Valid @RequestBody ManageTemplateRequest request) {
        return toTemplateAggregateResponse(documentationService.updateTemplate(
                configurationActorResolver.requireActorMembership(),
                templateId,
                toManageTemplateCommand(request)));
    }

    @DeleteMapping("/documentation/templates/{templateId}")
    TemplateResponse deactivateTemplate(@PathVariable UUID templateId) {
        return toTemplateResponse(documentationService.deactivateTemplate(
                configurationActorResolver.requireActorMembership(),
                templateId));
    }

    @GetMapping("/documentation/task-library")
    PagedResponse<TaskLibraryItemResponse> listTaskLibrary(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "category", required = false) TaskTemplateCategory category,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "visitTypeId", required = false) UUID visitTypeId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        List<TaskLibraryItemResponse> items = documentationService.listTaskLibraryItems(
                        configurationActorResolver.requireActorMembership(),
                        search,
                        status,
                        category,
                        serviceLineId,
                        visitTypeId)
                .stream()
                .map(DocumentationController::toTaskLibraryItemResponse)
                .toList();
        return page(items, page, size);
    }

    @GetMapping("/documentation/task-library/{taskTemplateId}")
    TaskLibraryItemResponse getTaskLibraryItem(@PathVariable UUID taskTemplateId) {
        return toTaskLibraryItemResponse(documentationService.getTaskLibraryItem(
                configurationActorResolver.requireActorMembership(),
                taskTemplateId));
    }

    @PostMapping("/documentation/task-library")
    TaskLibraryItemResponse createTaskLibraryItem(@Valid @RequestBody ManageTaskLibraryItemRequest request) {
        return toTaskLibraryItemResponse(documentationService.createTaskLibraryItem(
                configurationActorResolver.requireActorMembership(),
                new ManageTaskLibraryItemCommand(
                        request.serviceLineId(),
                        request.visitTypeId(),
                        request.name(),
                        request.code(),
                        request.description(),
                        request.category(),
                        request.displayOrder(),
                        request.defaultSortOrder(),
                        request.defaultCompletionExpectation(),
                        request.requiredByDefault())));
    }

    @PutMapping("/documentation/task-library/{taskTemplateId}")
    TaskLibraryItemResponse updateTaskLibraryItem(@PathVariable UUID taskTemplateId, @Valid @RequestBody ManageTaskLibraryItemRequest request) {
        return toTaskLibraryItemResponse(documentationService.updateTaskLibraryItem(
                configurationActorResolver.requireActorMembership(),
                taskTemplateId,
                new ManageTaskLibraryItemCommand(
                        request.serviceLineId(),
                        request.visitTypeId(),
                        request.name(),
                        request.code(),
                        request.description(),
                        request.category(),
                        request.displayOrder(),
                        request.defaultSortOrder(),
                        request.defaultCompletionExpectation(),
                        request.requiredByDefault())));
    }

    @DeleteMapping("/documentation/task-library/{taskTemplateId}")
    TaskLibraryItemResponse deactivateTaskLibraryItem(@PathVariable UUID taskTemplateId) {
        return toTaskLibraryItemResponse(documentationService.deactivateTaskLibraryItem(
                configurationActorResolver.requireActorMembership(),
                taskTemplateId));
    }

    @GetMapping("/visit-documentation")
    PagedResponse<DocumentationSummaryResponse> listDocumentation(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "caregiverMembershipId", required = false) UUID caregiverMembershipId,
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "status", required = false) DocumentationRecordStatus status,
            @RequestParam(name = "templateId", required = false) UUID templateId,
            @RequestParam(name = "visitTypeId", required = false) UUID visitTypeId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        List<DocumentationSummaryResponse> items = documentationService.listDocumentation(
                        configurationActorResolver.requireActorMembership(),
                        from,
                        to,
                        branchId,
                        caregiverMembershipId,
                        patientId,
                        status,
                        templateId,
                        visitTypeId)
                .stream()
                .map(DocumentationController::toDocumentationSummaryResponse)
                .toList();
        return page(items, page, size);
    }

    @GetMapping("/visit-documentation/{documentationRecordId}")
    DocumentationAggregateResponse getDocumentation(@PathVariable UUID documentationRecordId) {
        return toDocumentationAggregateResponse(documentationService.getDocumentationAggregate(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId));
    }

    @GetMapping("/visit-documentation/by-visit")
    DocumentationAggregateResponse loadDocumentationForVisit(
            @RequestParam("visitOccurrenceId") UUID visitOccurrenceId,
            @RequestParam(name = "selectedTemplateId", required = false) UUID selectedTemplateId) {
        return toDocumentationAggregateResponse(documentationService.loadDocumentationForVisit(
                configurationActorResolver.requireActorMembership(),
                visitOccurrenceId,
                selectedTemplateId));
    }

    @PostMapping("/visit-documentation")
    DocumentationAggregateResponse createDocumentation(@Valid @RequestBody CreateDocumentationRecordRequest request) {
        return toDocumentationAggregateResponse(documentationService.createDocumentationRecord(
                configurationActorResolver.requireActorMembership(),
                new CreateDocumentationRecordCommand(
                        request.visitOccurrenceId(),
                        request.selectedTemplateId(),
                        request.startedAt())));
    }

    @PutMapping("/visit-documentation/{documentationRecordId}/draft")
    DocumentationAggregateResponse saveDraft(
            @PathVariable UUID documentationRecordId,
            @Valid @RequestBody SaveDocumentationDraftRequest request) {
        return toDocumentationAggregateResponse(documentationService.saveDraft(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId,
                new SaveDocumentationDraftCommand(
                        request.fieldResponses().stream()
                                .map(item -> new FieldResponseCommand(
                                        item.templateFieldId(),
                                        item.normalizedValue(),
                                        item.displayValue(),
                                        item.responseNotes(),
                                        item.completionState(),
                                        item.completedAt()))
                                .toList(),
                        request.taskResponses().stream()
                                .map(item -> new TaskResponseCommand(
                                        item.templateTaskId(),
                                        item.completionState(),
                                        item.completionNotes(),
                                        item.completedAt()))
                                .toList(),
                        request.savedAt())));
    }

    @PostMapping("/visit-documentation/{documentationRecordId}/submit")
    DocumentationAggregateResponse submitDocumentation(
            @PathVariable UUID documentationRecordId,
            @Valid @RequestBody SubmitDocumentationRequest request) {
        return toDocumentationAggregateResponse(documentationService.submitDocumentation(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId,
                request.submittedAt()));
    }

    @GetMapping("/visit-documentation/{documentationRecordId}/attachments")
    List<AttachmentLinkResponse> listAttachments(@PathVariable UUID documentationRecordId) {
        return toDocumentationAggregateResponse(documentationService.getDocumentationAggregate(
                        configurationActorResolver.requireActorMembership(),
                        documentationRecordId))
                .attachmentLinks();
    }

    @PostMapping("/visit-documentation/{documentationRecordId}/attachments/patient-links")
    AttachmentLinkResponse linkPatientAttachment(
            @PathVariable UUID documentationRecordId,
            @Valid @RequestBody LinkPatientAttachmentRequest request) {
        return toAttachmentLinkResponse(documentationService.linkPatientAttachment(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId,
                request.patientAttachmentId(),
                request.caption(),
                request.description()));
    }

    @PostMapping("/visit-documentation/{documentationRecordId}/attachments/mobile-links")
    AttachmentLinkResponse linkMobileArtifact(
            @PathVariable UUID documentationRecordId,
            @Valid @RequestBody LinkMobileArtifactRequest request) {
        return toAttachmentLinkResponse(documentationService.linkMobileArtifact(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId,
                request.mobileArtifactId(),
                request.caption(),
                request.description()));
    }

    @DeleteMapping("/visit-documentation/{documentationRecordId}/attachments/{attachmentLinkId}")
    ResponseEntity<Void> unlinkAttachment(@PathVariable UUID documentationRecordId, @PathVariable UUID attachmentLinkId) {
        documentationService.unlinkAttachment(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId,
                attachmentLinkId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/visit-documentation/{documentationRecordId}/printable-summary")
    PrintableSummaryProjection printableSummary(@PathVariable UUID documentationRecordId) {
        return documentationService.generatePrintableSummary(configurationActorResolver.requireActorMembership(), documentationRecordId);
    }

    @GetMapping(value = "/visit-documentation/{documentationRecordId}/printable-summary/export", produces = MediaType.TEXT_HTML_VALUE)
    String exportPrintableSummary(@PathVariable UUID documentationRecordId) {
        PrintableSummaryProjection projection = documentationService.generatePrintableSummary(
                configurationActorResolver.requireActorMembership(),
                documentationRecordId);
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h1>").append(escape(projection.templateTitle())).append("</h1>");
        html.append("<p>").append(escape(projection.header().patientDisplayName())).append("</p>");
        html.append("<ul>");
        projection.fields().forEach(field -> html.append("<li>")
                .append(escape(field.fieldKey()))
                .append(": ")
                .append(escape(field.displayValue()))
                .append("</li>"));
        html.append("</ul><ol>");
        projection.tasks().forEach(task -> html.append("<li>")
                .append(escape(task.taskTitle()))
                .append(" [")
                .append(escape(task.completionState()))
                .append("]</li>"));
        html.append("</ol></body></html>");
        return html.toString();
    }

    private static ManageTemplateCommand toManageTemplateCommand(ManageTemplateRequest request) {
        return new ManageTemplateCommand(
                request.name(),
                request.code(),
                request.templateType(),
                request.structuredDefinitionJson(),
                request.displayOrder(),
                request.serviceLineId(),
                request.visitTypeId(),
                request.branchId(),
                request.helpText(),
                request.allowedActorRoles(),
                request.requiresSignatureVerification(),
                request.sections().stream()
                        .map(item -> new SectionCommand(item.sectionKey(), item.title(), item.helpText(), item.sortOrder()))
                        .toList(),
                request.fields().stream()
                        .map(item -> new FieldDefinitionCommand(
                                item.sectionKey(),
                                item.fieldKey(),
                                item.label(),
                                item.fieldType(),
                                item.requiredField(),
                                item.sortOrder(),
                                item.optionsJson(),
                                item.helpText(),
                                item.visibleActorRoles(),
                                item.editableActorRoles()))
                        .toList(),
                request.tasks().stream()
                        .map(item -> new TemplateTaskCommand(
                                item.sectionKey(),
                                item.taskTemplateId(),
                                item.titleOverride(),
                                item.descriptionOverride(),
                                item.requiredOverride(),
                                item.sortOrder()))
                        .toList());
    }

    private static TemplateAggregateResponse toTemplateAggregateResponse(TemplateAggregate aggregate) {
        return new TemplateAggregateResponse(
                toTemplateResponse(aggregate.template()),
                aggregate.sections().stream().map(DocumentationController::toSectionResponse).toList(),
                aggregate.fields().stream().map(DocumentationController::toFieldDefinitionResponse).toList(),
                aggregate.tasks().stream().map(DocumentationController::toTemplateTaskResponse).toList());
    }

    private static TemplateResponse toTemplateResponse(DocumentationTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getAgencyId(),
                template.getName(),
                template.getCode(),
                template.getTemplateType(),
                template.getStructuredDefinitionJson(),
                template.getVersion(),
                template.getStatus(),
                template.getDisplayOrder(),
                template.getServiceLineId(),
                template.getVisitTypeId(),
                template.getBranchId(),
                template.getHelpText(),
                template.allowedActorRoleSet(),
                template.isRequiresSignatureVerification());
    }

    private static SectionResponse toSectionResponse(DocumentationTemplateSection section) {
        return new SectionResponse(section.getId(), section.getSectionKey(), section.getTitle(), section.getHelpText(), section.getSortOrder());
    }

    private static FieldDefinitionResponse toFieldDefinitionResponse(DocumentationTemplateField field) {
        return new FieldDefinitionResponse(
                field.getId(),
                field.getDocumentationTemplateId(),
                field.getSection() == null ? null : field.getSection().getId(),
                field.getFieldKey(),
                field.getLabel(),
                field.getFieldType(),
                field.isRequiredField(),
                field.getSortOrder(),
                field.getOptionsJson(),
                field.getHelpText(),
                field.visibleActorRoleSet(),
                field.editableActorRoleSet());
    }

    private static TemplateTaskResponse toTemplateTaskResponse(DocumentationTemplateTask task) {
        return new TemplateTaskResponse(
                task.getId(),
                task.getDocumentationTemplateId(),
                task.getSection() == null ? null : task.getSection().getId(),
                task.getTaskTemplate() == null ? null : task.getTaskTemplate().getId(),
                task.effectiveTitle(),
                task.effectiveDescription(),
                task.effectiveRequired(),
                task.getSortOrder());
    }

    private static TaskLibraryItemResponse toTaskLibraryItemResponse(TaskTemplate template) {
        return new TaskLibraryItemResponse(
                template.getId(),
                template.getAgencyId(),
                template.getServiceLine() == null ? null : template.getServiceLine().getId(),
                template.getVisitType() == null ? null : template.getVisitType().getId(),
                template.getName(),
                template.getCode(),
                template.getDescription(),
                template.getCategory(),
                template.getStatus(),
                template.getDisplayOrder(),
                template.getDefaultSortOrder(),
                template.getDefaultCompletionExpectation(),
                template.isRequiredByDefault());
    }

    private static DocumentationAggregateResponse toDocumentationAggregateResponse(DocumentationAggregate aggregate) {
        return new DocumentationAggregateResponse(
                toDocumentationRecordResponse(aggregate.record()),
                aggregate.fieldResponses().stream().map(DocumentationController::toFieldResponse).toList(),
                aggregate.taskResponses().stream().map(DocumentationController::toTaskResponse).toList(),
                aggregate.attachmentLinks().stream().map(DocumentationController::toAttachmentLinkResponse).toList());
    }

    private static DocumentationRecordResponse toDocumentationRecordResponse(VisitDocumentationRecord record) {
        return new DocumentationRecordResponse(
                record.getId(),
                record.getVisitOccurrenceId(),
                record.getPatientId(),
                record.getBranchId(),
                record.getSelectedTemplateId(),
                record.getAuthorMembership().getId(),
                record.getLastEditorMembership().getId(),
                record.getStatus(),
                record.getStartedAt(),
                record.getSubmittedAt(),
                record.getLastSavedAt(),
                record.getPrintableSummaryVersion());
    }

    private static FieldResponseView toFieldResponse(DocumentationFieldResponse response) {
        return new FieldResponseView(
                response.getId(),
                response.getTemplateFieldId(),
                response.getFieldKey(),
                response.getNormalizedValue(),
                response.getDisplayValue(),
                response.getResponseNotes(),
                response.getCompletionState(),
                response.getCompletedAt());
    }

    private static TaskResponseView toTaskResponse(DocumentationTaskResponse response) {
        return new TaskResponseView(
                response.getId(),
                response.getTemplateTaskId(),
                response.getTaskTitle(),
                response.getTaskDescription(),
                response.isCompletionRequired(),
                response.getCompletionState(),
                response.getCompletionNotes(),
                response.getCompletedAt(),
                response.getSortOrder());
    }

    private static AttachmentLinkResponse toAttachmentLinkResponse(DocumentationAttachmentLink link) {
        return new AttachmentLinkResponse(
                link.getId(),
                link.getDocumentationRecord().getId(),
                link.getPatientAttachment() == null ? null : link.getPatientAttachment().getId(),
                link.getMobileArtifact() == null ? null : link.getMobileArtifact().getId(),
                link.getCaption(),
                link.getDescription(),
                link.getLinkedAt());
    }

    private static DocumentationSummaryResponse toDocumentationSummaryResponse(DocumentationSummary summary) {
        return new DocumentationSummaryResponse(
                summary.id(),
                summary.visitOccurrenceId(),
                summary.patientId(),
                summary.patientFirstName(),
                summary.patientLastName(),
                summary.branchId(),
                summary.branchName(),
                summary.templateId(),
                summary.templateName(),
                summary.status(),
                summary.lastSavedAt(),
                summary.submittedAt(),
                summary.authorMembershipId(),
                summary.authorEmail());
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    record ManageTemplateRequest(
            @NotBlank String name,
            @NotBlank String code,
            @NotNull DocumentationTemplateType templateType,
            @NotBlank String structuredDefinitionJson,
            int displayOrder,
            UUID serviceLineId,
            UUID visitTypeId,
            UUID branchId,
            String helpText,
            Set<AgencyRole> allowedActorRoles,
            boolean requiresSignatureVerification,
            @NotEmpty List<@Valid SectionRequest> sections,
            @NotEmpty List<@Valid FieldDefinitionRequest> fields,
            List<@Valid TemplateTaskRequest> tasks) {
        public ManageTemplateRequest {
            tasks = tasks == null ? List.of() : List.copyOf(tasks);
        }
    }

    record SectionRequest(@NotBlank String sectionKey, @NotBlank String title, String helpText, int sortOrder) {
    }

    record FieldDefinitionRequest(
            String sectionKey,
            @NotBlank String fieldKey,
            @NotBlank String label,
            @NotNull DocumentationFieldType fieldType,
            boolean requiredField,
            int sortOrder,
            String optionsJson,
            String helpText,
            Set<AgencyRole> visibleActorRoles,
            Set<AgencyRole> editableActorRoles) {
    }

    record TemplateTaskRequest(
            String sectionKey,
            UUID taskTemplateId,
            String titleOverride,
            String descriptionOverride,
            Boolean requiredOverride,
            int sortOrder) {
    }

    record ManageTaskLibraryItemRequest(
            UUID serviceLineId,
            UUID visitTypeId,
            @NotBlank String name,
            @NotBlank String code,
            String description,
            @NotNull TaskTemplateCategory category,
            int displayOrder,
            int defaultSortOrder,
            String defaultCompletionExpectation,
            boolean requiredByDefault) {
    }

    record CreateDocumentationRecordRequest(@NotNull UUID visitOccurrenceId, @NotNull UUID selectedTemplateId, OffsetDateTime startedAt) {
    }

    record SaveDocumentationDraftRequest(
            List<@Valid FieldResponseRequest> fieldResponses,
            List<@Valid TaskResponseRequest> taskResponses,
            OffsetDateTime savedAt) {
        public SaveDocumentationDraftRequest {
            fieldResponses = fieldResponses == null ? List.of() : List.copyOf(fieldResponses);
            taskResponses = taskResponses == null ? List.of() : List.copyOf(taskResponses);
        }
    }

    record FieldResponseRequest(
            @NotNull UUID templateFieldId,
            String normalizedValue,
            String displayValue,
            String responseNotes,
            DocumentationResponseState completionState,
            OffsetDateTime completedAt) {
    }

    record TaskResponseRequest(
            @NotNull UUID templateTaskId,
            DocumentationResponseState completionState,
            String completionNotes,
            OffsetDateTime completedAt) {
    }

    record SubmitDocumentationRequest(OffsetDateTime submittedAt) {
    }

    record LinkPatientAttachmentRequest(@NotNull UUID patientAttachmentId, String caption, String description) {
    }

    record LinkMobileArtifactRequest(@NotNull UUID mobileArtifactId, String caption, String description) {
    }

    record TemplateAggregateResponse(
            TemplateResponse template,
            List<SectionResponse> sections,
            List<FieldDefinitionResponse> fields,
            List<TemplateTaskResponse> tasks) {
    }

    record TemplateResponse(
            UUID id,
            UUID agencyId,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int version,
            ConfigurationStatus status,
            int displayOrder,
            UUID serviceLineId,
            UUID visitTypeId,
            UUID branchId,
            String helpText,
            Set<AgencyRole> allowedActorRoles,
            boolean requiresSignatureVerification) {
    }

    record SectionResponse(UUID id, String sectionKey, String title, String helpText, int sortOrder) {
    }

    record FieldDefinitionResponse(
            UUID id,
            UUID documentationTemplateId,
            UUID sectionId,
            String fieldKey,
            String label,
            DocumentationFieldType fieldType,
            boolean requiredField,
            int sortOrder,
            String optionsJson,
            String helpText,
            Set<AgencyRole> visibleActorRoles,
            Set<AgencyRole> editableActorRoles) {
    }

    record TemplateTaskResponse(
            UUID id,
            UUID documentationTemplateId,
            UUID sectionId,
            UUID taskTemplateId,
            String effectiveTitle,
            String effectiveDescription,
            boolean effectiveRequired,
            int sortOrder) {
    }

    record TaskLibraryItemResponse(
            UUID id,
            UUID agencyId,
            UUID serviceLineId,
            UUID visitTypeId,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            ConfigurationStatus status,
            int displayOrder,
            int defaultSortOrder,
            String defaultCompletionExpectation,
            boolean requiredByDefault) {
    }

    record DocumentationAggregateResponse(
            DocumentationRecordResponse record,
            List<FieldResponseView> fieldResponses,
            List<TaskResponseView> taskResponses,
            List<AttachmentLinkResponse> attachmentLinks) {
    }

    record DocumentationRecordResponse(
            UUID id,
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            UUID selectedTemplateId,
            UUID authorMembershipId,
            UUID lastEditorMembershipId,
            DocumentationRecordStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            OffsetDateTime lastSavedAt,
            int printableSummaryVersion) {
    }

    record FieldResponseView(
            UUID id,
            UUID templateFieldId,
            String fieldKey,
            String normalizedValue,
            String displayValue,
            String responseNotes,
            DocumentationResponseState completionState,
            OffsetDateTime completedAt) {
    }

    record TaskResponseView(
            UUID id,
            UUID templateTaskId,
            String taskTitle,
            String taskDescription,
            boolean completionRequired,
            DocumentationResponseState completionState,
            String completionNotes,
            OffsetDateTime completedAt,
            int sortOrder) {
    }

    record AttachmentLinkResponse(
            UUID id,
            UUID documentationRecordId,
            UUID patientAttachmentId,
            UUID mobileArtifactId,
            String caption,
            String description,
            OffsetDateTime linkedAt) {
    }

    record DocumentationSummaryResponse(
            UUID id,
            UUID visitOccurrenceId,
            UUID patientId,
            String patientFirstName,
            String patientLastName,
            UUID branchId,
            String branchName,
            UUID templateId,
            String templateName,
            DocumentationRecordStatus status,
            OffsetDateTime lastSavedAt,
            OffsetDateTime submittedAt,
            UUID authorMembershipId,
            String authorEmail) {
    }
}
