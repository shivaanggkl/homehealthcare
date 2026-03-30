package com.homehealthcare.careprogression.foundation;

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
class CareProgressionAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private CareProgressionAuditService careProgressionAuditService;

    @Test
    void careProgressionAuditUsesStandardizedEpic13ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.QA_CLINICAL_REVIEWER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        careProgressionAuditService.recordGoalTemplateSaved(actorMembership, targetId, branchId, "{\"status\":\"ACTIVE\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic13CareProgressionAuditAction.GOAL_TEMPLATE_SAVED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic13CareProgressionTargetType.GOAL_TEMPLATE.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void careProgressionFoundationContractsCoverExpectedLifecycleAndEventVocabulary() {
        assertThat(EnumSet.allOf(GoalTemplateLifecycleStatus.class))
                .containsExactlyInAnyOrder(
                        GoalTemplateLifecycleStatus.DRAFT,
                        GoalTemplateLifecycleStatus.ACTIVE,
                        GoalTemplateLifecycleStatus.INACTIVE,
                        GoalTemplateLifecycleStatus.ARCHIVED);

        assertThat(EnumSet.allOf(PatientGoalLifecycleStatus.class))
                .containsExactlyInAnyOrder(
                        PatientGoalLifecycleStatus.ACTIVE,
                        PatientGoalLifecycleStatus.COMPLETED,
                        PatientGoalLifecycleStatus.UNMET,
                        PatientGoalLifecycleStatus.NOT_ATTAINED,
                        PatientGoalLifecycleStatus.CANCELLED);

        assertThat(EnumSet.allOf(GoalInterventionLifecycleStatus.class))
                .containsExactlyInAnyOrder(
                        GoalInterventionLifecycleStatus.ACTIVE,
                        GoalInterventionLifecycleStatus.COMPLETED,
                        GoalInterventionLifecycleStatus.INACTIVE,
                        GoalInterventionLifecycleStatus.CANCELLED);

        assertThat(EnumSet.allOf(GoalProgressNoteLifecycleStatus.class))
                .containsExactlyInAnyOrder(
                        GoalProgressNoteLifecycleStatus.DRAFT,
                        GoalProgressNoteLifecycleStatus.FINALIZED,
                        GoalProgressNoteLifecycleStatus.AMENDED);

        assertThat(EnumSet.allOf(CarePlanSyncStatus.class))
                .containsExactlyInAnyOrder(
                        CarePlanSyncStatus.ALIGNED,
                        CarePlanSyncStatus.UNSYNCED,
                        CarePlanSyncStatus.STALE,
                        CarePlanSyncStatus.FAILED);

        assertThat(EnumSet.allOf(GoalTargetDatePosture.class))
                .containsExactlyInAnyOrder(
                        GoalTargetDatePosture.ON_TRACK,
                        GoalTargetDatePosture.AT_RISK,
                        GoalTargetDatePosture.OVERDUE,
                        GoalTargetDatePosture.NOT_APPLICABLE);

        assertThat(EnumSet.allOf(CareProgressionEventType.class))
                .containsExactlyInAnyOrder(
                        CareProgressionEventType.GOAL_STATE_CHANGED,
                        CareProgressionEventType.TARGET_DATE_AT_RISK,
                        CareProgressionEventType.TARGET_DATE_OVERDUE,
                        CareProgressionEventType.CAREPLAN_SYNC_DRIFT_DETECTED);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Care", "Reviewer", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
