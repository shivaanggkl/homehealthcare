package com.homehealthcare.branch.application;

public class DuplicateBranchNameException extends RuntimeException {

    public DuplicateBranchNameException(String name) {
        super("Branch name already exists within agency: " + name);
    }
}
