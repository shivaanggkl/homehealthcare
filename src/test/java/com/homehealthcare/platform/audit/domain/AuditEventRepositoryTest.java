package com.homehealthcare.platform.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuditEventRepositoryTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsUnifiedAuditEventShapeIncludingBranchOutcomeAndMetadata() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "North Branch", "NB1", "101 Main St", "America/Chicago"));
        User actor = userRepository.saveAndFlush(
                User.invite("Alicia", "Admin", "alicia.audit." + UUID.randomUUID() + "@example.com", null));

        AuditEvent successEvent = auditEventRepository.saveAndFlush(AuditEvent.createSuccess(
                "USER",
                actor.getId(),
                actor.getEmail(),
                "BRANCH_VIEWED",
                "BRANCH",
                branch.getId(),
                agency.getId(),
                branch.getId(),
                "{\"source\":\"api\"}"));

        AuditEvent failureEvent = auditEventRepository.saveAndFlush(AuditEvent.createFailure(
                "USER",
                actor.getId(),
                actor.getEmail(),
                "BRANCH_EDIT_DENIED",
                "BRANCH",
                branch.getId(),
                agency.getId(),
                branch.getId(),
                "{\"reason\":\"BRANCH_SCOPE_DENIED\"}"));

        assertThat(successEvent.getOccurredAt()).isNotNull();
        assertThat(failureEvent.getOccurredAt()).isNotNull();

        List<AuditEvent> failureEvents = auditEventRepository.findAllByOutcomeOrderByOccurredAtAsc(AuditEventOutcome.FAILURE);
        assertThat(failureEvents)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getActorId()).isEqualTo(actor.getId());
                    assertThat(event.getTargetType()).isEqualTo("BRANCH");
                    assertThat(event.getTargetId()).isEqualTo(branch.getId());
                    assertThat(event.getAgencyId()).isEqualTo(agency.getId());
                    assertThat(event.getBranchId()).isEqualTo(branch.getId());
                    assertThat(event.getMetadataJson()).contains("BRANCH_SCOPE_DENIED");
                });
    }
}
