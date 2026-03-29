package com.homehealthcare.documentation.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentationAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordTemplateCreated(
            AgencyMembership actorMembership,
            Epic8DocumentationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic8DocumentationAuditAction.TEMPLATE_CREATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordTemplateUpdated(
            AgencyMembership actorMembership,
            Epic8DocumentationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic8DocumentationAuditAction.TEMPLATE_UPDATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordTaskLibraryUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic8DocumentationAuditAction.TASK_LIBRARY_UPDATED,
                Epic8DocumentationTargetType.TASK_LIBRARY_ITEM,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordDocumentationDraftSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED,
                Epic8DocumentationTargetType.VISIT_DOCUMENTATION_RECORD,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordDocumentationSubmitted(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic8DocumentationAuditAction.DOCUMENTATION_SUBMITTED,
                Epic8DocumentationTargetType.VISIT_DOCUMENTATION_RECORD,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordDocumentationAmended(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic8DocumentationAuditAction.DOCUMENTATION_AMENDED,
                Epic8DocumentationTargetType.VISIT_DOCUMENTATION_RECORD,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordAttachmentLinked(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic8DocumentationAuditAction.ATTACHMENT_LINKED,
                Epic8DocumentationTargetType.DOCUMENTATION_ATTACHMENT_LINK,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordPrintableSummaryGenerated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic8DocumentationAuditAction.PRINTABLE_SUMMARY_GENERATED,
                Epic8DocumentationTargetType.PRINTABLE_SUMMARY_PROJECTION,
                targetId,
                branchId,
                metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic8DocumentationAuditAction action,
            Epic8DocumentationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        Objects.requireNonNull(actorMembership, "actorMembership must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");

        auditEventRepository.save(AuditEvent.createSuccess(
                ACTOR_TYPE,
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                action.actionType(),
                targetType.name(),
                targetId,
                actorMembership.getAgencyId(),
                branchId,
                metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson));
    }
}
