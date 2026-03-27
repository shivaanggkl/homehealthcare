package com.homehealthcare.auth.domain;

public enum PasswordResetTokenStatus {
    PENDING,
    CONSUMED,
    CANCELLED,
    EXPIRED
}
