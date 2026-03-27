package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.LogoutService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class LogoutController {

    private final LogoutService logoutService;

    LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Refresh-Token", required = false) String refreshTokenHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie,
            @RequestParam(name = "redirectTo", required = false) String redirectTo) {
        LogoutService.LogoutResult result = logoutService.logout(new LogoutService.LogoutCommand(
                bearerToken(authorizationHeader, accessTokenCookie),
                firstNonBlank(refreshTokenHeader, refreshTokenCookie),
                firstNonBlank(sessionIdHeader, sessionIdCookie),
                redirectTo));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, result.redirectTo());
        AuthCookieSupport.addLogoutCookies(headers);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
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
}
