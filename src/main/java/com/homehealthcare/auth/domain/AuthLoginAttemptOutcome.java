package com.homehealthcare.auth.domain;

public enum AuthLoginAttemptOutcome {
    SUCCESS,
    FAILURE,
    LOCKED_OUT,
    RATE_LIMITED
}
