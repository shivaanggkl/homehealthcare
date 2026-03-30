package com.homehealthcare.review.application;

import java.util.UUID;

public class UnauthorizedReviewActorException extends RuntimeException {

    public UnauthorizedReviewActorException(UUID membershipId, String action) {
        super("Agency membership %s is not authorized to %s in the review workspace.".formatted(membershipId, action));
    }
}
