package com.homehealthcare.security.branch;

import com.homehealthcare.security.tenant.TenantAccessPrincipal;
import java.util.Set;
import java.util.UUID;

public interface BranchAccessPrincipal extends TenantAccessPrincipal {

    AgencyRole agencyRole();

    Set<UUID> assignedBranchIds();
}
