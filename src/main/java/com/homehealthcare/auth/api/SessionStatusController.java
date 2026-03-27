package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.CurrentAuthSessionResolver;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/session")
class SessionStatusController {

    private final CurrentAuthSessionResolver currentAuthSessionResolver;

    SessionStatusController(CurrentAuthSessionResolver currentAuthSessionResolver) {
        this.currentAuthSessionResolver = currentAuthSessionResolver;
    }

    @GetMapping
    SessionStatusResponse status(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie) {
        CurrentAuthSessionResolver.SessionSnapshot snapshot = currentAuthSessionResolver.requireActiveSnapshot(
                bearerToken(authorizationHeader, accessTokenCookie),
                firstNonBlank(sessionIdHeader, sessionIdCookie));
        return new SessionStatusResponse(
                snapshot.sessionId(),
                snapshot.userId(),
                snapshot.idleTimeoutAt(),
                snapshot.absoluteTimeoutAt(),
                snapshot.forcedLogoutAt(),
                snapshot.warningRequired(),
                snapshot.secondsUntilForcedLogout());
    }

    private static String bearerToken(String authorizationHeader, String fallbackCookie) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return fallbackCookie;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    record SessionStatusResponse(
            UUID sessionId,
            UUID userId,
            OffsetDateTime idleTimeoutAt,
            OffsetDateTime absoluteTimeoutAt,
            OffsetDateTime forcedLogoutAt,
            boolean warningRequired,
            long secondsUntilForcedLogout) {
    }
}
