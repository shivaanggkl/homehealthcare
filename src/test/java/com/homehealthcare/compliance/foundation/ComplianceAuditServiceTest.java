package com.homehealthcare.compliance.foundation;

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
class ComplianceAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private ComplianceAuditService complianceAuditService;

    @Test
    void complianceAuditUsesStandardizedEpic11ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.QA_CLINICAL_REVIEWER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        complianceAuditService.recordChecklistDefinitionSaved(actorMembership, targetId, branchId, "{\"status\":\"ACTIVE\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic11ComplianceAuditAction.CHECKLIST_DEFINITION_SAVED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic11ComplianceTargetType.CHECKLIST_DEFINITION.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void complianceFoundationContractsCoverExpectedLifecycleAndEventVocabulary() {
        assertThat(EnumSet.allOf(ComplianceChecklistResultStatus.class))
                .containsExactlyInAnyOrder(
                        ComplianceChecklistResultStatus.PASS,
                        ComplianceChecklistResultStatus.FAIL,
                        ComplianceChecklistResultStatus.WARNING,
                        ComplianceChecklistResultStatus.NOT_APPLICABLE);

        assertThat(EnumSet.allOf(ConsentAcknowledgmentStatus.class))
                .containsExactlyInAnyOrder(
                        ConsentAcknowledgmentStatus.ACTIVE,
                        ConsentAcknowledgmentStatus.EXPIRED,
                        ConsentAcknowledgmentStatus.REVOKED,
                        ConsentAcknowledgmentStatus.MISSING);

        assertThat(EnumSet.allOf(PatientRiskReminderStatus.class))
                .containsExactlyInAnyOrder(
                        PatientRiskReminderStatus.ACTIVE,
                        PatientRiskReminderStatus.RESOLVED,
                        PatientRiskReminderStatus.EXPIRED);

        assertThat(EnumSet.allOf(CertificationPeriodStatus.class))
                .containsExactlyInAnyOrder(
                        CertificationPeriodStatus.CURRENT,
                        CertificationPeriodStatus.UPCOMING_EXPIRY,
                        CertificationPeriodStatus.EXPIRED,
                        CertificationPeriodStatus.MISSING);

        assertThat(EnumSet.allOf(ComplianceReadinessStatus.class))
                .containsExactlyInAnyOrder(
                        ComplianceReadinessStatus.READY,
                        ComplianceReadinessStatus.WARNING,
                        ComplianceReadinessStatus.NON_COMPLIANT,
                        ComplianceReadinessStatus.UNKNOWN);

        assertThat(EnumSet.allOf(ComplianceEventType.class))
                .containsExactlyInAnyOrder(
                        ComplianceEventType.PATIENT_RISK_REMINDER_TRIGGERED,
                        ComplianceEventType.PATIENT_RISK_REMINDER_RESOLVED,
                        ComplianceEventType.ACKNOWLEDGMENT_MISSING,
                        ComplianceEventType.ACKNOWLEDGMENT_EXPIRED,
                        ComplianceEventType.CERTIFICATION_PERIOD_EXPIRING,
                        ComplianceEventType.REQUIRED_DOCUMENTATION_GAP_DETECTED,
                        ComplianceEventType.COMPLIANCE_STATUS_CHANGED);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Compliance", "Reviewer", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
