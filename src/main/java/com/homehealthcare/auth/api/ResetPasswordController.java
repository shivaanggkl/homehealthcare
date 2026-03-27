package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.ResetPasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    ResetPasswordController(ResetPasswordService resetPasswordService) {
        this.resetPasswordService = resetPasswordService;
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordService.ResetPasswordResult result = resetPasswordService.resetPassword(
                new ResetPasswordService.ResetPasswordCommand(
                        request.token(),
                        request.newPassword(),
                        request.revokeExistingSessions()));

        return new ResetPasswordResponse(
                "Password reset successful",
                result.revokedExistingSessions());
    }

    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank String newPassword,
            boolean revokeExistingSessions) {
    }

    record ResetPasswordResponse(
            String message,
            boolean revokedExistingSessions) {
    }
}
