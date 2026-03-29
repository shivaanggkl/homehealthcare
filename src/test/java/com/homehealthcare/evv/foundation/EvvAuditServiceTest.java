package com.homehealthcare.evv.foundation;

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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvvAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private EvvAuditService evvAuditService;

    @Test
    void clockInAuditUsesStandardizedEpic7ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.CAREGIVER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evvAuditService.recordClockInRecorded(actorMembership, targetId, branchId, "{\"verificationStatus\":\"PENDING_VERIFICATION\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic7EvvTargetType.EVV_CLOCK_EVENT.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void evvFoundationContractsEnforceValidationAndDefaults() {
        assertThatThrownBy(() -> new GeofenceEvaluationResult(
                GeofenceEvaluationOutcome.WITHIN_TOLERANCE,
                -1,
                25,
                false,
                "WITHIN_TOLERANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distanceFromExpectedMeters");

        assertThatThrownBy(() -> new EvvComplianceProjection(
                true,
                true,
                GeofenceEvaluationOutcome.WITHIN_TOLERANCE,
                true,
                -1,
                false,
                EvvComplianceOutcome.READY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openExceptionCount");

        assertThat(GeofenceEvaluationResult.notEvaluable(" ").reasonCode()).isEqualTo("UNSPECIFIED");
        assertThat(EvvComplianceProjection.missedVisit(1).overallOutcome()).isEqualTo(EvvComplianceOutcome.MISSED_VISIT);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Evv", "Caregiver", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
