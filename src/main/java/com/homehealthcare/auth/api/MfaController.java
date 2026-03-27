package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.MfaEnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/mfa")
class MfaController {

    private final MfaEnrollmentService mfaEnrollmentService;

    MfaController(MfaEnrollmentService mfaEnrollmentService) {
        this.mfaEnrollmentService = mfaEnrollmentService;
    }

    @GetMapping("/status")
    MfaStatusResponse status(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie) {
        MfaEnrollmentService.MfaStatusView status = mfaEnrollmentService.currentStatus(new MfaEnrollmentService.MfaStatusCommand(
                bearerToken(authorizationHeader, accessTokenCookie),
                firstNonBlank(sessionIdHeader, sessionIdCookie)));
        return new MfaStatusResponse(status.userId(), status.mfaEnabled(), status.enrolledAt(), status.recoveryCodesRemaining());
    }

    @PostMapping("/enrollment/start")
    ResponseEntity<EnrollmentStartResponse> start(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionIdHeader,
            @CookieValue(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
            @CookieValue(name = AuthCookieSupport.SESSION_ID_COOKIE, required = false) String sessionIdCookie,
            @Valid @RequestBody EnrollmentStartRequest request) {
        MfaEnrollmentService.EnrollmentStartResult result = mfaEnrollmentService.startEnrollment(
                new MfaEnrollmentService.EnrollmentStartCommand(
                        bearerToken(authorizationHeader, accessTokenCookie),
                        firstNonBlank(sessionIdHeader, sessionIdCookie),
                        request.currentPassword()));
        return new ResponseEntity<>(new EnrollmentStartResponse(
                result.enrollmentToken(),
                result.manualEntryKey(),
                result.otpauthUri(),
                result.expiresAt(),
                result.recoveryCodes()), HttpStatus.OK);
    }

    @PostMapping("/enrollment/confirm")
    EnrollmentConfirmResponse confirm(@Valid @RequestBody EnrollmentConfirmRequest request) {
        MfaEnrollmentService.EnrollmentConfirmResult result = mfaEnrollmentService.confirmEnrollment(
                new MfaEnrollmentService.EnrollmentConfirmCommand(
                        request.enrollmentToken(),
                        request.totpCode()));
        return new EnrollmentConfirmResponse(result.userId(), result.mfaEnabled(), result.recoveryCodesRemaining());
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

    record EnrollmentStartRequest(@NotBlank String currentPassword) {
    }

    record EnrollmentStartResponse(
            String enrollmentToken,
            String manualEntryKey,
            String otpauthUri,
            OffsetDateTime expiresAt,
            List<String> recoveryCodes) {
    }

    record EnrollmentConfirmRequest(
            @NotBlank String enrollmentToken,
            @NotBlank String totpCode) {
    }

    record EnrollmentConfirmResponse(
            UUID userId,
            boolean mfaEnabled,
            long recoveryCodesRemaining) {
    }

    record MfaStatusResponse(
            UUID userId,
            boolean mfaEnabled,
            OffsetDateTime enrolledAt,
            long recoveryCodesRemaining) {
    }
}
