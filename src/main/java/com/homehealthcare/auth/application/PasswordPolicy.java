package com.homehealthcare.auth.application;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    private final PasswordPolicyProperties properties;
    private final CommonPasswordChecker commonPasswordChecker;

    public void validate(String password) {
        validate(password, List.of(), null);
    }

    public void validate(String password, Collection<String> recentPasswordHashes, PasswordEncoder passwordEncoder) {
        if (password == null || password.length() < properties.getMinimumLength()) {
            throw new WeakPasswordException(describe().summary());
        }
        if (properties.isRequireUppercase() && !password.chars().anyMatch(Character::isUpperCase)) {
            throw new WeakPasswordException(describe().summary());
        }
        if (properties.isRequireLowercase() && !password.chars().anyMatch(Character::isLowerCase)) {
            throw new WeakPasswordException(describe().summary());
        }
        if (properties.isRequireDigit() && !password.chars().anyMatch(Character::isDigit)) {
            throw new WeakPasswordException(describe().summary());
        }
        if (properties.isRequireSymbol() && password.chars().noneMatch(PasswordPolicy::isSymbol)) {
            throw new WeakPasswordException(describe().summary());
        }
        if (properties.isCommonPasswordCheckEnabled() && commonPasswordChecker.isCommonPassword(password)) {
            throw new WeakPasswordException("Password is too common or compromised. Choose a less predictable password");
        }
        if (passwordEncoder != null
                && properties.getPreventReuseCount() > 0
                && recentPasswordHashes != null
                && recentPasswordHashes.stream()
                        .filter(hash -> hash != null && !hash.isBlank())
                        .anyMatch(hash -> passwordEncoder.matches(password, hash))) {
            throw new PasswordReuseNotAllowedException(properties.getPreventReuseCount());
        }
    }

    public PasswordPolicyView describe() {
        String summary = "Password must be at least " + properties.getMinimumLength()
                + " characters and include upper, lower, digit, and symbol characters";
        return new PasswordPolicyView(
                properties.getMinimumLength(),
                properties.isRequireUppercase(),
                properties.isRequireLowercase(),
                properties.isRequireDigit(),
                properties.isRequireSymbol(),
                properties.isCommonPasswordCheckEnabled(),
                properties.getPreventReuseCount(),
                summary);
    }

    private static boolean isSymbol(int character) {
        return !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
    }

    public record PasswordPolicyView(
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
