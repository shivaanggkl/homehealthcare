package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.InvalidLoginCredentialsException;
import com.homehealthcare.auth.application.CurrentAuthSessionNotFoundException;
import com.homehealthcare.auth.application.CurrentPasswordMismatchException;
import com.homehealthcare.auth.application.CurrentSessionRevocationNotAllowedException;
import com.homehealthcare.auth.application.PasswordResetTokenAlreadyUsedException;
import com.homehealthcare.auth.application.PasswordResetTokenExpiredException;
import com.homehealthcare.auth.application.PasswordResetTokenNotFoundException;
import com.homehealthcare.auth.application.PasswordReuseNotAllowedException;
import com.homehealthcare.auth.application.WeakPasswordException;
import com.homehealthcare.auth.application.TooManyLoginAttemptsException;
import com.homehealthcare.auth.application.InvalidTotpCodeException;
import com.homehealthcare.auth.application.MfaEnrollmentChallengeAlreadyUsedException;
import com.homehealthcare.auth.application.MfaEnrollmentChallengeExpiredException;
import com.homehealthcare.auth.application.MfaEnrollmentChallengeNotFoundException;
import com.homehealthcare.auth.application.MfaEnrollmentRequiredException;
import com.homehealthcare.auth.application.MfaLoginChallengeAlreadyUsedException;
import com.homehealthcare.auth.application.MfaLoginChallengeExpiredException;
import com.homehealthcare.auth.application.MfaLoginChallengeNotFoundException;
import com.homehealthcare.auth.application.ManagedSessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = LoginController.class)
class AuthenticationApiExceptionHandler {

    @ExceptionHandler(InvalidLoginCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorResponse handleInvalidLoginCredentials(InvalidLoginCredentialsException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    ErrorResponse handleTooManyLoginAttempts(TooManyLoginAttemptsException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(CurrentAuthSessionNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorResponse handleMissingCurrentAuthSession(CurrentAuthSessionNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MfaEnrollmentRequiredException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ErrorResponse handleMfaEnrollmentRequired(MfaEnrollmentRequiredException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({
            PasswordResetTokenNotFoundException.class,
            PasswordResetTokenAlreadyUsedException.class,
            WeakPasswordException.class,
            PasswordReuseNotAllowedException.class,
            CurrentPasswordMismatchException.class,
            MfaEnrollmentChallengeNotFoundException.class,
            MfaEnrollmentChallengeAlreadyUsedException.class,
            MfaLoginChallengeNotFoundException.class,
            MfaLoginChallengeAlreadyUsedException.class,
            InvalidTotpCodeException.class,
            CurrentSessionRevocationNotAllowedException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleBadRequest(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ManagedSessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse handleManagedSessionNotFound(ManagedSessionNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(PasswordResetTokenExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ErrorResponse handleExpiredResetToken(PasswordResetTokenExpiredException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MfaEnrollmentChallengeExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ErrorResponse handleExpiredMfaEnrollmentChallenge(MfaEnrollmentChallengeExpiredException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MfaLoginChallengeExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ErrorResponse handleExpiredMfaLoginChallenge(MfaLoginChallengeExpiredException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    record ErrorResponse(String message) {
    }
}
