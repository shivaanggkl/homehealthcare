package com.homehealthcare.user.application;

import com.homehealthcare.user.domain.UserStatus;
import java.util.UUID;

public class UserSignInBlockedException extends RuntimeException {

    public UserSignInBlockedException(UUID userId, UserStatus status) {
        super("User " + userId + " cannot sign in with status " + status);
    }
}
