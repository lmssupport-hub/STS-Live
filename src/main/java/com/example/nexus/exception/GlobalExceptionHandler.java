package com.example.nexus.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Combined global error handler — everything in ONE file.
 *
 * - Works TODAY with the current TaskService (which throws plain
 *   RuntimeException for every validation rule) via the generic
 *   RuntimeException fallback at the bottom.
 * - ALSO future-ready: if TaskService (or any other service) is later
 *   refactored to throw the typed exceptions below (ResourceNotFoundException,
 *   ConflictException, BadRequestException), they get the correct HTTP
 *   status automatically (404 / 409 / 400) instead of falling through
 *   to the generic 400 fallback.
 *
 * No other files needed — the 3 custom exception types are nested
 * static classes at the bottom of this same file.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── @Valid failures on request DTOs (e.g. Main Task Name-07: max length) ──
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(err.getField(), err.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);
        return body;
    }

    // ── 404 — typed "not found" errors (future use) ──
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        return message(ex.getMessage());
    }

    // ── 409 — concurrent update conflict (future use, needs @Version on entity) ──
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(ConflictException ex) {
        return message(ex.getMessage());
    }

    // Safety net: JPA's own @Version check throws this directly if two saves race
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        return message("This record was updated by another user. Please refresh and try again.");
    }

    // ── 400 — multipart size guard (future use, if file upload added) ──
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMaxSize(MaxUploadSizeExceededException ex) {
        return message("Invalid file format or file size exceeds 5 MB");
    }

    // ── 400 — typed business-rule violations (future use) ──
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException ex) {
        return message(ex.getMessage());
    }

    // ── Fallback — current TaskService throws plain RuntimeException for
    //    EVERYTHING (Priority-02, date rules, target count, duplicate name, etc.)
    //    "not found" style messages get mapped to 404, everything else to 400.
    @ExceptionHandler(RuntimeException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Something went wrong. Please try again.";
        HttpStatus status = msg.toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return org.springframework.http.ResponseEntity.status(status).body(message(msg));
    }

    // ── 500 — anything truly unexpected (DB down, server unavailable = Connectivity-02) ──
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGeneric(Exception ex) {
        return message("Server error. Please try again in a moment.");
    }

    private Map<String, String> message(String msg) {
        Map<String, String> body = new HashMap<>();
        body.put("message", msg);
        return body;
    }

    // ════════════════════════════════════════════════════════════════════
    //  Nested exception types — combined into this same file so nothing
    //  else needs to be created. Throw these from any service (optional,
    //  not required today) to get precise 404/409/400 status codes.
    // ════════════════════════════════════════════════════════════════════

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}