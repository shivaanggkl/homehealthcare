package com.homehealthcare.security.authorization;

import com.homehealthcare.membership.domain.AgencyMembership;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class AgencyAuthorizationGuard {

    public boolean hasPermission(AgencyMembership actorMembership, AgencyPermission permission) {
        Objects.requireNonNull(actorMembership, "actorMembership must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
        return actorMembership.isActive() && permission.isAllowedFor(actorMembership.getRole());
    }

    public void requirePermission(
            AgencyMembership actorMembership,
            AgencyPermission permission,
            Function<UUID, ? extends RuntimeException> deniedExceptionFactory) {
        Objects.requireNonNull(deniedExceptionFactory, "deniedExceptionFactory must not be null");
        if (!hasPermission(actorMembership, permission)) {
            throw deniedExceptionFactory.apply(actorMembership.getId());
        }
    }
}
