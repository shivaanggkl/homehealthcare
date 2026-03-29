package com.homehealthcare.mobile.foundation;

import java.util.Objects;
import java.util.UUID;

public record MobileSyncResult(
        MobileSyncDisposition disposition,
        UUID serverMutationId,
        String message) {

    public MobileSyncResult {
        Objects.requireNonNull(disposition, "disposition must not be null");
        message = message == null || message.isBlank() ? disposition.name() : message;
    }

    public static MobileSyncResult accepted(UUID serverMutationId, String message) {
        return new MobileSyncResult(MobileSyncDisposition.ACCEPTED, serverMutationId, message);
    }

    public static MobileSyncResult duplicate(UUID serverMutationId, String message) {
        return new MobileSyncResult(MobileSyncDisposition.DUPLICATE_ALREADY_APPLIED, serverMutationId, message);
    }

    public static MobileSyncResult rejectedValidation(String message) {
        return new MobileSyncResult(MobileSyncDisposition.REJECTED_VALIDATION, null, message);
    }

    public static MobileSyncResult rejectedAuthorization(String message) {
        return new MobileSyncResult(MobileSyncDisposition.REJECTED_AUTHORIZATION, null, message);
    }
}
