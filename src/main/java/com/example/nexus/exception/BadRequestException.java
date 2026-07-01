package com.example.nexus.exception;

/** Thrown for general business-rule / validation failures (e.g. invalid status value). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
