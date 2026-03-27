package com.homehealthcare.platform.email;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignedEmailLinkService {

    private final SystemEmailProperties properties;

    public String createInvitationLink(String token, OffsetDateTime expiresAt) {
        return createSignedLink(properties.getInvitePath(), "INVITATION", token, expiresAt);
    }

    public String createPasswordResetLink(String token, OffsetDateTime expiresAt) {
        return createSignedLink(properties.getPasswordResetPath(), "PASSWORD_RESET", token, expiresAt);
    }

    public String createSecurityAlertLink(String reference, OffsetDateTime expiresAt) {
        return createSignedLink(properties.getSecurityAlertPath(), "SECURITY_ALERT", reference, expiresAt);
    }

    public boolean verify(String purpose, String tokenOrReference, OffsetDateTime expiresAt, String signature) {
        if (expiresAt.isBefore(OffsetDateTime.now()) || expiresAt.isEqual(OffsetDateTime.now())) {
            return false;
        }
        return computeSignature(purpose, tokenOrReference, expiresAt).equals(signature);
    }

    private String createSignedLink(String path, String purpose, String tokenOrReference, OffsetDateTime expiresAt) {
        String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expiresAt);
        String signature = computeSignature(purpose, tokenOrReference, expiresAt);
        return normalizeBaseUrl(properties.getBaseUrl()) + normalizePath(path)
                + "?token=" + urlEncode(tokenOrReference)
                + "&expiresAt=" + urlEncode(timestamp)
                + "&signature=" + urlEncode(signature);
    }

    private String computeSignature(String purpose, String tokenOrReference, OffsetDateTime expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getLinkSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((purpose + "|" + tokenOrReference + "|" + expiresAt.toInstant()).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign email link", exception);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
