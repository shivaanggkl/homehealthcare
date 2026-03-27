package com.homehealthcare.agency.application;

public class DuplicateAgencySlugException extends RuntimeException {

    public DuplicateAgencySlugException(String slug) {
        super("Agency slug already exists: " + slug);
    }
}
