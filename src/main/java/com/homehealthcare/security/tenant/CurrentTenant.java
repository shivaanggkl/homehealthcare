package com.homehealthcare.security.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenant {

    public Optional<TenantContext> get() {
        return TenantContextHolder.get();
    }

    public TenantContext require() {
        return get().orElseThrow(() -> new TenantContextException("No tenant context is bound to the current request"));
    }

    public UUID requireAgencyId() {
        return require().agencyId();
    }

    public UUID requireMembershipId() {
        return require().membershipId();
    }
}
