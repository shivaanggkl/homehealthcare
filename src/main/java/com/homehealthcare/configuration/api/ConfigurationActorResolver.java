package com.homehealthcare.configuration.api;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigurationActorResolver {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;

    public AgencyMembership requireActorMembership() {
        return agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
    }
}
