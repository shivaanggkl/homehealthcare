package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.UserSessionManagementService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/sessions")
class UserSessionsController {

    private final UserSessionManagementService userSessionManagementService;

    UserSessionsController(UserSessionManagementService userSessionManagementService) {
        this.userSessionManagementService = userSessionManagementService;
    }

    @GetMapping
    List<UserSessionResponse> listSessions(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie) {
        return userSessionManagementService.listSessions(
                        bearerToken(authorizationHeader, accessTokenCookie),
                        firstNonBlank(sessionIdHeader, sessionIdCookie))
                .stream()
                .map(session -> new UserSessionResponse(
                        session.sessionId(),
                        session.current(),
                        session.active(),
                        session.createdAt(),
                        session.lastActivityAt(),
                        session.absoluteExpiresAt(),
                        session.revokedAt(),
                        session.revocationReason()))
                .toList();
    }

    @DeleteMapping("/{sessionId}")
    ResponseEntity<RevokeSessionResponse> revokeSession(
            @PathVariable UUID sessionId,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie) {
        UserSessionManagementService.RevokedSessionResult result = userSessionManagementService.revokeSession(
                new UserSessionManagementService.RevokeSessionCommand(
                        bearerToken(authorizationHeader, accessTokenCookie),
                        firstNonBlank(sessionIdHeader, sessionIdCookie),
                        sessionId));
        return new ResponseEntity<>(
                new RevokeSessionResponse(result.sessionId(), result.revoked(), result.revocationReason()),
                HttpStatus.OK);
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

    record UserSessionResponse(
            UUID sessionId,
            boolean current,
            boolean active,
            Instant createdAt,
            OffsetDateTime lastActivityAt,
            OffsetDateTime absoluteExpiresAt,
            OffsetDateTime revokedAt,
            String revocationReason) {
    }

    record RevokeSessionResponse(
            UUID sessionId,
            boolean revoked,
            String revocationReason) {
    }
}
