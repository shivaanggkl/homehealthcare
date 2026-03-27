package com.homehealthcare.user.application;

import java.util.UUID;

public interface UserSessionService {

    void revokeAllSessions(UUID userId, String reason);
}
