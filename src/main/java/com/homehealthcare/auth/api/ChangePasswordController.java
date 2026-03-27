package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.ChangePasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class ChangePasswordController {

    private final ChangePasswordService changePasswordService;

    ChangePasswordController(ChangePasswordService changePasswordService) {
        this.changePasswordService = changePasswordService;
    }

    @PostMapping("/change-password")
    ResponseEntity<ChangePasswordResponse> changePassword(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Refresh-Token", required = false) String refreshTokenHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie,
            @Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordService.ChangePasswordResult result = changePasswordService.changePassword(
                new ChangePasswordService.ChangePasswordCommand(
                        bearerToken(authorizationHeader, accessTokenCookie),
                        firstNonBlank(refreshTokenHeader, refreshTokenCookie),
                        firstNonBlank(sessionIdHeader, sessionIdCookie),
                        request.currentPassword(),
                        request.newPassword(),
                        request.invalidateOtherSessions()));

        return new ResponseEntity<>(
                new ChangePasswordResponse("Password changed successfully", result.invalidatedOtherSessions()),
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

    record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword,
            boolean invalidateOtherSessions) {
    }

    record ChangePasswordResponse(
            String message,
            boolean invalidatedOtherSessions) {
    }
}
