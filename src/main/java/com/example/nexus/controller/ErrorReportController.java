package com.example.nexus.controller;

import com.example.nexus.dto.ErrorDtos.*;
import com.example.nexus.entity.ErrorReport;
import com.example.nexus.service.ErrorReportService;
import com.example.nexus.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/errors")
@CrossOrigin("*")
public class ErrorReportController {

    @Autowired
    private ErrorReportService errorReportService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractEmail(String authHeader) {
        return jwtUtil.extractEmail(authHeader.replace("Bearer ", ""));
    }

    // ── Field 14: Create Error ──
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ErrorResponse> createError(
            @RequestPart("data") @Valid CreateErrorRequest request,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot,
            @RequestHeader("Authorization") String authHeader) {

        ErrorResponse saved = errorReportService.createError(request, screenshot, extractEmail(authHeader));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Field 1 (Search) + Field 2 (Filter) ──
    @GetMapping
    public ResponseEntity<List<ErrorResponse>> getErrors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) Long projectId,
            @RequestHeader("Authorization") String authHeader) {

        List<ErrorResponse> errors = errorReportService.getErrors(
                keyword, status, priority, assignedUserId, projectId, extractEmail(authHeader));
        return ResponseEntity.ok(errors);
    }

    // ── Field 13: Show More ──
    @GetMapping("/{id}")
    public ResponseEntity<ErrorResponse> getErrorById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(errorReportService.getErrorById(id, extractEmail(authHeader)));
    }

    // ── Field 15: Update Error ──
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ErrorResponse> updateError(
            @PathVariable Long id,
            @RequestPart("data") @Valid UpdateErrorRequest request,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot,
            @RequestHeader("Authorization") String authHeader) {

        ErrorResponse updated = errorReportService.updateError(id, request, screenshot, extractEmail(authHeader));
        return ResponseEntity.ok(updated);
    }

    // ── Field 11: quick Status update ──
    @PatchMapping("/{id}/status")
    public ResponseEntity<ErrorResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("Authorization") String authHeader) {

        return ResponseEntity.ok(errorReportService.updateStatus(id, request, extractEmail(authHeader)));
    }

    // ── Field 14 (S#14): Delete Error ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteError(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        errorReportService.deleteError(id, extractEmail(authHeader));
        return ResponseEntity.ok(Map.of("message", "Error deleted successfully"));
    }

    // ── Field 9: Serve the uploaded screenshot file ──
    @GetMapping("/{id}/screenshot")
    public ResponseEntity<Resource> getScreenshot(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        ErrorReport error = errorReportService.getErrorForScreenshot(id, extractEmail(authHeader));

        if (error.getScreenshotPath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(error.getScreenshotPath());
        Resource resource = new FileSystemResource(file);

        String contentType = file.getName().toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG_VALUE
                : MediaType.IMAGE_JPEG_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + error.getScreenshotName() + "\"")
                .body(resource);
    }
}