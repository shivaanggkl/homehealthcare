package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.RefreshSessionService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class RefreshSessionController {

    private final RefreshSessionService refreshSessionService;

    RefreshSessionController(RefreshSessionService refreshSessionService) {
        this.refreshSessionService = refreshSessionService;
    }

    @PostMapping("/refresh")
    ResponseEntity<RefreshResponse> refresh(
            @RequestHeader(name = "X-Refresh-Token", required = false) String refreshTokenHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie) {
        RefreshSessionService.RefreshedSession refreshedSession = refreshSessionService.refresh(
                new RefreshSessionService.RefreshCommand(
                        firstNonBlank(refreshTokenHeader, refreshTokenCookie),
                        firstNonBlank(sessionIdHeader, sessionIdCookie)));
        RefreshResponse response = new RefreshResponse(
                refreshedSession.userId(),
                refreshedSession.sessionId(),
                refreshedSession.accessToken(),
                refreshedSession.accessTokenExpiresAt(),
                refreshedSession.refreshToken(),
                refreshedSession.refreshTokenExpiresAt());
        HttpHeaders headers = new HttpHeaders();
        AuthCookieSupport.addRefreshCookies(headers, response);
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    record RefreshResponse(
            UUID userId,
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {
    }
}
