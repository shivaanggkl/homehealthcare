package com.homehealthcare.documentation.application;

import java.util.List;

public class DocumentationValidationException extends RuntimeException {

    private final List<FieldValidationError> fieldErrors;
    private final List<TaskValidationError> taskErrors;

    public DocumentationValidationException(String message, List<FieldValidationError> fieldErrors, List<TaskValidationError> taskErrors) {
        super(message);
        this.fieldErrors = List.copyOf(fieldErrors);
        this.taskErrors = List.copyOf(taskErrors);
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }

    public List<TaskValidationError> getTaskErrors() {
        return taskErrors;
    }

    public record FieldValidationError(String fieldKey, String message) {
    }

    public record TaskValidationError(String taskTitle, String message) {
    }
}
