package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserSessionManagementService {

    private static final String ACTION_USER_SESSION_REVOKED = "USER_SESSION_REVOKED";

    private final CurrentAuthSessionResolver currentAuthSessionResolver;
    private final AuthSessionRepository authSessionRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public List<UserSessionView> listSessions(String accessToken, String sessionId) {
        AuthSession currentSession = currentAuthSessionResolver.requireActive(accessToken, sessionId);
        return authSessionRepository.findTop10ByUser_IdOrderByLastActivityAtDesc(currentSession.getUser().getId())
                .stream()
                .map(session -> toView(session, currentSession.getId()))
                .toList();
    }

    @Transactional
    public RevokedSessionResult revokeSession(@NotNull RevokeSessionCommand command) {
        AuthSession currentSession = currentAuthSessionResolver.requireActive(command.accessToken(), command.currentSessionId());
        AuthSession targetSession = authSessionRepository.findByIdAndUser_Id(command.targetSessionId(), currentSession.getUser().getId())
                .orElseThrow(ManagedSessionNotFoundException::new);

        if (targetSession.getId().equals(currentSession.getId())) {
            throw new CurrentSessionRevocationNotAllowedException();
        }

        targetSession.revoke("USER_SELF_REVOKED", OffsetDateTime.now());
        auditEventRepository.save(AuditEvent.create(
                "USER",
                currentSession.getUser().getId(),
                currentSession.getUser().getEmail(),
                ACTION_USER_SESSION_REVOKED,
                "AUTH_SESSION",
                targetSession.getId(),
                null,
                "{\"reason\":\"USER_SELF_REVOKED\",\"currentSessionId\":\"" + currentSession.getId() + "\"}"));

        return new RevokedSessionResult(targetSession.getId(), targetSession.isRevoked(), targetSession.getRevocationReason());
    }

    public void auditAdminRevocations(UUID actorMembershipId, String actorEmail, UUID agencyId, List<UUID> sessionIds, String reason) {
        for (UUID sessionId : sessionIds) {
            auditEventRepository.save(AuditEvent.create(
                    "AGENCY_MEMBERSHIP",
                    actorMembershipId,
                    actorEmail,
                    ACTION_USER_SESSION_REVOKED,
                    "AUTH_SESSION",
                    sessionId,
                    agencyId,
                    "{\"reason\":\"" + reason + "\"}"));
        }
    }

    private static UserSessionView toView(AuthSession session, UUID currentSessionId) {
        return new UserSessionView(
                session.getId(),
                session.getId().equals(currentSessionId),
                !session.isRevoked(),
                session.getCreatedAt(),
                session.getLastActivityAt(),
                session.getAbsoluteExpiresAt(),
                session.getRevokedAt(),
                session.getRevocationReason());
    }

    public record RevokeSessionCommand(
            @NotBlank String accessToken,
            @NotBlank String currentSessionId,
            @NotNull UUID targetSessionId) {
    }

    public record UserSessionView(
            UUID sessionId,
            boolean current,
            boolean active,
            Instant createdAt,
            OffsetDateTime lastActivityAt,
            OffsetDateTime absoluteExpiresAt,
            OffsetDateTime revokedAt,
            String revocationReason) {
    }

    public record RevokedSessionResult(
            UUID sessionId,
            boolean revoked,
            String revocationReason) {
    }
}
