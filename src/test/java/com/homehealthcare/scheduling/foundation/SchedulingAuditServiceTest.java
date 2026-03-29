package com.homehealthcare.scheduling.foundation;

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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulingAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private SchedulingAuditService schedulingAuditService;

    @Test
    void visitCreateAuditUsesStandardizedEpic5ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.AGENCY_OWNER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schedulingAuditService.recordVisitCreated(
                actorMembership,
                targetId,
                branchId,
                "{\"status\":\"PLANNED\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic5SchedulingAuditAction.VISIT_CREATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic5SchedulingTargetType.VISIT_OCCURRENCE.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getAgencyId()).isEqualTo(actorMembership.getAgencyId());
        assertThat(event.getBranchId()).isEqualTo(branchId);
    }

    @Test
    void travelEvaluationDefaultsBlankMetadataToEmptyJson() {
        AgencyMembership actorMembership = membership(AgencyRole.SCHEDULER_COORDINATOR);
        UUID targetId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schedulingAuditService.recordTravelEvaluated(actorMembership, targetId, null, " ");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic5SchedulingAuditAction.TRAVEL_EVALUATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic5SchedulingTargetType.TRAVEL_AWARENESS_EVALUATION.name());
        assertThat(event.getMetadataJson()).isEqualTo("{}");
        assertThat(event.getBranchId()).isNull();
    }

    @Test
    void travelAwarenessContractRejectsNegativeDurations() {
        assertThatThrownBy(() -> new SchedulingTravelAwareness(-1, 10, TravelAwarenessLevel.FEASIBLE, "X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimatedTravelMinutes");

        assertThatThrownBy(() -> new SchedulingTravelAwareness(10, -1, TravelAwarenessLevel.FEASIBLE, "X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gapMinutes");
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Schedule", "Admin", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
