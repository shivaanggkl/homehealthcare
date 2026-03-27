package com.homehealthcare.user.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersistentUserSessionService implements UserSessionService {

    private final AuthSessionRepository authSessionRepository;

    @Override
    @Transactional
    public List<UUID> revokeAllSessions(UUID userId, String reason) {
        List<UUID> revokedSessionIds = new ArrayList<>();
        for (AuthSession authSession : authSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId)) {
            authSession.revoke(reason, OffsetDateTime.now());
            if (authSession.isRevoked()) {
                revokedSessionIds.add(authSession.getId());
            }
        }
        return List.copyOf(revokedSessionIds);
    }
}
