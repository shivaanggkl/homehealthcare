package com.homehealthcare.patientevent.foundation;

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
class PatientEventAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private PatientEventAuditService patientEventAuditService;

    @Test
    void patientEventAuditUsesStandardizedEpic12ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.QA_CLINICAL_REVIEWER);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patientEventAuditService.recordIncidentCreated(actorMembership, targetId, branchId, "{\"severity\":\"HIGH\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic12PatientEventTargetType.INCIDENT_RECORD.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void patientEventFoundationContractsCoverExpectedLifecycleAndAlertVocabulary() {
        assertThat(EnumSet.allOf(IncidentRecordStatus.class))
                .containsExactlyInAnyOrder(
                        IncidentRecordStatus.OPEN,
                        IncidentRecordStatus.IN_REVIEW,
                        IncidentRecordStatus.RESOLVED,
                        IncidentRecordStatus.CLOSED);

        assertThat(EnumSet.allOf(InfectionRecordStatus.class))
                .containsExactlyInAnyOrder(
                        InfectionRecordStatus.ACTIVE,
                        InfectionRecordStatus.MONITORING,
                        InfectionRecordStatus.RESOLVED);

        assertThat(EnumSet.allOf(WoundRecordStatus.class))
                .containsExactlyInAnyOrder(
                        WoundRecordStatus.ACTIVE,
                        WoundRecordStatus.MONITORING,
                        WoundRecordStatus.IMPROVING,
                        WoundRecordStatus.STABLE,
                        WoundRecordStatus.DETERIORATING,
                        WoundRecordStatus.RESOLVED);

        assertThat(EnumSet.allOf(PatientEventFollowUpStatus.class))
                .containsExactlyInAnyOrder(
                        PatientEventFollowUpStatus.OPEN,
                        PatientEventFollowUpStatus.COMPLETED,
                        PatientEventFollowUpStatus.CANCELLED);

        assertThat(EnumSet.allOf(PatientEventEscalationStatus.class))
                .containsExactlyInAnyOrder(
                        PatientEventEscalationStatus.ACTIVE,
                        PatientEventEscalationStatus.CLEARED);

        assertThat(EnumSet.allOf(PatientEventHistoryEntryType.class))
                .containsExactlyInAnyOrder(
                        PatientEventHistoryEntryType.INCIDENT_EVENT,
                        PatientEventHistoryEntryType.INFECTION_EVENT,
                        PatientEventHistoryEntryType.WOUND_CREATED,
                        PatientEventHistoryEntryType.WOUND_HISTORY_CAPTURED,
                        PatientEventHistoryEntryType.FOLLOW_UP_MILESTONE,
                        PatientEventHistoryEntryType.ESCALATION_MILESTONE,
                        PatientEventHistoryEntryType.EVIDENCE_EVENT);

        assertThat(EnumSet.allOf(PatientEventAlertType.class))
                .containsExactlyInAnyOrder(
                        PatientEventAlertType.HIGH_SEVERITY_INCIDENT,
                        PatientEventAlertType.OVERDUE_FOLLOW_UP,
                        PatientEventAlertType.ACTIVE_INFECTION,
                        PatientEventAlertType.WOUND_PROGRESSION_ALERT);

        PatientEventAlertContract contract = new PatientEventAlertContract(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PatientEventAlertType.HIGH_SEVERITY_INCIDENT,
                UUID.randomUUID(),
                Epic12PatientEventTargetType.INCIDENT_RECORD.name(),
                "HIGH",
                "Escalate incident review");

        assertThat(contract.alertType()).isEqualTo(PatientEventAlertType.HIGH_SEVERITY_INCIDENT);
        assertThat(contract.targetType()).isEqualTo(Epic12PatientEventTargetType.INCIDENT_RECORD.name());
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Patient Event", "Reviewer", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
