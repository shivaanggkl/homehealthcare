package com.homehealthcare.branch.application;

public class DuplicateBranchCodeException extends RuntimeException {

    public DuplicateBranchCodeException(String code) {
        super("Branch code already exists within agency: " + code);
    }
}
