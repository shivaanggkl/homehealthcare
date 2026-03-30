package com.homehealthcare.revenuereadiness.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class RevenueReadinessAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private RevenueReadinessAuditService revenueReadinessAuditService;

    @Test
    void revenueReadinessAuditUsesStandardizedEpic14ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.BILLING_BACK_OFFICE);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        revenueReadinessAuditService.recordReadinessRecalculated(actorMembership, targetId, branchId, "{\"status\":\"BLOCKED\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void exportAuditRejectsUnsupportedTargetTypes() {
        AgencyMembership actorMembership = membership(AgencyRole.BILLING_BACK_OFFICE);

        assertThatThrownBy(() -> revenueReadinessAuditService.recordExportGenerated(
                actorMembership,
                Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payroll or invoice");
    }

    @Test
    void revenueReadinessFoundationContractsCoverExpectedLifecycleAndProjectionVocabulary() {
        assertThat(EnumSet.allOf(RevenueReadinessEntityCategory.class))
                .containsExactlyInAnyOrder(
                        RevenueReadinessEntityCategory.REVENUE_READINESS_PROJECTION,
                        RevenueReadinessEntityCategory.READINESS_VALIDATION_RESULT,
                        RevenueReadinessEntityCategory.REVENUE_EXCEPTION_FLAG,
                        RevenueReadinessEntityCategory.PAYROLL_EXPORT_ROW,
                        RevenueReadinessEntityCategory.INVOICE_EXPORT_ROW,
                        RevenueReadinessEntityCategory.AUTHORIZATION_USAGE_SNAPSHOT,
                        RevenueReadinessEntityCategory.PAYER_SERVICE_SUMMARY_PROJECTION);

        assertThat(EnumSet.allOf(RevenueReadinessStatus.class))
                .containsExactlyInAnyOrder(
                        RevenueReadinessStatus.READY,
                        RevenueReadinessStatus.WARNING,
                        RevenueReadinessStatus.BLOCKED,
                        RevenueReadinessStatus.EXPORTED);

        assertThat(EnumSet.allOf(RevenueValidationOutcome.class))
                .containsExactlyInAnyOrder(
                        RevenueValidationOutcome.PASS,
                        RevenueValidationOutcome.WARNING,
                        RevenueValidationOutcome.FAIL);

        assertThat(EnumSet.allOf(RevenueExportLifecycleStatus.class))
                .containsExactlyInAnyOrder(
                        RevenueExportLifecycleStatus.NOT_REQUESTED,
                        RevenueExportLifecycleStatus.STAGED,
                        RevenueExportLifecycleStatus.GENERATED,
                        RevenueExportLifecycleStatus.HANDED_OFF,
                        RevenueExportLifecycleStatus.VOIDED);

        assertThat(EnumSet.allOf(RevenueUsagePosture.class))
                .containsExactlyInAnyOrder(
                        RevenueUsagePosture.WITHIN_LIMITS,
                        RevenueUsagePosture.NEAR_LIMIT,
                        RevenueUsagePosture.OVER_LIMIT,
                        RevenueUsagePosture.MISSING_AUTHORIZATION,
                        RevenueUsagePosture.NOT_APPLICABLE);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Revenue", "Ops", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
