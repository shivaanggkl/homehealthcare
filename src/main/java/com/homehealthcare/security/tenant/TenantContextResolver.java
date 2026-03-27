package com.homehealthcare.security.tenant;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class TenantContextResolver {

    public Optional<TenantContext> resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        TenantAccessPrincipal tenantPrincipal = extractTenantPrincipal(authentication)
                .orElseThrow(() -> new TenantContextException("Authenticated request is missing tenant membership context"));

        Set<TenantMembership> memberships = tenantPrincipal.memberships().stream()
                .filter(TenantMembership::isActive)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (memberships.isEmpty()) {
            throw new TenantContextException("Authenticated request has no active agency memberships");
        }

        UUID currentAgencyId = tenantPrincipal.currentAgencyId();
        if (currentAgencyId != null) {
            return Optional.of(memberships.stream()
                    .filter(membership -> membership.agencyId().equals(currentAgencyId))
                    .findFirst()
                    .map(membership -> new TenantContext(membership.agencyId(), membership.membershipId()))
                    .orElseThrow(() -> new TenantContextException("Authenticated request tried to access an unauthorized agency")));
        }

        if (memberships.size() == 1) {
            TenantMembership membership = memberships.iterator().next();
            return Optional.of(new TenantContext(membership.agencyId(), membership.membershipId()));
        }

        throw new TenantContextException("Authenticated request must select an agency when multiple memberships exist");
    }

    private Optional<TenantAccessPrincipal> extractTenantPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof TenantAccessPrincipal tenantPrincipal) {
            return Optional.of(tenantPrincipal);
        }
        if (authentication.getDetails() instanceof TenantAccessPrincipal tenantPrincipal) {
            return Optional.of(tenantPrincipal);
        }
        return Optional.empty();
    }
}
