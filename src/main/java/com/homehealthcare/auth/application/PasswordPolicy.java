package com.homehealthcare.auth.application;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public void validate(String password) {
        if (password == null
                || password.length() < 12
                || !password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)
                || password.chars().noneMatch(PasswordPolicy::isSymbol)) {
            throw new WeakPasswordException();
        }
    }

    private static boolean isSymbol(int character) {
        return !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
    }
}
