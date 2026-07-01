package com.example.nexus.exception;

/** Thrown when a Project/Task/ErrorReport/User cannot be found by id. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
