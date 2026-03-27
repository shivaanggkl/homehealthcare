package com.homehealthcare.user.application;

import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticationPolicy {

    public boolean canSignIn(User user) {
        return user.getStatus() == UserStatus.ACTIVE && user.hasPasswordHash();
    }

    public void requireCanSignIn(User user) {
        if (!canSignIn(user)) {
            throw new UserSignInBlockedException(user.getId(), user.getStatus());
        }
    }
}
