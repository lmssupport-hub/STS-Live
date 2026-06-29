package com.example.nexus.controller;

import com.example.nexus.dto.MeetingDtos.MeetingRequest;
import com.example.nexus.dto.MeetingDtos.MeetingResponse;
import com.example.nexus.service.MeetingService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin("*")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    // ── CREATE ─────────────────────────────────────────────────────────────────
    // Frontend sends multipart/form-data:
    //   Part "dto"   → JSON blob (MeetingRequest)
    //   Part "files" → optional file attachments
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeetingResponse create(
            @RequestPart("dto")                      String          dtoJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws Exception {

        MeetingRequest req = parseDto(dtoJson);
        return meetingService.create(req, files);
    }

    // ── GET ALL (search + filter) ──────────────────────────────────────────────
    @GetMapping
    public List<MeetingResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long   ownerId) {

        return meetingService.getAll(search, status, ownerId);
    }

    // ── GET BY ID ──────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public MeetingResponse getById(@PathVariable Long id) {
        return meetingService.getById(id);
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeetingResponse update(
            @PathVariable                            Long            id,
            @RequestPart("dto")                      String          dtoJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws Exception {

        MeetingRequest req = parseDto(dtoJson);
        return meetingService.update(id, req, files);
    }

    // ── START MEETING ──────────────────────────────────────────────────────────
    // Transitions status: Scheduled → In Progress
    @PatchMapping("/{id}/start")
    public MeetingResponse startMeeting(@PathVariable Long id) {
        return meetingService.startMeeting(id);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        meetingService.delete(id);
        return Map.of("message", "Meeting deleted successfully");
    }

    // ── HELPER ─────────────────────────────────────────────────────────────────
    // Parse the JSON "dto" part into MeetingRequest.
    // ObjectMapper is created inline so the controller stays stateless and
    // avoids any Spring bean-wiring complications with multipart parsing.
    private MeetingRequest parseDto(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(json, MeetingRequest.class);
    }
}