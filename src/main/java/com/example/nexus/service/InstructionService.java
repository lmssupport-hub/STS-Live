package com.example.nexus.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.nexus.dto.InstructionResponseDTO.AcknowledgementResponseDTO;
import com.example.nexus.dto.InstructionResponseDTO.DocumentResponseDTO;
import com.example.nexus.dto.InstructionResponseDTO.InstructionRequestDTO;
import com.example.nexus.entity.Instruction.InstructionAcknowledgement;
import com.example.nexus.entity.Instruction.InstructionDocument;
import com.example.nexus.repository.InstructionRepository.InstructionAcknowledgementRepository;
import com.example.nexus.dto.InstructionResponseDTO;
import com.example.nexus.entity.Instruction;
import com.example.nexus.entity.User;
import com.example.nexus.repository.InstructionRepository;
import com.example.nexus.repository.UserRepository;

@Service
public class InstructionService {

    @Autowired private InstructionRepository instructionRepository;
    @Autowired private InstructionAcknowledgementRepository acknowledgementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private EmailService emailService;
    @Autowired private NotificationService notificationService; // NEW

    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "Client Instruction", "Process Update", "SOP Update", "Operational Guideline", "Announcement");
    private static final List<String> ALLOWED_PRIORITIES = List.of("High", "Medium", "Low");
    private static final List<String> ALLOWED_STATUSES = List.of("Draft", "Active", "Expired", "Archived");

    // Same multi-tenant convention as ProjectService.resolveTeamAdminId
    private Long resolveTeamAdminId(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Invalid session"));
        if ("ADMIN".equals(requester.getRole()) || "SUPER_ADMIN".equals(requester.getRole())) {
            return requester.getId();
        }
        if (requester.getCreatedByAdminId() == null) {
            throw new RuntimeException("Your account is not linked to a team yet");
        }
        return requester.getCreatedByAdminId();
    }

    public InstructionResponseDTO createInstruction(InstructionRequestDTO dto, MultipartFile[] files, String requesterEmail) {
        validateInstruction(dto);

        Instruction instruction = new Instruction();
        instruction.setTitle(dto.getTitle().trim());
        instruction.setCategory(dto.getCategory());
        instruction.setDescription(dto.getDescription());
        instruction.setPriority(dto.getPriority());
        instruction.setEffectiveDate(dto.getEffectiveDate());
        instruction.setTargetUsersOrTeams(dto.getTargetUsersOrTeams());
        instruction.setStatus(isBlank(dto.getStatus()) ? "Active" : dto.getStatus());
        instruction.setOwnerEmail(requesterEmail);
        instruction.setTeamAdminId(resolveTeamAdminId(requesterEmail));
        instruction.setCreatedAt(LocalDateTime.now());
        instruction.setUpdatedAt(LocalDateTime.now());

        Instruction saved = instructionRepository.save(instruction);

        // Acknowledgement Status defaults to Pending for every assigned user
        for (String targetUser : dto.getTargetUsersOrTeams()) {
            InstructionAcknowledgement ack = new InstructionAcknowledgement();
            ack.setUserEmail(targetUser);
            ack.setStatus("Pending");
            ack.setInstruction(saved);
            saved.getAcknowledgements().add(ack);
        }

        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) attachDocument(saved, file);
            }
        }

        Instruction finalSaved = instructionRepository.save(saved);

        // NEW — notify every target user/team that a new instruction landed on them
        notifyInstructionTargets(finalSaved, finalSaved.getTargetUsersOrTeams());

        return toDto(finalSaved);
    }

    public List<InstructionResponseDTO> getAllInstructions(String search, String status, String category,
            String priority, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        return instructionRepository.searchAndFilter(teamAdminId, search, status, category, priority)
                .stream().map(this::toDto).toList();
    }

    public InstructionResponseDTO getInstructionById(Long id, String requesterEmail) {
        return toDto(findScoped(id, requesterEmail));
    }

    public InstructionResponseDTO updateInstruction(Long id, InstructionRequestDTO dto, Long clientVersion, String requesterEmail) {
        Instruction existing = findScoped(id, requesterEmail);
        validateInstruction(dto);

        // Edge Case #4 — concurrent update detection
        if (clientVersion != null && existing.getVersion() != null && !clientVersion.equals(existing.getVersion())) {
            throw new RuntimeException("This instruction was updated by someone else. Please refresh and try again.");
        }

        // NEW — remember who was already targeted before the update
        List<String> previousTargets = existing.getTargetUsersOrTeams() != null
                ? new ArrayList<>(existing.getTargetUsersOrTeams())
                : new ArrayList<>();

        existing.setTitle(dto.getTitle().trim());
        existing.setCategory(dto.getCategory());
        existing.setDescription(dto.getDescription());
        existing.setPriority(dto.getPriority());
        existing.setEffectiveDate(dto.getEffectiveDate());
        existing.setTargetUsersOrTeams(dto.getTargetUsersOrTeams());
        // Edge Case #5 — only the final selected status is applied
        if (!isBlank(dto.getStatus())) existing.setStatus(dto.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        Instruction saved;
        try {
            saved = instructionRepository.saveAndFlush(existing);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("This instruction was updated by someone else. Please refresh and try again.");
        }

        // NEW — only notify the newly added targets, not everyone again
        List<String> newTargets = saved.getTargetUsersOrTeams() == null
                ? List.of()
                : saved.getTargetUsersOrTeams().stream()
                        .filter(target -> !previousTargets.contains(target))
                        .toList();

        if (!newTargets.isEmpty()) {
            notifyInstructionTargets(saved, newTargets);
        }

        return toDto(saved);
    }

    public void deleteInstruction(Long id, String requesterEmail) {
        instructionRepository.delete(findScoped(id, requesterEmail));
    }

    public InstructionResponseDTO acknowledgeInstruction(Long id, String requesterEmail) {
        findScoped(id, requesterEmail);

        InstructionAcknowledgement ack = acknowledgementRepository
                .findByInstructionIdAndUserEmailIgnoreCase(id, requesterEmail)
                .orElseThrow(() -> new RuntimeException("You are not an assigned recipient of this instruction"));

        // Edge Case #11 — record only the first acknowledgement
        if ("Acknowledged".equals(ack.getStatus())) {
            throw new RuntimeException("You have already acknowledged this instruction");
        }

        ack.setStatus("Acknowledged");
        ack.setAcknowledgedAt(LocalDateTime.now());
        acknowledgementRepository.save(ack);

        return toDto(instructionRepository.findById(id).orElseThrow());
    }

    public void sendReminder(Long id, String requesterEmail) {
        Instruction instruction = findScoped(id, requesterEmail);
        List<InstructionAcknowledgement> pending = acknowledgementRepository.findByInstructionIdAndStatus(id, "Pending");

        // Edge Case #15 — nothing to remind
        if (pending.isEmpty()) {
            throw new RuntimeException("All assigned users have already acknowledged this instruction");
        }

        for (InstructionAcknowledgement ack : pending) {
            try {
                emailService.sendInstructionReminderEmail(ack.getUserEmail(), instruction.getTitle());
            } catch (Exception e) {
                // Edge Case #10 — log and continue; don't fail the whole batch
                System.err.println("Reminder failed for " + ack.getUserEmail() + ": " + e.getMessage());
            }

            // NEW — in-app reminder notification alongside the email
            notificationService.notifyUserByEmail(
                    ack.getUserEmail(),
                    "Reminder: " + instruction.getTitle(),
                    "You still need to acknowledge the instruction \"" + instruction.getTitle() + "\".",
                    "Reminder Notification",
                    "INSTRUCTION",
                    instruction.getId());
        }
    }

    public InstructionResponseDTO addDocuments(Long id, MultipartFile[] files, String requesterEmail) {
        Instruction instruction = findScoped(id, requesterEmail);
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) attachDocument(instruction, file);
            }
        }
        return toDto(instructionRepository.save(instruction));
    }

    // ════════════════════════════════════════════════════════════════════
    //  NEW — Notification helper
    // ════════════════════════════════════════════════════════════════════

    private void notifyInstructionTargets(Instruction instruction, List<String> targetEmails) {
        if (targetEmails == null || targetEmails.isEmpty()) return;

        String title = "New Instruction: " + instruction.getTitle();
        String message = "A new instruction \"" + instruction.getTitle() + "\" (" + instruction.getCategory()
                + ") has been issued. Priority: " + instruction.getPriority() + ".";

        for (String targetEmail : targetEmails) {
            notificationService.notifyUserByEmail(
                    targetEmail, title, message, "New Instruction", "INSTRUCTION", instruction.getId());
        }
    }

    private void attachDocument(Instruction instruction, MultipartFile file) {
        String storedPath = fileStorageService.store(file, instruction.getId());
        InstructionDocument doc = new InstructionDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(fileStorageService.getExtension(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setFilePath(storedPath);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setInstruction(instruction);
        instruction.getDocuments().add(doc);
    }

    private Instruction findScoped(Long id, String requesterEmail) {
        Instruction instruction = instructionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Instruction not found"));
        if (!instruction.getTeamAdminId().equals(resolveTeamAdminId(requesterEmail))) {
            throw new RuntimeException("You don't have access to this instruction");
        }
        return instruction;
    }

    private void validateInstruction(InstructionRequestDTO dto) {
        if (isBlank(dto.getTitle())) throw new RuntimeException("Instruction title is required");
        if (dto.getTitle().trim().length() > 200) throw new RuntimeException("Instruction title must not exceed 200 characters");
        if (isBlank(dto.getCategory()) || !ALLOWED_CATEGORIES.contains(dto.getCategory())) throw new RuntimeException("Category is required");
        if (isBlank(dto.getDescription())) throw new RuntimeException("Instruction description is required");
        if (dto.getDescription().length() > 5000) throw new RuntimeException("Instruction description must not exceed 5000 characters");
        if (isBlank(dto.getPriority()) || !ALLOWED_PRIORITIES.contains(dto.getPriority())) throw new RuntimeException("Priority is required");
        if (dto.getTargetUsersOrTeams() == null || dto.getTargetUsersOrTeams().isEmpty()) throw new RuntimeException("At least one user or team must be selected");
        if (dto.getEffectiveDate() != null && dto.getEffectiveDate().isBefore(LocalDate.now())) throw new RuntimeException("Invalid effective date");
        if (!isBlank(dto.getStatus()) && !ALLOWED_STATUSES.contains(dto.getStatus())) throw new RuntimeException("Instruction status is required");
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private InstructionResponseDTO toDto(Instruction i) {
        List<DocumentResponseDTO> docs = i.getDocuments().stream()
                .map(d -> new DocumentResponseDTO(d.getId(), d.getFileName(), d.getFileType(), d.getFileSize(), d.getUploadedAt()))
                .toList();

        List<AcknowledgementResponseDTO> acks = i.getAcknowledgements().stream()
                .map(a -> new AcknowledgementResponseDTO(a.getId(), a.getUserEmail(), a.getStatus(), a.getAcknowledgedAt()))
                .toList();

        return new InstructionResponseDTO(
                i.getId(), i.getTitle(), i.getCategory(), i.getDescription(), i.getPriority(), i.getStatus(),
                i.getEffectiveDate(), i.getOwnerEmail(), i.getVersion(), i.getCreatedAt(), i.getUpdatedAt(),
                i.getTargetUsersOrTeams(), docs, acks);
    }
}

// File storage helper for Supporting Documents (10MB limit, format check)
@Service
class FileStorageService {

    @Value("${app.upload-dir:uploads/instructions}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

    public String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }

    public void validate(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("Uploaded file is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new RuntimeException("File size exceeds 10 MB");
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("Invalid file format. Allowed: PDF, DOC, DOCX, XLS, XLSX, JPG, PNG");
        }
    }

    public String store(MultipartFile file, Long instructionId) {
        validate(file);
        try {
            Path targetDir = Paths.get(uploadDir, String.valueOf(instructionId));
            Files.createDirectories(targetDir);
            String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path targetPath = targetDir.resolve(safeName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }
}