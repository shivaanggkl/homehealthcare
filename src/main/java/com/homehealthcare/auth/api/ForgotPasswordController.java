package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.ForgotPasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordService.ForgotPasswordResult result = forgotPasswordService.requestReset(
                new ForgotPasswordService.ForgotPasswordCommand(request.email()));
        return new ForgotPasswordResponse(result.message());
    }

    record ForgotPasswordRequest(
            @NotBlank @Email String email) {
    }

    record ForgotPasswordResponse(String message) {
    }
}
