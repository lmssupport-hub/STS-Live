package com.example.nexus.service;

import com.example.nexus.dto.MeetingDtos;
import com.example.nexus.dto.MeetingDtos.MeetingRequest;
import com.example.nexus.dto.MeetingDtos.MeetingResponse;
import com.example.nexus.dto.MeetingDtos.MemberInfo;
import com.example.nexus.entity.Meeting;
import com.example.nexus.entity.Project;
import com.example.nexus.entity.User;
import com.example.nexus.repository.MeetingRepository;
import com.example.nexus.repository.ProjectRepository;
import com.example.nexus.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final List<String> VALID_STATUSES =
            Arrays.asList("Scheduled", "In Progress", "Completed", "Expiry");

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // ── CREATE ─────────────────────────────────────────────────────────────────

    public MeetingResponse create(MeetingRequest req, List<MultipartFile> files) {
        validate(req);

        Meeting meeting = new Meeting();
        applyRequest(meeting, req);

        // Parse agenda lines → action items
        meeting.setActionItems(parseActionItems(req.getAgenda()));

        // Handle uploaded documents (store filenames; swap for cloud URLs in prod)
        if (files != null && !files.isEmpty()) {
            List<String> urls = files.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(f -> "/uploads/meetings/" + f.getOriginalFilename())
                    .collect(Collectors.toList());
            meeting.setDocumentUrls(urls);
        }

        Meeting saved = meetingRepository.save(meeting);
        return toResponse(saved);
    }

    // ── GET ALL (with optional filters) ───────────────────────────────────────

    public List<MeetingResponse> getAll(String search, String status, Long ownerId) {
        List<Meeting> result;

        boolean hasSearch  = search  != null && !search.isBlank();
        boolean hasStatus  = status  != null && !status.isBlank();
        boolean hasOwner   = ownerId != null;

        if (hasSearch && hasStatus && hasOwner) {
            result = meetingRepository.searchByKeywordStatusAndOwner(search, status, ownerId);
        } else if (hasSearch && hasStatus) {
            result = meetingRepository.searchByKeywordAndStatus(search, status);
        } else if (hasSearch && hasOwner) {
            result = meetingRepository.searchByKeywordAndOwner(search, ownerId);
        } else if (hasSearch) {
            result = meetingRepository.searchByKeyword(search);
        } else if (hasStatus && hasOwner) {
            result = meetingRepository.findByStatusAndOwnerId(status, ownerId);
        } else if (hasStatus) {
            result = meetingRepository.findByStatus(status);
        } else if (hasOwner) {
            result = meetingRepository.findByOwnerId(ownerId);
        } else {
            result = meetingRepository.findAll();
        }

        return result.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── GET BY ID ──────────────────────────────────────────────────────────────

    public MeetingResponse getById(Long id) {
        Meeting meeting = findOrThrow(id);
        return toResponse(meeting);
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    public MeetingResponse update(Long id, MeetingRequest req, List<MultipartFile> files) {
        Meeting meeting = findOrThrow(id);
        validate(req);
        applyRequest(meeting, req);

        // Re-parse action items from updated agenda
        meeting.setActionItems(parseActionItems(req.getAgenda()));

        // Append newly uploaded documents (keep existing ones)
        if (files != null && !files.isEmpty()) {
            List<String> existing = new ArrayList<>(
                    meeting.getDocumentUrls() == null ? List.of() : meeting.getDocumentUrls());
            files.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(f -> "/uploads/meetings/" + f.getOriginalFilename())
                    .forEach(existing::add);
            meeting.setDocumentUrls(existing);
        }

        Meeting saved = meetingRepository.save(meeting);
        return toResponse(saved);
    }

    // ── START MEETING (status: Scheduled → In Progress) ───────────────────────

    public MeetingResponse startMeeting(Long id) {
        Meeting meeting = findOrThrow(id);

        if (!"Scheduled".equals(meeting.getStatus())) {
            throw new RuntimeException("Only Scheduled meetings can be started");
        }
        meeting.setStatus("In Progress");
        Meeting saved = meetingRepository.save(meeting);
        return toResponse(saved);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        findOrThrow(id);   // throws 404-style RuntimeException if missing
        meetingRepository.deleteById(id);
    }

    // ── VALIDATION ─────────────────────────────────────────────────────────────

    private void validate(MeetingRequest req) {

        if (isBlank(req.getTitle()))
            throw new RuntimeException("Meeting title is required");
        if (req.getTitle().trim().length() > 200)
            throw new RuntimeException("Meeting title must not exceed 200 characters");

        if (isBlank(req.getMeetingDateTime()))
            throw new RuntimeException("Meeting date and time is required");

        LocalDateTime dt = parseDateTime(req.getMeetingDateTime());
        if (dt.isBefore(LocalDateTime.now()))
            throw new RuntimeException("Meeting date and time must be in the future");

        if (isBlank(req.getAgenda()))
            throw new RuntimeException("Agenda is required");
        if (req.getAgenda().trim().length() > 2000)
            throw new RuntimeException("Agenda must not exceed 2000 characters");

        if (req.getDecisionsPolls() != null && req.getDecisionsPolls().length() > 1000)
            throw new RuntimeException("Decisions & Polls must not exceed 1000 characters");

        if (req.getMemberIds() == null || req.getMemberIds().isEmpty())
            throw new RuntimeException("At least one member is required");

        if (isBlank(req.getStatus()))
            throw new RuntimeException("Meeting status is required");
        if (!VALID_STATUSES.contains(req.getStatus()))
            throw new RuntimeException("Invalid status: " + req.getStatus());

        if (req.getOwnerId() == null)
            throw new RuntimeException("Owner is required");
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────

    private Meeting findOrThrow(Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));
    }

    private void applyRequest(Meeting meeting, MeetingRequest req) {
        meeting.setTitle(req.getTitle().trim());
        meeting.setMeetingDateTime(parseDateTime(req.getMeetingDateTime()));
        meeting.setAgenda(req.getAgenda().trim());
        meeting.setDecisionsPolls(
                req.getDecisionsPolls() != null ? req.getDecisionsPolls().trim() : null);
        meeting.setStatus(req.getStatus());
        meeting.setOwnerId(req.getOwnerId());
        meeting.setProjectId(req.getProjectId());
        meeting.setMemberIds(req.getMemberIds());
    }

    private LocalDateTime parseDateTime(String raw) {
        try {
            return LocalDateTime.parse(raw.trim(), DT_FMT);
        } catch (DateTimeParseException e) {
            throw new RuntimeException(
                    "Invalid date/time format. Expected yyyy-MM-dd'T'HH:mm:ss, got: " + raw);
        }
    }

    /** Split agenda by newlines; each non-blank line becomes an action item. */
    private List<String> parseActionItems(String agenda) {
        if (agenda == null || agenda.isBlank()) return List.of();
        return Arrays.stream(agenda.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ── RESPONSE MAPPING ───────────────────────────────────────────────────────

    private MeetingResponse toResponse(Meeting m) {
        MeetingResponse res = new MeetingResponse();

        res.setId(m.getId());
        res.setTitle(m.getTitle());
        res.setMeetingDateTime(m.getMeetingDateTime().format(DT_FMT));
        res.setAgenda(m.getAgenda());
        res.setDecisionsPolls(m.getDecisionsPolls());
        res.setStatus(m.getStatus());
        res.setOwnerId(m.getOwnerId());
        res.setProjectId(m.getProjectId());
        res.setMemberIds(m.getMemberIds());
        res.setDocumentUrls(m.getDocumentUrls());
        res.setActionItems(m.getActionItems());

        if (m.getCreatedAt() != null)
            res.setCreatedAt(m.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (m.getUpdatedAt() != null)
            res.setUpdatedAt(m.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Enrich: owner name
        if (m.getOwnerId() != null) {
            userRepository.findById(m.getOwnerId()).ifPresent(u ->
                    res.setOwnerName(u.getFirstName()));
        }

        // Enrich: project name
        if (m.getProjectId() != null) {
            projectRepository.findById(m.getProjectId()).ifPresent(p ->
                    res.setProjectName(p.getProjectName()));
        }

        // Enrich: member details
        if (m.getMemberIds() != null && !m.getMemberIds().isEmpty()) {
            List<MemberInfo> members = m.getMemberIds().stream()
                    .map(uid -> userRepository.findById(uid)
                            .map(u -> new MemberInfo(u.getId(), u.getFirstName(), u.getEmail()))
                            .orElse(new MemberInfo(uid, "Unknown", "")))
                    .collect(Collectors.toList());
            res.setMembers(members);
        }

        return res;
    }
}