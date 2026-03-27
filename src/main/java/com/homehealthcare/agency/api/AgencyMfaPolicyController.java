package com.homehealthcare.agency.api;

import com.homehealthcare.agency.application.AgencyMfaPolicyService;
import com.homehealthcare.agency.domain.AgencyMfaPolicyMode;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/mfa-policy")
class AgencyMfaPolicyController {

    private final AgencyMfaPolicyService agencyMfaPolicyService;

    AgencyMfaPolicyController(AgencyMfaPolicyService agencyMfaPolicyService) {
        this.agencyMfaPolicyService = agencyMfaPolicyService;
    }

    @GetMapping
    MfaPolicyResponse currentPolicy() {
        AgencyMfaPolicyService.AgencyMfaPolicyView policy = agencyMfaPolicyService.currentPolicy();
        return new MfaPolicyResponse(policy.agencyId(), policy.mode(), policy.requiredRoles());
    }

    @PutMapping
    MfaPolicyResponse updatePolicy(@Valid @RequestBody UpdateMfaPolicyRequest request) {
        AgencyMfaPolicyService.AgencyMfaPolicyView policy = agencyMfaPolicyService.updatePolicy(
                new AgencyMfaPolicyService.UpdateAgencyMfaPolicyCommand(request.mode(), request.requiredRoles()));
        return new MfaPolicyResponse(policy.agencyId(), policy.mode(), policy.requiredRoles());
    }

    record UpdateMfaPolicyRequest(
            @NotNull AgencyMfaPolicyMode mode,
            Set<AgencyRole> requiredRoles) {
    }

    record MfaPolicyResponse(
            UUID agencyId,
            AgencyMfaPolicyMode mode,
            Set<AgencyRole> requiredRoles) {
    }
}
