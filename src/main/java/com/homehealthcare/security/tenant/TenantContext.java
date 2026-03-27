package com.homehealthcare.security.tenant;

import java.util.UUID;

public record TenantContext(UUID agencyId, UUID membershipId) {
}
