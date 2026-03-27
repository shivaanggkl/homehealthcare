package com.homehealthcare.security.tenant;

import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import java.util.UUID;

public record TenantMembership(UUID membershipId, UUID agencyId, AgencyMembershipStatus status) {

    public TenantMembership(UUID membershipId, UUID agencyId) {
        this(membershipId, agencyId, AgencyMembershipStatus.ACTIVE);
    }

    public boolean isActive() {
        return status == AgencyMembershipStatus.ACTIVE;
    }
}
