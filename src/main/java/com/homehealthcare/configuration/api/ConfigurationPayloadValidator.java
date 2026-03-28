package com.homehealthcare.configuration.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationPayloadValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void requireJsonObjectOrArray(String fieldName, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        validateJson(fieldName, payload);
    }

    public void validateOptionalJson(String fieldName, String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        validateJson(fieldName, payload);
    }

    private void validateJson(String fieldName, String payload) {
        try {
            objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException(fieldName + " must be valid JSON");
        }
    }
}
