package com.example.nexus.controller;

import com.example.nexus.dto.ErrorDtos.*;
import com.example.nexus.entity.ErrorReport;
import com.example.nexus.exception.ResourceNotFoundException;
import com.example.nexus.repository.ErrorReportRepository;
import com.example.nexus.service.ErrorReportService;
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
    private ErrorReportRepository errorReportRepository; // used only for screenshot fetch

    // ── Field 14: Create Error (multipart so Screenshot - Field 9 - can be attached) ──
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ErrorResponse> createError(
            @RequestPart("data") @Valid CreateErrorRequest request,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot) {

        ErrorResponse saved = errorReportService.createError(request, screenshot);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Field 1 (Search) + Field 2 (Filter: Status, Priority, Assign User) ──
    @GetMapping
    public ResponseEntity<List<ErrorResponse>> getErrors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) Long projectId) {

        List<ErrorResponse> errors = errorReportService.getErrors(
                keyword, status, priority, assignedUserId, projectId);
        return ResponseEntity.ok(errors);
    }

    // ── Field 13: Show More (Expand Row) - fetch full detail of a single error ──
    @GetMapping("/{id}")
    public ResponseEntity<ErrorResponse> getErrorById(@PathVariable Long id) {
        return ResponseEntity.ok(errorReportService.getErrorById(id));
    }

    // ── Field 15: Update Error (from expanded row, inline save, no page reload) ──
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ErrorResponse> updateError(
            @PathVariable Long id,
            @RequestPart("data") @Valid UpdateErrorRequest request,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot) {

        ErrorResponse updated = errorReportService.updateError(id, request, screenshot);
        return ResponseEntity.ok(updated);
    }

    // ── Field 11: quick Status update from expanded row ──
    @PatchMapping("/{id}/status")
    public ResponseEntity<ErrorResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        return ResponseEntity.ok(errorReportService.updateStatus(id, request));
    }

    // ── Field 14 (S#14): Delete Error ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteError(@PathVariable Long id) {
        errorReportService.deleteError(id);
        return ResponseEntity.ok(Map.of("message", "Error deleted successfully"));
    }

    // ── Field 9: Serve the uploaded screenshot file ──
    @GetMapping("/{id}/screenshot")
    public ResponseEntity<Resource> getScreenshot(@PathVariable Long id) {
        ErrorReport error = errorReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error report not found with id: " + id));

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
