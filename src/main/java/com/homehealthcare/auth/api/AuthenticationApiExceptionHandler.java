package com.homehealthcare.auth.api;

import com.homehealthcare.auth.application.InvalidLoginCredentialsException;
import com.homehealthcare.auth.application.PasswordResetTokenAlreadyUsedException;
import com.homehealthcare.auth.application.PasswordResetTokenExpiredException;
import com.homehealthcare.auth.application.PasswordResetTokenNotFoundException;
import com.homehealthcare.auth.application.WeakPasswordException;
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

    @ExceptionHandler({
            PasswordResetTokenNotFoundException.class,
            PasswordResetTokenAlreadyUsedException.class,
            WeakPasswordException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleBadRequest(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(PasswordResetTokenExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ErrorResponse handleExpiredResetToken(PasswordResetTokenExpiredException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    record ErrorResponse(String message) {
    }
}
