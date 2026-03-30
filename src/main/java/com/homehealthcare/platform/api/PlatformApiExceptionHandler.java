package com.homehealthcare.platform.api;

import com.homehealthcare.analytics.application.AnalyticsEntityNotFoundException;
import com.homehealthcare.analytics.application.UnauthorizedAnalyticsActorException;
import com.homehealthcare.careprogression.application.CareProgressionConflictException;
import com.homehealthcare.careprogression.application.CareProgressionEntityNotFoundException;
import com.homehealthcare.careprogression.application.UnauthorizedCareProgressionActorException;
import com.homehealthcare.compliance.application.ComplianceConflictException;
import com.homehealthcare.compliance.application.ComplianceEntityNotFoundException;
import com.homehealthcare.compliance.application.UnauthorizedComplianceActorException;
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
import com.homehealthcare.patientevent.application.PatientEventConflictException;
import com.homehealthcare.patientevent.application.PatientEventEntityNotFoundException;
import com.homehealthcare.patientevent.application.UnauthorizedPatientEventActorException;
import com.homehealthcare.review.application.ReviewConflictException;
import com.homehealthcare.review.application.ReviewEntityNotFoundException;
import com.homehealthcare.review.application.UnauthorizedReviewActorException;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessConflictException;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessEntityNotFoundException;
import com.homehealthcare.revenuereadiness.application.UnauthorizedRevenueReadinessActorException;
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

    @ExceptionHandler(AnalyticsEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleAnalyticsNotFound(AnalyticsEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedAnalyticsActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedAnalyticsActor(UnauthorizedAnalyticsActorException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(CareProgressionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleCareProgressionConflict(CareProgressionConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(CareProgressionEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleCareProgressionNotFound(CareProgressionEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedCareProgressionActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedCareProgressionActor(UnauthorizedCareProgressionActorException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(ComplianceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleComplianceConflict(ComplianceConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(ComplianceEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleComplianceNotFound(ComplianceEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedComplianceActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedComplianceActor(UnauthorizedComplianceActorException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(PatientEventConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handlePatientEventConflict(PatientEventConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(PatientEventEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handlePatientEventNotFound(PatientEventEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedPatientEventActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedPatientEventActor(UnauthorizedPatientEventActorException exception) {
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

    @ExceptionHandler(ReviewConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleReviewConflict(ReviewConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(ReviewEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleReviewNotFound(ReviewEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedReviewActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedReviewActor(UnauthorizedReviewActorException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(RevenueReadinessConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleRevenueReadinessConflict(RevenueReadinessConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(RevenueReadinessEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleRevenueReadinessNotFound(RevenueReadinessEntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedRevenueReadinessActorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> handleUnauthorizedRevenueReadinessActor(UnauthorizedRevenueReadinessActorException exception) {
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
