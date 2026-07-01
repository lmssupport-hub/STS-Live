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

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid annotation errors (field-level)
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

    // 404 — Project/Task/Error not found
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return body;
    }

    // 409 — Edge Case #4: concurrent update conflict (version mismatch)
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(ConflictException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return body;
    }

    // Safety net: JPA's own @Version check can throw this directly if two
    // saves race past our manual check — treat the same as ConflictException
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "This error was updated by another user. Please refresh and try again.");
        return body;
    }

    // 400 — multipart size guard kicks in before our own size check runs
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMaxSize(MaxUploadSizeExceededException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Invalid file format or file size exceeds 5 MB");
        return body;
    }

    // 400 — covers all business-rule violations: invalid screenshot format/size,
    // task not belonging to project, invalid status value, etc.
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return body;
    }

    // Fallback — any other unexpected RuntimeException (matches your existing convention)
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleRuntime(RuntimeException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return body;
    }
}
