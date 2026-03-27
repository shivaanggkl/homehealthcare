package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.EmailPasswordLoginService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
class LoginController {

    private final EmailPasswordLoginService emailPasswordLoginService;

    LoginController(EmailPasswordLoginService emailPasswordLoginService) {
        this.emailPasswordLoginService = emailPasswordLoginService;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        EmailPasswordLoginService.LoginResult result = emailPasswordLoginService.login(
                new EmailPasswordLoginService.LoginCommand(
                        request.email(),
                        request.password()));

        LoginResponse response = new LoginResponse(
                result.userId(),
                result.sessionId(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt());
        HttpHeaders headers = new HttpHeaders();
        AuthCookieSupport.addLoginCookies(headers, response);
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    record LoginResponse(
            UUID userId,
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {
    }
}
