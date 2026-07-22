package com.example.nexus.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.nexus.dto.InstructionResponseDTO.InstructionRequestDTO;
import com.example.nexus.dto.InstructionResponseDTO;
import com.example.nexus.entity.Instruction.InstructionDocument;
import com.example.nexus.repository.InstructionRepository.InstructionDocumentRepository;
import com.example.nexus.service.InstructionService;
import com.example.nexus.util.JwtUtil;

@RestController
@RequestMapping("/api/instructions")
@CrossOrigin("*")
public class InstructionController {

    @Autowired
    private InstructionService instructionService;

    @Autowired
    private InstructionDocumentRepository instructionDocumentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractEmail(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractEmail(token);
    }

    // Create — multipart so Supporting Documents can be attached in the same call.
    // Angular sends: FormData with a JSON blob part named "instruction" + file parts named "files".
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InstructionResponseDTO createInstruction(
            @RequestPart("instruction") InstructionRequestDTO instruction,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestHeader("Authorization") String authHeader) {
        return instructionService.createInstruction(instruction, files, extractEmail(authHeader));
    }

    @GetMapping
    public List<InstructionResponseDTO> getAllInstructions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestHeader("Authorization") String authHeader) {
        return instructionService.getAllInstructions(search, status, category, priority, extractEmail(authHeader));
    }

    @GetMapping("/{id}")
    public InstructionResponseDTO getInstructionById(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return instructionService.getInstructionById(id, extractEmail(authHeader));
    }

    // Update — JSON body. Send back the "version" you received from GET so the
    // server can detect concurrent edits (SRS Edge Case #4).
    @PutMapping("/{id}")
    public InstructionResponseDTO updateInstruction(@PathVariable Long id,
            @RequestBody InstructionRequestDTO instruction,
            @RequestParam(required = false) Long version,
            @RequestHeader("Authorization") String authHeader) {
        return instructionService.updateInstruction(id, instruction, version, extractEmail(authHeader));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteInstruction(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        instructionService.deleteInstruction(id, extractEmail(authHeader));
        return Map.of("message", "Instruction deleted successfully");
    }

    @PostMapping("/{id}/acknowledge")
    public InstructionResponseDTO acknowledgeInstruction(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return instructionService.acknowledgeInstruction(id, extractEmail(authHeader));
    }

    @PostMapping("/{id}/reminder")
    public Map<String, String> sendReminder(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        instructionService.sendReminder(id, extractEmail(authHeader));
        return Map.of("message", "Reminder sent to pending users");
    }

    // Add more supporting documents to an existing instruction
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InstructionResponseDTO addDocuments(@PathVariable Long id,
            @RequestPart("files") MultipartFile[] files,
            @RequestHeader("Authorization") String authHeader) {
        return instructionService.addDocuments(id, files, extractEmail(authHeader));
    }

    // Download a single supporting document
    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) throws IOException {
        InstructionDocument doc = instructionDocumentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path path = Path.of(doc.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !Files.isReadable(path)) {
            throw new RuntimeException("File not found on server");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }
}