package com.homehealthcare.review.foundation;

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
class ReviewAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private ReviewAuditService reviewAuditService;

    @Test
    void reviewAuditUsesStandardizedEpic10ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.QA_CLINICAL_REVIEWER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewAuditService.recordReviewItemCreated(actorMembership, targetId, branchId, "{\"status\":\"PENDING_REVIEW\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic10ReviewTargetType.REVIEW_WORK_ITEM.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void reviewFoundationContractsCoverExpectedLifecycleAndDecisionVocabulary() {
        assertThat(EnumSet.allOf(ReviewLifecycleStatus.class))
                .containsExactlyInAnyOrder(
                        ReviewLifecycleStatus.PENDING_REVIEW,
                        ReviewLifecycleStatus.ASSIGNED,
                        ReviewLifecycleStatus.IN_REVIEW,
                        ReviewLifecycleStatus.RETURNED_FOR_FIX,
                        ReviewLifecycleStatus.RESUBMITTED,
                        ReviewLifecycleStatus.APPROVED,
                        ReviewLifecycleStatus.REJECTED,
                        ReviewLifecycleStatus.SIGNOFF_REQUESTED,
                        ReviewLifecycleStatus.SIGNOFF_COMPLETED);

        assertThat(EnumSet.allOf(ReviewDecisionType.class))
                .containsExactlyInAnyOrder(
                        ReviewDecisionType.APPROVE,
                        ReviewDecisionType.REJECT,
                        ReviewDecisionType.RETURN_FOR_FIX,
                        ReviewDecisionType.REQUEST_SIGNOFF);
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Review", "Admin", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
