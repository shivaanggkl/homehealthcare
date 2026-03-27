package com.homehealthcare.user.application;

import java.util.UUID;
import java.util.List;

public interface UserSessionService {

    List<UUID> revokeAllSessions(UUID userId, String reason);
}
