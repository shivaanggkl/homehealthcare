package com.homehealthcare.workforce.foundation;

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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkforceAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private WorkforceAuditService workforceAuditService;

    @Test
    void workforceCreateAuditUsesStandardizedEpic4ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.AGENCY_OWNER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workforceAuditService.recordCreated(
                actorMembership,
                Epic4WorkforceTargetType.CAREGIVER_PROFILE,
                targetId,
                branchId,
                "{\"status\":\"ACTIVE\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic4WorkforceAuditAction.CREATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic4WorkforceTargetType.CAREGIVER_PROFILE.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getAgencyId()).isEqualTo(actorMembership.getAgencyId());
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void workforcePerformanceRefreshDefaultsBlankMetadataToEmptyJson() {
        AgencyMembership actorMembership = membership(AgencyRole.BRANCH_ADMIN);
        UUID targetId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workforceAuditService.recordPerformanceRefreshed(actorMembership, targetId, null, " ");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic4WorkforceAuditAction.PERFORMANCE_REFRESHED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic4WorkforceTargetType.CAREGIVER_PERFORMANCE_SUMMARY.name());
        assertThat(event.getMetadataJson()).isEqualTo("{}");
        assertThat(event.getBranchId()).isNull();
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Workforce", "Admin", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
