package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionTimeoutService {

    private static final String ACTION_USER_SESSION_TIMED_OUT = "USER_SESSION_TIMED_OUT";

    private final AuthSessionRepository authSessionRepository;
    private final AuditEventRepository auditEventRepository;
    private final SessionSecurityProperties sessionSecurityProperties;

    @Transactional
    public boolean enforceTimeouts(AuthSession session) {
        OffsetDateTime now = OffsetDateTime.now();
        if (session.isRevoked()) {
            return false;
        }
        if (session.isAbsoluteExpiredAt(now)) {
            revokeForTimeout(session, "SESSION_ABSOLUTE_TIMEOUT", "absolute");
            return false;
        }
        if (session.isIdleExpiredAt(now, sessionSecurityProperties.getIdleTimeout())) {
            revokeForTimeout(session, "SESSION_IDLE_TIMEOUT", "idle");
            return false;
        }
        return true;
    }

    @Transactional
    public void recordActivity(AuthSession session) {
        session.recordActivity(OffsetDateTime.now());
        authSessionRepository.save(session);
    }

    private void revokeForTimeout(AuthSession session, String reason, String timeoutType) {
        session.revoke(reason, OffsetDateTime.now());
        authSessionRepository.save(session);
        auditEventRepository.save(AuditEvent.create(
                "USER",
                session.getUser().getId(),
                session.getUser().getEmail(),
                ACTION_USER_SESSION_TIMED_OUT,
                "AUTH_SESSION",
                session.getId(),
                null,
                "{\"timeoutType\":\"" + timeoutType + "\",\"sessionId\":\"" + session.getId() + "\"}"));
    }
}
