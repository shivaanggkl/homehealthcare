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
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.foundation.Epic3PatientAuditAction;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.foundation.Epic5SchedulingAuditAction;
import com.homehealthcare.scheduling.foundation.Epic5SchedulingTargetType;
import com.homehealthcare.mobile.foundation.Epic6MobileAuditAction;
import com.homehealthcare.mobile.foundation.Epic6MobileTargetType;
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
