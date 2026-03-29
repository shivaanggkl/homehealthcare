package com.homehealthcare.mobile.foundation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MobileSyncEnvelope(
        UUID envelopeId,
        String operationType,
        String idempotencyKey,
        Instant clientOccurredAt,
        UUID visitId,
        UUID caregiverProfileId,
        String payloadChecksum) {

    public MobileSyncEnvelope {
        Objects.requireNonNull(envelopeId, "envelopeId must not be null");
        Objects.requireNonNull(clientOccurredAt, "clientOccurredAt must not be null");
        if (operationType == null || operationType.isBlank()) {
            throw new IllegalArgumentException("operationType must not be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        payloadChecksum = payloadChecksum == null || payloadChecksum.isBlank() ? "UNSPECIFIED" : payloadChecksum;
    }
}
