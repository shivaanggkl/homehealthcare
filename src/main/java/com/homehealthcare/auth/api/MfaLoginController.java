package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.MfaLoginChallengeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class MfaLoginController {

    private final MfaLoginChallengeService mfaLoginChallengeService;

    MfaLoginController(MfaLoginChallengeService mfaLoginChallengeService) {
        this.mfaLoginChallengeService = mfaLoginChallengeService;
    }

    @PostMapping("/login/mfa")
    ResponseEntity<MfaLoginResponse> complete(@Valid @RequestBody MfaLoginRequest request) {
        MfaLoginChallengeService.CompletedLoginResult result = mfaLoginChallengeService.completeChallenge(
                new MfaLoginChallengeService.CompleteLoginChallengeCommand(
                        request.challengeToken(),
                        request.totpCode(),
                        request.recoveryCode()));

        MfaLoginResponse response = new MfaLoginResponse(
                result.userId(),
                result.sessionId(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt(),
                result.recoveryCodeUsed());
        HttpHeaders headers = new HttpHeaders();
        AuthCookieSupport.addLoginCookies(headers, new LoginController.LoginResponse(
                result.userId(),
                false,
                null,
                result.sessionId(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt()));
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    record MfaLoginRequest(
            @NotBlank String challengeToken,
            String totpCode,
            String recoveryCode) {
    }

    record MfaLoginResponse(
            UUID userId,
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt,
            boolean recoveryCodeUsed) {
    }
}
