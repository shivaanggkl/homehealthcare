package com.homehealthcare.mobile.foundation;

import com.homehealthcare.security.authorization.AgencyPermission;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record MobileSessionContext(
        UUID userId,
        UUID agencyId,
        UUID membershipId,
        UUID caregiverProfileId,
        UUID branchId,
        String sessionId,
        Set<AgencyPermission> grantedPermissions,
        boolean offlineSyncEnabled) {

    public MobileSessionContext {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        Objects.requireNonNull(membershipId, "membershipId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        grantedPermissions = grantedPermissions == null ? Set.of() : Set.copyOf(grantedPermissions);
    }
}
