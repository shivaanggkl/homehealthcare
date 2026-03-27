package com.homehealthcare.auth.application;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final long TIME_STEP_SECONDS = 30L;
    private static final int ALLOWED_CLOCK_SKEW_STEPS = 1;

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpauthUri(String issuer, String accountName, String secret) {
        String normalizedIssuer = urlEncode(issuer);
        String normalizedAccount = urlEncode(accountName);
        return "otpauth://totp/" + normalizedIssuer + ":" + normalizedAccount
                + "?secret=" + secret
                + "&issuer=" + normalizedIssuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean verifyCode(String secret, String code, Instant now) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long counter = now.getEpochSecond() / TIME_STEP_SECONDS;
        for (long skew = -ALLOWED_CLOCK_SKEW_STEPS; skew <= ALLOWED_CLOCK_SKEW_STEPS; skew++) {
            if (generateCode(secret, counter + skew).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String generateCurrentCode(String secret, Instant now) {
        return generateCode(secret, now.getEpochSecond() / TIME_STEP_SECONDS);
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate TOTP code", exception);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder builder = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte datum : data) {
            buffer <<= 8;
            buffer |= datum & 0xFF;
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                builder.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            builder.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return builder.toString();
    }

    private static byte[] base32Decode(String value) {
        String normalized = value.replace("=", "").trim().toUpperCase();
        int buffer = 0;
        int bitsLeft = 0;
        List<Byte> bytes = new ArrayList<>();
        for (int index = 0; index < normalized.length(); index++) {
            int digit = BASE32_ALPHABET.indexOf(normalized.charAt(index));
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer <<= 5;
            buffer |= digit;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes.add((byte) ((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }
        byte[] decoded = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            decoded[i] = bytes.get(i);
        }
        return decoded;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
