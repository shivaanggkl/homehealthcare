package com.homehealthcare.security.tenant;

import java.util.Set;
import java.util.UUID;

public interface TenantAccessPrincipal {

    UUID currentAgencyId();

    Set<TenantMembership> memberships();
}
