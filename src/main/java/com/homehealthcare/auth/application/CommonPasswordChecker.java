package com.homehealthcare.auth.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class CommonPasswordChecker {

    private final Set<String> blockedPasswords;

    public CommonPasswordChecker(ResourceLoader resourceLoader) {
        this.blockedPasswords = loadBlockedPasswords(resourceLoader.getResource("classpath:security/common-passwords.txt"));
    }

    public boolean isCommonPassword(String password) {
        if (password == null) {
            return false;
        }
        return blockedPasswords.contains(password.trim().toLowerCase(Locale.ROOT));
    }

    private static Set<String> loadBlockedPasswords(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load common password blocklist", exception);
        }
    }
}
