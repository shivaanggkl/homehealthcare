package com.homehealthcare.auth.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

final class AuthCookieSupport {

    static final String ACCESS_TOKEN_COOKIE = "hhc_access_token";
    static final String REFRESH_TOKEN_COOKIE = "hhc_refresh_token";
    static final String SESSION_ID_COOKIE = "hhc_session_id";

    private AuthCookieSupport() {
    }

    static void addLoginCookies(HttpHeaders headers, LoginController.LoginResponse response) {
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(
                ACCESS_TOKEN_COOKIE,
                response.accessToken(),
                Math.max(0, response.accessTokenExpiresAt().toEpochSecond() - java.time.OffsetDateTime.now().toEpochSecond())));
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(
                REFRESH_TOKEN_COOKIE,
                response.refreshToken(),
                Math.max(0, response.refreshTokenExpiresAt().toEpochSecond() - java.time.OffsetDateTime.now().toEpochSecond())));
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(
                SESSION_ID_COOKIE,
                response.sessionId().toString(),
                Math.max(0, response.refreshTokenExpiresAt().toEpochSecond() - java.time.OffsetDateTime.now().toEpochSecond())));
    }

    static void addLogoutCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, expireCookie(ACCESS_TOKEN_COOKIE));
        headers.add(HttpHeaders.SET_COOKIE, expireCookie(REFRESH_TOKEN_COOKIE));
        headers.add(HttpHeaders.SET_COOKIE, expireCookie(SESSION_ID_COOKIE));
    }

    private static String buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    private static String expireCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }
}
