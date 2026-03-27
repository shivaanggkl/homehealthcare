package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.PasswordPolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class PasswordPolicyController {

    private final PasswordPolicy passwordPolicy;

    PasswordPolicyController(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    @GetMapping("/password-policy")
    PasswordPolicyResponse passwordPolicy() {
        PasswordPolicy.PasswordPolicyView view = passwordPolicy.describe();
        return new PasswordPolicyResponse(
                view.minimumLength(),
                view.requireUppercase(),
                view.requireLowercase(),
                view.requireDigit(),
                view.requireSymbol(),
                view.commonPasswordCheckEnabled(),
                view.preventReuseCount(),
                view.summary());
    }

    record PasswordPolicyResponse(
            int minimumLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSymbol,
            boolean commonPasswordCheckEnabled,
            int preventReuseCount,
            String summary) {
    }
}
