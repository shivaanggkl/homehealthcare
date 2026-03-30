package com.homehealthcare.platform.audit.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationAuditAction;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.documentation.foundation.Epic8DocumentationAuditAction;
import com.homehealthcare.documentation.foundation.Epic8DocumentationTargetType;
import com.homehealthcare.messaging.foundation.Epic9MessagingAuditAction;
import com.homehealthcare.messaging.foundation.Epic9MessagingTargetType;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.review.foundation.Epic10ReviewAuditAction;
import com.homehealthcare.review.foundation.Epic10ReviewTargetType;
import com.homehealthcare.compliance.foundation.Epic11ComplianceAuditAction;
import com.homehealthcare.compliance.foundation.Epic11ComplianceTargetType;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventAuditAction;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.careprogression.foundation.Epic13CareProgressionAuditAction;
import com.homehealthcare.careprogression.foundation.Epic13CareProgressionTargetType;
import com.homehealthcare.revenuereadiness.foundation.Epic14RevenueReadinessAuditAction;
import com.homehealthcare.revenuereadiness.foundation.Epic14RevenueReadinessTargetType;
import com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction;
import com.homehealthcare.analytics.foundation.Epic15AnalyticsTargetType;
import com.homehealthcare.patient.foundation.Epic3PatientAuditAction;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.foundation.Epic5SchedulingAuditAction;
import com.homehealthcare.scheduling.foundation.Epic5SchedulingTargetType;
import com.homehealthcare.mobile.foundation.Epic6MobileAuditAction;
import com.homehealthcare.mobile.foundation.Epic6MobileTargetType;
import com.homehealthcare.evv.foundation.Epic7EvvAuditAction;
import com.homehealthcare.evv.foundation.Epic7EvvTargetType;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.TenantAccessPrincipal;
import com.homehealthcare.security.tenant.TenantMembership;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.workforce.foundation.Epic4WorkforceAuditAction;
import com.homehealthcare.workforce.foundation.Epic4WorkforceTargetType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditEventApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void ownerCanFilterAndExportAuditEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        User targetUser = createUser("Casey", "Caregiver");

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                "USER_STATUS_CHANGED",
                "USER",
                targetUser.getId(),
                agency.getId(),
                null,
                "{\"newStatus\":\"LOCKED\"}"));
        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                "AGENCY_SETTINGS_UPDATED",
                "AGENCY",
                agency.getId(),
                agency.getId(),
                null,
                "{\"timezone\":\"America/New_York\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", "USER_STATUS_CHANGED")
                        .param("targetUserId", targetUser.getId().toString())
                        .param("from", Instant.now().minusSeconds(3600).toString())
                        .param("to", Instant.now().plusSeconds(3600).toString())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value("USER_STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[0].targetId").value(targetUser.getId().toString()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", "USER_STATUS_CHANGED")
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER_STATUS_CHANGED")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(targetUser.getId().toString())));
    }

    @Test
    void auditApiCanFilterAndExportEpic2ConfigurationEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID serviceLineId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic2ConfigurationAuditAction.CREATED.actionType(),
                Epic2ConfigurationTargetType.SERVICE_LINE.name(),
                serviceLineId,
                agency.getId(),
                null,
                "{\"code\":\"PD\",\"status\":\"ACTIVE\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic2ConfigurationAuditAction.CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic2ConfigurationAuditAction.CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic2ConfigurationTargetType.SERVICE_LINE.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic2ConfigurationAuditAction.CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic2ConfigurationTargetType.SERVICE_LINE.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic2ConfigurationAuditAction.CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic3PatientEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID patientId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic3PatientAuditAction.CREATED.actionType(),
                Epic3PatientTargetType.PATIENT.name(),
                patientId,
                agency.getId(),
                null,
                "{\"status\":\"ACTIVE\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic3PatientAuditAction.CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic3PatientAuditAction.CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic3PatientTargetType.PATIENT.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic3PatientAuditAction.CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic3PatientTargetType.PATIENT.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic3PatientAuditAction.CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic4WorkforceEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID caregiverProfileId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic4WorkforceAuditAction.CREATED.actionType(),
                Epic4WorkforceTargetType.CAREGIVER_PROFILE.name(),
                caregiverProfileId,
                agency.getId(),
                null,
                "{\"status\":\"ACTIVE\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic4WorkforceAuditAction.CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic4WorkforceAuditAction.CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic4WorkforceTargetType.CAREGIVER_PROFILE.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic4WorkforceAuditAction.CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic4WorkforceTargetType.CAREGIVER_PROFILE.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic4WorkforceAuditAction.CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic11ComplianceEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID projectionId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED.actionType(),
                Epic11ComplianceTargetType.STATUS_PROJECTION.name(),
                projectionId,
                agency.getId(),
                null,
                "{\"readiness\":\"WARNING\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic11ComplianceTargetType.STATUS_PROJECTION.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic11ComplianceTargetType.STATUS_PROJECTION.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic12PatientEventEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID incidentId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType(),
                Epic12PatientEventTargetType.INCIDENT_RECORD.name(),
                incidentId,
                agency.getId(),
                null,
                "{\"severity\":\"HIGH\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic12PatientEventTargetType.INCIDENT_RECORD.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic12PatientEventTargetType.INCIDENT_RECORD.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic13CareProgressionEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID patientGoalId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic13CareProgressionAuditAction.PATIENT_GOAL_CREATED.actionType(),
                Epic13CareProgressionTargetType.PATIENT_GOAL.name(),
                patientGoalId,
                agency.getId(),
                null,
                "{\"status\":\"ACTIVE\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic13CareProgressionAuditAction.PATIENT_GOAL_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic13CareProgressionAuditAction.PATIENT_GOAL_CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic13CareProgressionTargetType.PATIENT_GOAL.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic13CareProgressionAuditAction.PATIENT_GOAL_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic13CareProgressionTargetType.PATIENT_GOAL.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic13CareProgressionAuditAction.PATIENT_GOAL_CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic14RevenueReadinessEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID readinessProjectionId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED.actionType(),
                Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION.name(),
                readinessProjectionId,
                agency.getId(),
                null,
                "{\"status\":\"BLOCKED\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic15AnalyticsEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID snapshotId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType(),
                Epic15AnalyticsTargetType.DASHBOARD_METRIC_SNAPSHOT.name(),
                snapshotId,
                agency.getId(),
                null,
                "{\"metric\":\"TODAYS_VISITS\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic15AnalyticsTargetType.DASHBOARD_METRIC_SNAPSHOT.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic15AnalyticsTargetType.DASHBOARD_METRIC_SNAPSHOT.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic5SchedulingEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID visitId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic5SchedulingAuditAction.VISIT_CREATED.actionType(),
                Epic5SchedulingTargetType.VISIT_OCCURRENCE.name(),
                visitId,
                agency.getId(),
                null,
                "{\"status\":\"PLANNED\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic5SchedulingAuditAction.VISIT_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic5SchedulingAuditAction.VISIT_CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic5SchedulingTargetType.VISIT_OCCURRENCE.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic5SchedulingAuditAction.VISIT_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic5SchedulingTargetType.VISIT_OCCURRENCE.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic5SchedulingAuditAction.VISIT_CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic10ReviewEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID reviewWorkItemId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType(),
                Epic10ReviewTargetType.REVIEW_WORK_ITEM.name(),
                reviewWorkItemId,
                agency.getId(),
                null,
                "{\"status\":\"PENDING_REVIEW\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic10ReviewTargetType.REVIEW_WORK_ITEM.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic10ReviewTargetType.REVIEW_WORK_ITEM.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic6MobileEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID sessionId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic6MobileAuditAction.MOBILE_SESSION_BOOTSTRAPPED.actionType(),
                Epic6MobileTargetType.MOBILE_DEVICE_SESSION.name(),
                sessionId,
                agency.getId(),
                null,
                "{\"offlineSyncEnabled\":true}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic6MobileAuditAction.MOBILE_SESSION_BOOTSTRAPPED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic6MobileAuditAction.MOBILE_SESSION_BOOTSTRAPPED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic6MobileTargetType.MOBILE_DEVICE_SESSION.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic6MobileAuditAction.MOBILE_SESSION_BOOTSTRAPPED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic6MobileTargetType.MOBILE_DEVICE_SESSION.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic6MobileAuditAction.MOBILE_SESSION_BOOTSTRAPPED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic7EvvEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID clockEventId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType(),
                Epic7EvvTargetType.EVV_CLOCK_EVENT.name(),
                clockEventId,
                agency.getId(),
                null,
                "{\"verificationStatus\":\"PENDING_VERIFICATION\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic7EvvTargetType.EVV_CLOCK_EVENT.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic7EvvTargetType.EVV_CLOCK_EVENT.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic8DocumentationEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID documentationId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED.actionType(),
                Epic8DocumentationTargetType.VISIT_DOCUMENTATION_RECORD.name(),
                documentationId,
                agency.getId(),
                null,
                "{\"status\":\"DRAFT\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic8DocumentationTargetType.VISIT_DOCUMENTATION_RECORD.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic8DocumentationTargetType.VISIT_DOCUMENTATION_RECORD.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED.actionType())));
    }

    @Test
    void auditApiCanFilterAndExportEpic9MessagingEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        UUID threadId = UUID.randomUUID();

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                ownerMembership.getId(),
                ownerMembership.getUser().getEmail(),
                Epic9MessagingAuditAction.THREAD_CREATED.actionType(),
                Epic9MessagingTargetType.COMMUNICATION_THREAD.name(),
                threadId,
                agency.getId(),
                null,
                "{\"threadType\":\"DIRECT_SECURE\"}"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", Epic9MessagingAuditAction.THREAD_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value(Epic9MessagingAuditAction.THREAD_CREATED.actionType()))
                .andExpect(jsonPath("$.content[0].targetType").value(Epic9MessagingTargetType.COMMUNICATION_THREAD.name()));

        mockMvc.perform(get("/api/audit-events/export")
                        .param("actionType", Epic9MessagingAuditAction.THREAD_CREATED.actionType())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic9MessagingTargetType.COMMUNICATION_THREAD.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Epic9MessagingAuditAction.THREAD_CREATED.actionType())));
    }

    @Test
    void caregiverCannotViewAuditEvents() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiverMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/audit-events")
                        .with(authentication(authenticationFor(caregiverMembership))))
                .andExpect(status().isForbidden());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(AgencyMembership membership) {
        TestTenantPrincipal principal = new TestTenantPrincipal(
                membership.getAgencyId(),
                Set.of(new TenantMembership(membership.getId(), membership.getAgencyId())));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        authentication.setDetails(principal);
        return authentication;
    }

    record TestTenantPrincipal(UUID currentAgencyId, Set<TenantMembership> memberships) implements TenantAccessPrincipal {
    }
}
