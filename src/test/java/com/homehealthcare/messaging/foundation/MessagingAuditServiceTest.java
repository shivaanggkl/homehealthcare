package com.homehealthcare.messaging.foundation;

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
class MessagingAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private MessagingAuditService messagingAuditService;

    @Test
    void threadAuditUsesStandardizedEpic9ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.BRANCH_ADMIN);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messagingAuditService.recordThreadCreated(
                actorMembership,
                targetId,
                branchId,
                "{\"threadType\":\"PATIENT_COORDINATION\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic9MessagingAuditAction.THREAD_CREATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic9MessagingTargetType.COMMUNICATION_THREAD.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void messagingFoundationContractsCoverExpectedThreadDeliveryAndEscalationVocabulary() {
        assertThat(EnumSet.allOf(MessagingThreadType.class))
                .containsExactlyInAnyOrder(
                        MessagingThreadType.DIRECT_SECURE,
                        MessagingThreadType.PATIENT_COORDINATION,
                        MessagingThreadType.VISIT_COORDINATION,
                        MessagingThreadType.TASK_DISCUSSION,
                        MessagingThreadType.BRANCH_BROADCAST);

        assertThat(EnumSet.allOf(MessagingDeliveryState.class))
                .containsExactlyInAnyOrder(
                        MessagingDeliveryState.PENDING,
                        MessagingDeliveryState.DELIVERED,
                        MessagingDeliveryState.READ);

        assertThat(EnumSet.allOf(MessagingEscalationStatus.class))
                .containsExactlyInAnyOrder(
                        MessagingEscalationStatus.NORMAL,
                        MessagingEscalationStatus.URGENT,
                        MessagingEscalationStatus.ESCALATED,
                        MessagingEscalationStatus.RESOLVED);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Messaging", "Admin", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
