package com.homehealthcare.documentation.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLink;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLinkRepository;
import com.homehealthcare.documentation.domain.DocumentationFieldResponse;
import com.homehealthcare.documentation.domain.DocumentationFieldResponseRepository;
import com.homehealthcare.documentation.domain.DocumentationTaskResponse;
import com.homehealthcare.documentation.domain.DocumentationTaskResponseRepository;
import com.homehealthcare.documentation.domain.DocumentationTemplateField;
import com.homehealthcare.documentation.domain.DocumentationTemplateFieldRepository;
import com.homehealthcare.documentation.domain.DocumentationTemplateSection;
import com.homehealthcare.documentation.domain.DocumentationTemplateSectionRepository;
import com.homehealthcare.documentation.domain.DocumentationTemplateTask;
import com.homehealthcare.documentation.domain.DocumentationTemplateTaskRepository;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.domain.VisitDocumentationRecordRepository;
import com.homehealthcare.documentation.foundation.DocumentationAuditService;
import com.homehealthcare.documentation.foundation.DocumentationFieldType;
import com.homehealthcare.documentation.foundation.DocumentationResponseState;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.documentation.foundation.Epic8DocumentationTargetType;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.SignatureVerificationLink;
import com.homehealthcare.evv.domain.SignatureVerificationLinkRepository;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
public class DocumentationService {

    private final DocumentationTemplateRepository documentationTemplateRepository;
    private final DocumentationTemplateSectionRepository documentationTemplateSectionRepository;
    private final DocumentationTemplateFieldRepository documentationTemplateFieldRepository;
    private final DocumentationTemplateTaskRepository documentationTemplateTaskRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final VisitDocumentationRecordRepository visitDocumentationRecordRepository;
    private final DocumentationFieldResponseRepository documentationFieldResponseRepository;
    private final DocumentationTaskResponseRepository documentationTaskResponseRepository;
    private final DocumentationAttachmentLinkRepository documentationAttachmentLinkRepository;
    private final PatientAttachmentRepository patientAttachmentRepository;
    private final MobileFieldArtifactRepository mobileFieldArtifactRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final BranchRepository branchRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final SignatureVerificationLinkRepository signatureVerificationLinkRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final DocumentationAuditService documentationAuditService;

    @Transactional
    public TemplateAggregate createTemplate(@NotNull AgencyMembership actorMembership, @Valid ManageTemplateCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_DOCUMENTATION_TEMPLATES);
        assertTemplateUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);
        DocumentationTemplate template = documentationTemplateRepository.saveAndFlush(DocumentationTemplate.createDraft(
                actorMembership.getAgency(),
                command.name(),
                command.code(),
                command.templateType(),
                command.structuredDefinitionJson(),
                command.displayOrder(),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                command.helpText(),
                command.allowedActorRoles(),
                command.requiresSignatureVerification()));
        TemplateAggregate aggregate = persistTemplateStructure(template, command);
        documentationAuditService.recordTemplateCreated(
                actorMembership,
                command.templateType() == DocumentationTemplateType.CUSTOM_FORM
                        ? Epic8DocumentationTargetType.FORM_TEMPLATE
                        : Epic8DocumentationTargetType.VISIT_NOTE_TEMPLATE,
                template.getId(),
                template.getBranchId(),
                "{\"code\":\"" + template.getCode() + "\",\"version\":" + template.getVersion() + "}");
        return aggregate;
    }

    @Transactional
    public TemplateAggregate updateTemplate(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID templateId,
            @Valid ManageTemplateCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_DOCUMENTATION_TEMPLATES);
        DocumentationTemplate template = resolveTemplate(actorMembership.getAgencyId(), templateId);
        assertTemplateUnique(actorMembership.getAgencyId(), command.name(), command.code(), templateId);
        template.updateDraft(
                command.name(),
                command.code(),
                command.templateType(),
                command.structuredDefinitionJson(),
                command.displayOrder(),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                resolveBranch(actorMembership.getAgencyId(), command.branchId()),
                command.helpText(),
                command.allowedActorRoles(),
                command.requiresSignatureVerification());
        DocumentationTemplate savedTemplate = documentationTemplateRepository.saveAndFlush(template);
        TemplateAggregate aggregate = persistTemplateStructure(savedTemplate, command);
        documentationAuditService.recordTemplateUpdated(
                actorMembership,
                command.templateType() == DocumentationTemplateType.CUSTOM_FORM
                        ? Epic8DocumentationTargetType.FORM_TEMPLATE
                        : Epic8DocumentationTargetType.VISIT_NOTE_TEMPLATE,
                savedTemplate.getId(),
                savedTemplate.getBranchId(),
                "{\"code\":\"" + savedTemplate.getCode() + "\",\"version\":" + savedTemplate.getVersion() + "}");
        return aggregate;
    }

    @Transactional
    public TaskTemplate createTaskLibraryItem(@NotNull AgencyMembership actorMembership, @Valid ManageTaskLibraryItemCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_DOCUMENTATION_TASK_LIBRARY);
        assertTaskUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);
        TaskTemplate saved = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                actorMembership.getAgency(),
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                command.name(),
                command.code(),
                command.description(),
                command.category(),
                command.displayOrder(),
                command.defaultSortOrder(),
                command.defaultCompletionExpectation(),
                command.requiredByDefault()));
        documentationAuditService.recordTaskLibraryUpdated(
                actorMembership,
                saved.getId(),
                null,
                "{\"code\":\"" + saved.getCode() + "\",\"category\":\"" + saved.getCategory().name() + "\"}");
        return saved;
    }

    @Transactional
    public TaskTemplate updateTaskLibraryItem(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID taskTemplateId,
            @Valid ManageTaskLibraryItemCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_DOCUMENTATION_TASK_LIBRARY);
        TaskTemplate taskTemplate = resolveTaskTemplate(actorMembership.getAgencyId(), taskTemplateId);
        assertTaskUnique(actorMembership.getAgencyId(), command.name(), command.code(), taskTemplateId);
        taskTemplate.updateDetails(
                resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId()),
                resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId()),
                command.name(),
                command.code(),
                command.description(),
                command.category(),
                command.displayOrder(),
                command.defaultSortOrder(),
                command.defaultCompletionExpectation(),
                command.requiredByDefault());
        TaskTemplate saved = taskTemplateRepository.saveAndFlush(taskTemplate);
        documentationAuditService.recordTaskLibraryUpdated(
                actorMembership,
                saved.getId(),
                null,
                "{\"code\":\"" + saved.getCode() + "\",\"category\":\"" + saved.getCategory().name() + "\"}");
        return saved;
    }

    @Transactional
    public DocumentationAggregate createDocumentationRecord(
            @NotNull AgencyMembership actorMembership,
            @Valid CreateDocumentationRecordCommand command) {
        requirePermission(actorMembership, AgencyPermission.DRAFT_VISIT_DOCUMENTATION);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), command.visitOccurrenceId());
        DocumentationTemplate template = resolveTemplate(actorMembership.getAgencyId(), command.selectedTemplateId());
        authorizeTemplateUse(actorMembership, template, AgencyPermission.DRAFT_VISIT_DOCUMENTATION);
        ensureTemplateMatchesVisit(template, visit);
        if (visitDocumentationRecordRepository.existsByVisitOccurrence_IdAndSelectedTemplate_Id(visit.getId(), template.getId())) {
            throw new DocumentationConflictException("An active documentation record already exists for this visit and template.");
        }
        OffsetDateTime now = command.startedAt() == null ? OffsetDateTime.now() : command.startedAt();
        VisitDocumentationRecord record = visitDocumentationRecordRepository.saveAndFlush(
                VisitDocumentationRecord.create(visit, template, actorMembership, now));
        List<DocumentationTaskResponse> taskResponses = new ArrayList<>();
        for (DocumentationTemplateTask templateTask : documentationTemplateTaskRepository.findAllByDocumentationTemplate_IdOrderBySortOrderAscIdAsc(template.getId())) {
            taskResponses.add(documentationTaskResponseRepository.saveAndFlush(DocumentationTaskResponse.create(record, templateTask)));
        }
        return new DocumentationAggregate(
                record,
                documentationFieldResponseRepository.findAllByDocumentationRecord_IdOrderByFieldKeyAsc(record.getId()),
                taskResponses,
                documentationAttachmentLinkRepository.findAllByDocumentationRecord_IdOrderByLinkedAtAsc(record.getId()));
    }

    @Transactional
    public DocumentationAggregate saveDraft(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRecordId,
            @Valid SaveDocumentationDraftCommand command) {
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        authorizeTemplateUse(actorMembership, record.getSelectedTemplate(), AgencyPermission.DRAFT_VISIT_DOCUMENTATION);
        OffsetDateTime now = command.savedAt() == null ? OffsetDateTime.now() : command.savedAt();

        for (FieldResponseCommand fieldCommand : command.fieldResponses()) {
            DocumentationTemplateField field = documentationTemplateFieldRepository.findByIdAndAgency_Id(fieldCommand.templateFieldId(), actorMembership.getAgencyId())
                    .orElseThrow(() -> new DocumentationEntityNotFoundException("DocumentationTemplateField", fieldCommand.templateFieldId()));
            if (!field.isVisibleTo(actorMembership.getRole()) || !field.isEditableBy(actorMembership.getRole())) {
                throw new UnauthorizedDocumentationActorException("Actor cannot edit field " + field.getFieldKey());
            }
            DocumentationFieldResponse response = documentationFieldResponseRepository
                    .findByDocumentationRecord_IdAndTemplateField_Id(record.getId(), field.getId())
                    .orElseGet(() -> DocumentationFieldResponse.create(
                            record,
                            field,
                            fieldCommand.normalizedValue(),
                            fieldCommand.displayValue(),
                            fieldCommand.responseNotes(),
                            coalesceState(fieldCommand.completionState()),
                            completedAt(fieldCommand.completionState(), fieldCommand.completedAt(), now)));
            response.update(
                    fieldCommand.normalizedValue(),
                    fieldCommand.displayValue(),
                    fieldCommand.responseNotes(),
                    coalesceState(fieldCommand.completionState()),
                    completedAt(fieldCommand.completionState(), fieldCommand.completedAt(), now));
            documentationFieldResponseRepository.save(response);
        }

        for (TaskResponseCommand taskCommand : command.taskResponses()) {
            DocumentationTemplateTask templateTask = documentationTemplateTaskRepository.findByIdAndAgency_Id(taskCommand.templateTaskId(), actorMembership.getAgencyId())
                    .orElseThrow(() -> new DocumentationEntityNotFoundException("DocumentationTemplateTask", taskCommand.templateTaskId()));
            DocumentationTaskResponse response = documentationTaskResponseRepository
                    .findByDocumentationRecord_IdAndTemplateTask_Id(record.getId(), templateTask.getId())
                    .orElseGet(() -> DocumentationTaskResponse.create(record, templateTask));
            response.update(
                    coalesceState(taskCommand.completionState()),
                    taskCommand.completionNotes(),
                    completedAt(taskCommand.completionState(), taskCommand.completedAt(), now));
            documentationTaskResponseRepository.save(response);
        }

        record.markDraftSaved(actorMembership, now);
        VisitDocumentationRecord savedRecord = visitDocumentationRecordRepository.saveAndFlush(record);
        documentationAuditService.recordDocumentationDraftSaved(
                actorMembership,
                savedRecord.getId(),
                savedRecord.getBranchId(),
                "{\"status\":\"" + savedRecord.getStatus().name() + "\"}");
        return aggregate(savedRecord);
    }

    @Transactional
    public DocumentationAggregate submitDocumentation(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRecordId,
            OffsetDateTime submittedAt) {
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        authorizeTemplateUse(actorMembership, record.getSelectedTemplate(), AgencyPermission.SUBMIT_VISIT_DOCUMENTATION);
        List<DocumentationFieldResponse> fieldResponses = documentationFieldResponseRepository.findAllByDocumentationRecord_IdOrderByFieldKeyAsc(record.getId());
        List<DocumentationTaskResponse> taskResponses = documentationTaskResponseRepository.findAllByDocumentationRecord_IdOrderBySortOrderAscTaskTitleAsc(record.getId());
        validateSubmission(record, actorMembership.getRole(), fieldResponses, taskResponses);
        OffsetDateTime now = submittedAt == null ? OffsetDateTime.now() : submittedAt;
        record.submit(actorMembership, now);
        VisitDocumentationRecord savedRecord = visitDocumentationRecordRepository.saveAndFlush(record);
        documentationAuditService.recordDocumentationSubmitted(
                actorMembership,
                savedRecord.getId(),
                savedRecord.getBranchId(),
                "{\"status\":\"" + savedRecord.getStatus().name() + "\"}");
        return new DocumentationAggregate(
                savedRecord,
                fieldResponses,
                taskResponses,
                documentationAttachmentLinkRepository.findAllByDocumentationRecord_IdOrderByLinkedAtAsc(record.getId()));
    }

    @Transactional
    public DocumentationAggregate amendDocumentation(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRecordId,
            @Valid SaveDocumentationDraftCommand command) {
        requirePermission(actorMembership, AgencyPermission.AMEND_VISIT_DOCUMENTATION);
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        authorizeTemplateUse(actorMembership, record.getSelectedTemplate(), AgencyPermission.AMEND_VISIT_DOCUMENTATION);
        record.amend(actorMembership, command.savedAt() == null ? OffsetDateTime.now() : command.savedAt());
        visitDocumentationRecordRepository.saveAndFlush(record);
        DocumentationAggregate aggregate = saveDraft(actorMembership, documentationRecordId, command);
        documentationAuditService.recordDocumentationAmended(
                actorMembership,
                documentationRecordId,
                aggregate.record().getBranchId(),
                "{\"status\":\"" + aggregate.record().getStatus().name() + "\"}");
        return aggregate;
    }

    @Transactional
    public DocumentationAttachmentLink linkPatientAttachment(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRecordId,
            @NotNull UUID patientAttachmentId,
            String caption,
            String description) {
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        authorizeTemplateUse(actorMembership, record.getSelectedTemplate(), AgencyPermission.DRAFT_VISIT_DOCUMENTATION);
        PatientAttachment attachment = patientAttachmentRepository.findById(patientAttachmentId)
                .filter(candidate -> Objects.equals(candidate.getAgencyId(), actorMembership.getAgencyId()))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("PatientAttachment", patientAttachmentId));
        DocumentationAttachmentLink link = documentationAttachmentLinkRepository.saveAndFlush(
                DocumentationAttachmentLink.linkPatientAttachment(record, attachment, actorMembership, caption, description, OffsetDateTime.now()));
        documentationAuditService.recordAttachmentLinked(actorMembership, link.getId(), record.getBranchId(), "{\"source\":\"PATIENT_ATTACHMENT\"}");
        return link;
    }

    @Transactional
    public DocumentationAttachmentLink linkMobileArtifact(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID documentationRecordId,
            @NotNull UUID mobileArtifactId,
            String caption,
            String description) {
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        authorizeTemplateUse(actorMembership, record.getSelectedTemplate(), AgencyPermission.DRAFT_VISIT_DOCUMENTATION);
        MobileFieldArtifact artifact = mobileFieldArtifactRepository.findById(mobileArtifactId)
                .filter(candidate -> Objects.equals(candidate.getAgencyId(), actorMembership.getAgencyId()))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("MobileFieldArtifact", mobileArtifactId));
        DocumentationAttachmentLink link = documentationAttachmentLinkRepository.saveAndFlush(
                DocumentationAttachmentLink.linkMobileArtifact(record, artifact, actorMembership, caption, description, OffsetDateTime.now()));
        documentationAuditService.recordAttachmentLinked(actorMembership, link.getId(), record.getBranchId(), "{\"source\":\"MOBILE_ARTIFACT\"}");
        return link;
    }

    @Transactional
    public PrintableSummaryProjection generatePrintableSummary(@NotNull AgencyMembership actorMembership, @NotNull UUID documentationRecordId) {
        requirePermission(actorMembership, AgencyPermission.GENERATE_PRINTABLE_DOCUMENTATION_SUMMARY);
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.VIEW_VISIT_DOCUMENTATION,
                deniedMembershipId -> new UnauthorizedDocumentationActorException("Actor cannot view visit documentation."));
        List<DocumentationFieldResponse> fieldResponses = documentationFieldResponseRepository.findAllByDocumentationRecord_IdOrderByFieldKeyAsc(record.getId());
        List<DocumentationTaskResponse> taskResponses = documentationTaskResponseRepository.findAllByDocumentationRecord_IdOrderBySortOrderAscTaskTitleAsc(record.getId());
        List<DocumentationAttachmentLink> attachmentLinks = documentationAttachmentLinkRepository.findAllByDocumentationRecord_IdOrderByLinkedAtAsc(record.getId());
        record.incrementPrintableSummaryVersion();
        visitDocumentationRecordRepository.saveAndFlush(record);
        documentationAuditService.recordPrintableSummaryGenerated(
                actorMembership,
                record.getId(),
                record.getBranchId(),
                "{\"version\":" + record.getPrintableSummaryVersion() + "}");
        return new PrintableSummaryProjection(
                record.getId(),
                new PrintableHeader(
                        patientDisplayName(record.getPatient()),
                        record.getVisitOccurrence().getId(),
                        record.getBranch() == null ? null : record.getBranch().getName(),
                        record.getVisitOccurrence().getPlannedStartAt(),
                        record.getVisitOccurrence().getPlannedEndAt()),
                record.getSelectedTemplate().getName(),
                fieldResponses.stream()
                        .filter(response -> response.getCompletionState() != DocumentationResponseState.PENDING || response.getDisplayValue() != null)
                        .map(response -> new PrintableField(response.getFieldKey(), response.getDisplayValue(), response.getResponseNotes()))
                        .toList(),
                taskResponses.stream()
                        .map(response -> new PrintableTask(response.getTaskTitle(), response.getCompletionState().name(), response.getCompletionNotes()))
                        .toList(),
                attachmentLinks.stream()
                        .map(link -> new PrintableAttachment(
                                link.getPatientAttachment() != null
                                        ? link.getPatientAttachment().getFileName()
                                        : link.getMobileArtifact().getFileName(),
                                link.getPatientAttachment() != null
                                        ? link.getPatientAttachment().getContentType()
                                        : link.getMobileArtifact().getContentType(),
                                link.getCaption(),
                                link.getDescription()))
                        .toList(),
                record.getAuthorMembership().getUser().getEmail(),
                record.getSubmittedAt(),
                record.getPrintableSummaryVersion());
    }

    @Transactional(readOnly = true)
    public DocumentationAggregate getDocumentationAggregate(@NotNull AgencyMembership actorMembership, @NotNull UUID documentationRecordId) {
        VisitDocumentationRecord record = resolveDocumentationRecord(actorMembership.getAgencyId(), documentationRecordId);
        authorizeTemplateUse(actorMembership, record.getSelectedTemplate(), AgencyPermission.VIEW_VISIT_DOCUMENTATION);
        return aggregate(record);
    }

    private TemplateAggregate persistTemplateStructure(DocumentationTemplate template, ManageTemplateCommand command) {
        documentationTemplateSectionRepository.deleteAllByDocumentationTemplate_Id(template.getId());
        documentationTemplateFieldRepository.deleteAllByDocumentationTemplate_Id(template.getId());
        documentationTemplateTaskRepository.deleteAllByDocumentationTemplate_Id(template.getId());

        Map<String, DocumentationTemplateSection> sectionsByKey = new LinkedHashMap<>();
        List<DocumentationTemplateSection> sections = new ArrayList<>();
        for (SectionCommand sectionCommand : command.sections()) {
            DocumentationTemplateSection section = documentationTemplateSectionRepository.saveAndFlush(
                    DocumentationTemplateSection.create(
                            template,
                            sectionCommand.sectionKey(),
                            sectionCommand.title(),
                            sectionCommand.helpText(),
                            sectionCommand.sortOrder()));
            sectionsByKey.put(section.getSectionKey(), section);
            sections.add(section);
        }

        List<DocumentationTemplateField> fields = new ArrayList<>();
        for (FieldDefinitionCommand fieldCommand : command.fields()) {
            fields.add(documentationTemplateFieldRepository.saveAndFlush(
                    DocumentationTemplateField.create(
                            template,
                            fieldCommand.sectionKey() == null ? null : sectionsByKey.get(fieldCommand.sectionKey()),
                            fieldCommand.fieldKey(),
                            fieldCommand.label(),
                            fieldCommand.fieldType(),
                            fieldCommand.requiredField(),
                            fieldCommand.sortOrder(),
                            fieldCommand.optionsJson(),
                            fieldCommand.helpText(),
                            fieldCommand.visibleActorRoles(),
                            fieldCommand.editableActorRoles())));
        }

        List<DocumentationTemplateTask> tasks = new ArrayList<>();
        for (TemplateTaskCommand taskCommand : command.tasks()) {
            TaskTemplate taskTemplate = taskCommand.taskTemplateId() == null ? null : resolveTaskTemplate(template.getAgencyId(), taskCommand.taskTemplateId());
            tasks.add(documentationTemplateTaskRepository.saveAndFlush(
                    DocumentationTemplateTask.create(
                            template,
                            taskCommand.sectionKey() == null ? null : sectionsByKey.get(taskCommand.sectionKey()),
                            taskTemplate,
                            taskCommand.titleOverride(),
                            taskCommand.descriptionOverride(),
                            taskCommand.requiredOverride(),
                            taskCommand.sortOrder())));
        }
        return new TemplateAggregate(template, sections, fields, tasks);
    }

    private DocumentationAggregate aggregate(VisitDocumentationRecord record) {
        return new DocumentationAggregate(
                record,
                documentationFieldResponseRepository.findAllByDocumentationRecord_IdOrderByFieldKeyAsc(record.getId()),
                documentationTaskResponseRepository.findAllByDocumentationRecord_IdOrderBySortOrderAscTaskTitleAsc(record.getId()),
                documentationAttachmentLinkRepository.findAllByDocumentationRecord_IdOrderByLinkedAtAsc(record.getId()));
    }

    private void validateSubmission(
            VisitDocumentationRecord record,
            AgencyRole actorRole,
            List<DocumentationFieldResponse> fieldResponses,
            List<DocumentationTaskResponse> taskResponses) {
        Map<UUID, DocumentationFieldResponse> responsesByFieldId = new LinkedHashMap<>();
        fieldResponses.forEach(response -> responsesByFieldId.put(response.getTemplateFieldId(), response));
        List<DocumentationValidationException.FieldValidationError> fieldErrors = new ArrayList<>();
        for (DocumentationTemplateField field : documentationTemplateFieldRepository.findAllByDocumentationTemplate_IdOrderBySortOrderAscLabelAsc(record.getSelectedTemplateId())) {
            if (!field.isVisibleTo(actorRole)) {
                continue;
            }
            DocumentationFieldResponse response = responsesByFieldId.get(field.getId());
            boolean missing = response == null
                    || (response.getNormalizedValue() == null && response.getDisplayValue() == null)
                    || response.getCompletionState() == DocumentationResponseState.PENDING;
            if (field.isRequiredField() && missing) {
                fieldErrors.add(new DocumentationValidationException.FieldValidationError(field.getFieldKey(), "Required field is incomplete."));
            }
        }

        Map<UUID, DocumentationTaskResponse> responsesByTaskId = new LinkedHashMap<>();
        taskResponses.forEach(response -> responsesByTaskId.put(response.getTemplateTaskId(), response));
        List<DocumentationValidationException.TaskValidationError> taskErrors = new ArrayList<>();
        for (DocumentationTemplateTask task : documentationTemplateTaskRepository.findAllByDocumentationTemplate_IdOrderBySortOrderAscIdAsc(record.getSelectedTemplateId())) {
            DocumentationTaskResponse response = responsesByTaskId.get(task.getId());
            if (task.effectiveRequired() && (response == null || response.getCompletionState() != DocumentationResponseState.COMPLETED)) {
                taskErrors.add(new DocumentationValidationException.TaskValidationError(task.effectiveTitle(), "Required task is incomplete."));
            }
        }

        if (record.getSelectedTemplate().isRequiresSignatureVerification()) {
            boolean signaturePresent = evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(record.getVisitOccurrenceId())
                    .map(session -> signatureVerificationLinkRepository.findAllByVerificationSession_IdOrderByRecordedAtAsc(session.getId()))
                    .stream()
                    .flatMap(List::stream)
                    .anyMatch(link -> link.getVerificationStatus() == SignatureVerificationStatus.PRESENT);
            if (!signaturePresent) {
                fieldErrors.add(new DocumentationValidationException.FieldValidationError("signature", "Configured signature verification is missing."));
            }
        }

        if (!fieldErrors.isEmpty() || !taskErrors.isEmpty()) {
            throw new DocumentationValidationException("Documentation submission failed validation.", fieldErrors, taskErrors);
        }
    }

    private void authorizeTemplateUse(AgencyMembership actorMembership, DocumentationTemplate template, AgencyPermission permission) {
        requirePermission(actorMembership, permission);
        Set<AgencyRole> allowedRoles = template.allowedActorRoleSet();
        if (!allowedRoles.isEmpty() && !allowedRoles.contains(actorMembership.getRole())) {
            throw new UnauthorizedDocumentationActorException("Actor role is not allowed to use the selected template.");
        }
    }

    private void ensureTemplateMatchesVisit(DocumentationTemplate template, VisitOccurrence visit) {
        if (template.getVisitTypeId() != null && !Objects.equals(template.getVisitTypeId(), visit.getVisitType() == null ? null : visit.getVisitType().getId())) {
            throw new DocumentationConflictException("Template visit type does not match the visit.");
        }
        if (template.getServiceLineId() != null && !Objects.equals(template.getServiceLineId(), visit.getServiceLine() == null ? null : visit.getServiceLine().getId())) {
            throw new DocumentationConflictException("Template service line does not match the visit.");
        }
        if (template.getBranchId() != null && !Objects.equals(template.getBranchId(), visit.getBranchId())) {
            throw new DocumentationConflictException("Template branch does not match the visit.");
        }
        if (template.getStatus() != ConfigurationStatus.ACTIVE && template.getStatus() != ConfigurationStatus.DRAFT) {
            throw new DocumentationConflictException("Selected template is not available for documentation.");
        }
    }

    private DocumentationTemplate resolveTemplate(UUID agencyId, UUID templateId) {
        return documentationTemplateRepository.findById(templateId)
                .filter(template -> Objects.equals(template.getAgencyId(), agencyId))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("DocumentationTemplate", templateId));
    }

    private VisitOccurrence resolveVisit(UUID agencyId, UUID visitId) {
        return visitOccurrenceRepository.findById(visitId)
                .filter(visit -> Objects.equals(visit.getAgencyId(), agencyId))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("VisitOccurrence", visitId));
    }

    private VisitDocumentationRecord resolveDocumentationRecord(UUID agencyId, UUID recordId) {
        return visitDocumentationRecordRepository.findByIdAndAgency_Id(recordId, agencyId)
                .orElseThrow(() -> new DocumentationEntityNotFoundException("VisitDocumentationRecord", recordId));
    }

    private TaskTemplate resolveTaskTemplate(UUID agencyId, UUID taskTemplateId) {
        return taskTemplateRepository.findById(taskTemplateId)
                .filter(taskTemplate -> Objects.equals(taskTemplate.getAgencyId(), agencyId))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("TaskTemplate", taskTemplateId));
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> Objects.equals(serviceLine.getAgencyId(), agencyId))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("ServiceLine", serviceLineId));
    }

    private VisitType resolveVisitType(UUID agencyId, UUID visitTypeId) {
        if (visitTypeId == null) {
            return null;
        }
        return visitTypeRepository.findById(visitTypeId)
                .filter(visitType -> Objects.equals(visitType.getAgencyId(), agencyId))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("VisitType", visitTypeId));
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .filter(branch -> Objects.equals(branch.getAgencyId(), agencyId))
                .orElseThrow(() -> new DocumentationEntityNotFoundException("Branch", branchId));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                permission,
                deniedMembershipId -> new UnauthorizedDocumentationActorException("Missing documentation permission: " + permission.name()));
    }

    private void assertTemplateUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? documentationTemplateRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : documentationTemplateRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DocumentationConflictException("Documentation template name already exists.");
        }
        String normalizedCode = code.trim().toUpperCase();
        boolean duplicateCode = existingId == null
                ? documentationTemplateRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : documentationTemplateRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DocumentationConflictException("Documentation template code already exists.");
        }
    }

    private void assertTaskUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? taskTemplateRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : taskTemplateRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DocumentationConflictException("Task library item name already exists.");
        }
        String normalizedCode = code.trim().toUpperCase();
        boolean duplicateCode = existingId == null
                ? taskTemplateRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : taskTemplateRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DocumentationConflictException("Task library item code already exists.");
        }
    }

    private static DocumentationResponseState coalesceState(DocumentationResponseState completionState) {
        return completionState == null ? DocumentationResponseState.PENDING : completionState;
    }

    private static OffsetDateTime completedAt(DocumentationResponseState state, OffsetDateTime completedAt, OffsetDateTime fallbackNow) {
        DocumentationResponseState effectiveState = coalesceState(state);
        return effectiveState == DocumentationResponseState.COMPLETED ? (completedAt == null ? fallbackNow : completedAt) : null;
    }

    private static String patientDisplayName(com.homehealthcare.patient.domain.Patient patient) {
        String preferred = patient.getPreferredName();
        String first = preferred != null && !preferred.isBlank() ? preferred : patient.getFirstName();
        return first + " " + patient.getLastName();
    }

    public record ManageTemplateCommand(
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
            @NotEmpty List<@Valid SectionCommand> sections,
            @NotEmpty List<@Valid FieldDefinitionCommand> fields,
            List<@Valid TemplateTaskCommand> tasks) {
    }

    public record SectionCommand(
            @NotBlank String sectionKey,
            @NotBlank String title,
            String helpText,
            int sortOrder) {
    }

    public record FieldDefinitionCommand(
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

    public record TemplateTaskCommand(
            String sectionKey,
            UUID taskTemplateId,
            String titleOverride,
            String descriptionOverride,
            Boolean requiredOverride,
            int sortOrder) {
    }

    public record ManageTaskLibraryItemCommand(
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

    public record CreateDocumentationRecordCommand(
            @NotNull UUID visitOccurrenceId,
            @NotNull UUID selectedTemplateId,
            OffsetDateTime startedAt) {
    }

    public record SaveDocumentationDraftCommand(
            List<@Valid FieldResponseCommand> fieldResponses,
            List<@Valid TaskResponseCommand> taskResponses,
            OffsetDateTime savedAt) {
        public SaveDocumentationDraftCommand {
            fieldResponses = fieldResponses == null ? List.of() : List.copyOf(fieldResponses);
            taskResponses = taskResponses == null ? List.of() : List.copyOf(taskResponses);
        }
    }

    public record FieldResponseCommand(
            @NotNull UUID templateFieldId,
            String normalizedValue,
            String displayValue,
            String responseNotes,
            DocumentationResponseState completionState,
            OffsetDateTime completedAt) {
    }

    public record TaskResponseCommand(
            @NotNull UUID templateTaskId,
            DocumentationResponseState completionState,
            String completionNotes,
            OffsetDateTime completedAt) {
    }

    public record TemplateAggregate(
            DocumentationTemplate template,
            List<DocumentationTemplateSection> sections,
            List<DocumentationTemplateField> fields,
            List<DocumentationTemplateTask> tasks) {
    }

    public record DocumentationAggregate(
            VisitDocumentationRecord record,
            List<DocumentationFieldResponse> fieldResponses,
            List<DocumentationTaskResponse> taskResponses,
            List<DocumentationAttachmentLink> attachmentLinks) {
    }

    public record PrintableSummaryProjection(
            UUID documentationRecordId,
            PrintableHeader header,
            String templateTitle,
            List<PrintableField> fields,
            List<PrintableTask> tasks,
            List<PrintableAttachment> attachments,
            String authorEmail,
            OffsetDateTime submittedAt,
            int version) {
    }

    public record PrintableHeader(
            String patientDisplayName,
            UUID visitOccurrenceId,
            String branchName,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt) {
    }

    public record PrintableField(String fieldKey, String displayValue, String notes) {
    }

    public record PrintableTask(String taskTitle, String completionState, String completionNotes) {
    }

    public record PrintableAttachment(String fileName, String contentType, String caption, String description) {
    }
}
