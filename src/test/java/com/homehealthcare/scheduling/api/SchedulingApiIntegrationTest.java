package com.homehealthcare.scheduling.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingopenshift.domain.OpenShiftRepository;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRuleRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SchedulingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;

    @Autowired
    private VisitOccurrenceRepository visitOccurrenceRepository;

    @Autowired
    private CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;

    @Autowired
    private OpenShiftRepository openShiftRepository;

    @Autowired
    private RecurringVisitRuleRepository recurringVisitRuleRepository;

    @Test
    void schedulerCanManageVisitsAssignmentsOpenShiftsAndWorkflowApis() throws Exception {
        Agency agency = persistAgency();
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership scheduler = persistMembership(agency, AgencyRole.SCHEDULER_COORDINATOR, "scheduler@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                branch,
                "CG-001",
                "Casey Care",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-001",
                "Ava",
                null,
                "Patient",
                null,
                LocalDate.of(1950, 5, 20),
                "F",
                "312-555-0100",
                null,
                "ava@example.com",
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine", 60, true, 1));

        mockMvc.perform(post("/api/schedule-visits")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "branchId": "%s",
                                  "serviceLineId": "%s",
                                  "visitTypeId": "%s",
                                  "plannedStartAt": "2026-04-10T09:00:00-05:00",
                                  "plannedEndAt": "2026-04-10T10:00:00-05:00",
                                  "timezone": "America/Chicago",
                                  "priority": "urgent",
                                  "creationMode": "manual",
                                  "notes": "Initial board visit"
                                }
                                """.formatted(patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.patientId").value(patient.getId().toString()));

        UUID visitId = visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(agency.getId())
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/schedule-visits")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .param("status", "PLANNED")
                        .param("patientId", patient.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(visitId.toString()));

        mockMvc.perform(get("/api/schedule-visits/{visitId}", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitTypeId").value(visitType.getId().toString()));

        mockMvc.perform(put("/api/schedule-visits/{visitId}", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "branchId": "%s",
                                  "serviceLineId": "%s",
                                  "visitTypeId": "%s",
                                  "plannedStartAt": "2026-04-10T09:15:00-05:00",
                                  "plannedEndAt": "2026-04-10T10:15:00-05:00",
                                  "timezone": "America/Chicago",
                                  "priority": "routine",
                                  "creationMode": "manual",
                                  "notes": "Updated board visit"
                                }
                                """.formatted(patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated board visit"));

        mockMvc.perform(get("/api/schedule-visits/{visitId}/matches", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caregiverProfileId").value(caregiverProfile.getId().toString()));

        mockMvc.perform(post("/api/schedule-visits/{visitId}/conflict-preview", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "caregiverProfileId": "%s",
                                  "enforcePatientOverlapCheck": false,
                                  "requireAvailabilityFit": false
                                }
                                """.formatted(caregiverProfile.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitOccurrenceId").value(visitId.toString()))
                .andExpect(jsonPath("$.caregiverProfileId").value(caregiverProfile.getId().toString()));

        mockMvc.perform(post("/api/schedule-visits/{visitId}/assignments", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "caregiverProfileId": "%s",
                                  "branchId": "%s",
                                  "assignmentSource": "board",
                                  "notes": "Assigned from board"
                                }
                                """.formatted(caregiverProfile.getId(), branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caregiverProfileId").value(caregiverProfile.getId().toString()))
                .andExpect(jsonPath("$.assignmentStatus").value("ACTIVE"));

        UUID actualAssignmentId = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(
                        visitId,
                        com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus.ACTIVE)
                .orElseThrow()
                .getId();

        mockMvc.perform(delete("/api/schedule-visits/{visitId}/assignments/{assignmentId}", visitId, actualAssignmentId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .param("convertToOpenShift", "true")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "priority": "urgent",
                                  "notes": "Re-open for coverage"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("REMOVED"));

        UUID openShiftId = openShiftRepository.findAllByAgency_IdOrderByOpenedAtAsc(agency.getId())
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/schedule-board")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .param("view", "DAY")
                        .param("date", "2026-04-10")
                        .param("openShiftsOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].openShift").value(true))
                .andExpect(jsonPath("$.items[0].visitId").value(visitId.toString()));

        mockMvc.perform(post("/api/open-shifts/{openShiftId}/assign", openShiftId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "caregiverProfileId": "%s",
                                  "branchId": "%s",
                                  "assignmentSource": "open_shift_pool",
                                  "notes": "Claimed from pool"
                                }
                                """.formatted(caregiverProfile.getId(), branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/schedule-visits/{visitId}/reschedule", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "newPlannedStartAt": "2026-04-10T13:00:00-05:00",
                                  "newPlannedEndAt": "2026-04-10T14:00:00-05:00",
                                  "timezone": "America/Chicago",
                                  "branchId": "%s",
                                  "newCaregiverProfileId": "%s",
                                  "reason": "Patient requested afternoon"
                                }
                                """.formatted(branch.getId(), caregiverProfile.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitOccurrenceId").value(visitId.toString()))
                .andExpect(jsonPath("$.reason").value("Patient requested afternoon"));

        mockMvc.perform(post("/api/schedule-visits/{visitId}/cancel", visitId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "cancellationParty": "ADMIN_SIDE",
                                  "reason": "Patient admitted"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitOccurrenceId").value(visitId.toString()))
                .andExpect(jsonPath("$.cancellationParty").value("ADMIN_SIDE"));
    }

    @Test
    void schedulerCanManageRecurringVisitApisAndCaregiverIsDeniedSchedulingWorkspace() throws Exception {
        Agency agency = persistAgency();
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership scheduler = persistMembership(agency, AgencyRole.SCHEDULER_COORDINATOR, "scheduler@northstar.example");
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-002",
                "Mila",
                null,
                "Patient",
                null,
                LocalDate.of(1952, 1, 10),
                "F",
                null,
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Skilled Visit", "SV", "Skilled", 60, true, 1));

        mockMvc.perform(post("/api/recurring-visits")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "branchId": "%s",
                                  "serviceLineId": "%s",
                                  "visitTypeId": "%s",
                                  "cadence": "SELECTED_WEEKDAYS",
                                  "weekdays": ["MONDAY", "WEDNESDAY"],
                                  "effectiveStart": "2026-04-01",
                                  "effectiveEnd": "2026-04-08",
                                  "plannedStartTime": "09:00:00",
                                  "plannedEndTime": "10:00:00",
                                  "timezone": "America/Chicago",
                                  "priority": "high",
                                  "creationMode": "recurring",
                                  "notes": "Weekday rule"
                                }
                                """.formatted(patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cadence").value("SELECTED_WEEKDAYS"));

        UUID recurringRuleId = recurringVisitRuleRepository.findAll().stream()
                .filter(rule -> agency.getId().equals(rule.getAgencyId()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/recurring-visits")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .param("patientId", patient.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(recurringRuleId.toString()));

        mockMvc.perform(put("/api/recurring-visits/{recurringRuleId}", recurringRuleId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "branchId": "%s",
                                  "serviceLineId": "%s",
                                  "visitTypeId": "%s",
                                  "cadence": "SELECTED_WEEKDAYS",
                                  "weekdays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                                  "effectiveStart": "2026-04-01",
                                  "effectiveEnd": "2026-04-10",
                                  "plannedStartTime": "09:00:00",
                                  "plannedEndTime": "10:00:00",
                                  "timezone": "America/Chicago",
                                  "priority": "high",
                                  "creationMode": "recurring",
                                  "notes": "Expanded weekday rule"
                                }
                                """.formatted(patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Expanded weekday rule"));

        mockMvc.perform(post("/api/recurring-visits/{recurringRuleId}/expand", recurringRuleId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "windowStart": "2026-04-01",
                                  "windowEnd": "2026-04-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recurringVisitRuleId").value(recurringRuleId.toString()));

        mockMvc.perform(delete("/api/recurring-visits/{recurringRuleId}", recurringRuleId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/schedule-visits")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isForbidden());
    }

    private Agency persistAgency() {
        return agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Schedule", "User", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
