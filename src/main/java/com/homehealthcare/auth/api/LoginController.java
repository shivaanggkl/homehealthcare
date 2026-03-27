package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.EmailPasswordLoginService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class LoginController {

    private final EmailPasswordLoginService emailPasswordLoginService;

    LoginController(EmailPasswordLoginService emailPasswordLoginService) {
        this.emailPasswordLoginService = emailPasswordLoginService;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        EmailPasswordLoginService.LoginResult result = emailPasswordLoginService.login(
                new EmailPasswordLoginService.LoginCommand(
                        request.email(),
                        request.password(),
                        clientIpAddress(httpServletRequest)));

        LoginResponse response = new LoginResponse(
                result.userId(),
                result.mfaRequired(),
                result.loginChallengeToken(),
                result.sessionId(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt());
        HttpHeaders headers = new HttpHeaders();
        if (!result.mfaRequired()) {
            AuthCookieSupport.addLoginCookies(headers, response);
        }
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    private static String clientIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    record LoginResponse(
            UUID userId,
            boolean mfaRequired,
            String loginChallengeToken,
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {
    }
}
