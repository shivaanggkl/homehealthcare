package com.homehealthcare.user.application;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(UserSessionService.class)
public class LoggingUserSessionService implements UserSessionService {

    @Override
    public List<UUID> revokeAllSessions(UUID userId, String reason) {
        log.info("Revoked active sessions userId={} reason={}", userId, reason);
        return List.of();
    }
}
