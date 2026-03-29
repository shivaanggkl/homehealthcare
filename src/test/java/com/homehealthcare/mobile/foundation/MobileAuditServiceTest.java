package com.homehealthcare.mobile.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private MobileAuditService mobileAuditService;

    @Test
    void mobileExecutionStartAuditUsesStandardizedEpic6ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.CAREGIVER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mobileAuditService.recordVisitExecutionStarted(actorMembership, targetId, branchId, "{\"status\":\"IN_PROGRESS\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic6MobileAuditAction.VISIT_EXECUTION_STARTED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic6MobileTargetType.VISIT_EXECUTION_SESSION.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void offlineSyncRejectedDefaultsBlankMetadataToEmptyJson() {
        AgencyMembership actorMembership = membership(AgencyRole.CAREGIVER);
        UUID targetId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mobileAuditService.recordOfflineSyncRejected(actorMembership, targetId, null, " ");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic6MobileAuditAction.OFFLINE_SYNC_REJECTED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic6MobileTargetType.OFFLINE_SYNC_ENVELOPE.name());
        assertThat(event.getMetadataJson()).isEqualTo("{}");
        assertThat(event.getBranchId()).isNull();
    }

    @Test
    void mobileFoundationContractsRejectBlankSessionAndSyncKeys() {
        assertThatThrownBy(() -> new MobileSessionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                " ",
                Set.of(AgencyPermission.VIEW_OWN_MOBILE_VISITS),
                true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionId");

        assertThatThrownBy(() -> new MobileSyncEnvelope(
                UUID.randomUUID(),
                "",
                "sync-key",
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operationType");

        assertThat(new MobileSyncResult(MobileSyncDisposition.ACCEPTED, UUID.randomUUID(), " "))
                .extracting(MobileSyncResult::message)
                .isEqualTo(MobileSyncDisposition.ACCEPTED.name());
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Mobile", "Caregiver", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
