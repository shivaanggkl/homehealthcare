package com.homehealthcare.documentation.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentationAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private DocumentationAuditService documentationAuditService;

    @Test
    void templateAuditUsesStandardizedEpic8ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.BRANCH_ADMIN);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentationAuditService.recordTemplateCreated(
                actorMembership,
                Epic8DocumentationTargetType.VISIT_NOTE_TEMPLATE,
                targetId,
                branchId,
                "{\"visitType\":\"SNV\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic8DocumentationAuditAction.TEMPLATE_CREATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic8DocumentationTargetType.VISIT_NOTE_TEMPLATE.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void documentationFoundationContractsCoverExpectedLifecycleAndFieldVocabulary() {
        assertThat(EnumSet.allOf(DocumentationRecordStatus.class))
                .containsExactlyInAnyOrder(
                        DocumentationRecordStatus.DRAFT,
                        DocumentationRecordStatus.IN_PROGRESS,
                        DocumentationRecordStatus.SUBMITTED,
                        DocumentationRecordStatus.AMENDED,
                        DocumentationRecordStatus.LOCKED);

        assertThat(EnumSet.allOf(DocumentationFieldType.class))
                .contains(
                        DocumentationFieldType.TEXT,
                        DocumentationFieldType.LONG_TEXT,
                        DocumentationFieldType.BOOLEAN,
                        DocumentationFieldType.NUMBER,
                        DocumentationFieldType.DATE_TIME,
                        DocumentationFieldType.SELECT_CODED_VALUE,
                        DocumentationFieldType.FREE_TEXT_BLOCK);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Documentation", "Admin", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
