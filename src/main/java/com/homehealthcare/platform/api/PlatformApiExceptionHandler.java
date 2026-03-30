package com.homehealthcare.platform.api;

import com.homehealthcare.documentation.application.DocumentationConflictException;
import com.homehealthcare.documentation.application.DocumentationEntityNotFoundException;
import com.homehealthcare.documentation.application.DocumentationValidationException;
import com.homehealthcare.documentation.application.UnauthorizedDocumentationActorException;
import com.homehealthcare.evv.application.EvvConflictException;
import com.homehealthcare.evv.application.EvvEntityNotFoundException;
import com.homehealthcare.evv.application.UnauthorizedEvvActorException;
import com.homehealthcare.messaging.application.MessagingConflictException;
import com.homehealthcare.messaging.application.MessagingEntityNotFoundException;
import com.homehealthcare.mobile.application.MobileConflictException;
import com.homehealthcare.mobile.application.MobileEntityNotFoundException;
import java.time.DateTimeException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.homehealthcare")
class PlatformApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(DateTimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> handleDateTime(DateTimeException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> handleInvalidBody(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return Map.of("error", message);
    }

    @ExceptionHandler(MobileConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleMobileConflict(MobileConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(MobileEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleMobileNotFound(MobileEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(DocumentationConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleDocumentationConflict(DocumentationConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(MessagingConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleMessagingConflict(MessagingConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(MessagingEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleMessagingNotFound(MessagingEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(DocumentationEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleDocumentationNotFound(DocumentationEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedDocumentationActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedDocumentationActor(UnauthorizedDocumentationActorException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(DocumentationValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> handleDocumentationValidation(DocumentationValidationException exception) {
        return Map.of(
                "error", exception.getMessage(),
                "fieldErrors", exception.getFieldErrors(),
                "taskErrors", exception.getTaskErrors());
    }

    @ExceptionHandler(EvvConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleEvvConflict(EvvConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(EvvEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleEvvNotFound(EvvEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedEvvActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedEvvActor(UnauthorizedEvvActorException exception) {
        return Map.of("error", exception.getMessage());
    }
}
