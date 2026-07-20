package com.example.nexus.controller;

import com.example.nexus.dto.MeetingDtos.MeetingRequest;
import com.example.nexus.dto.MeetingDtos.MeetingResponse;
import com.example.nexus.service.MeetingService;
import com.example.nexus.util.JwtUtil;

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

    @Autowired
    private JwtUtil jwtUtil;

    private String extractEmail(String authHeader) {
        return jwtUtil.extractEmail(authHeader.replace("Bearer ", ""));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeetingResponse create(
            @RequestPart("dto") String dtoJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader) throws Exception {

        MeetingRequest req = parseDto(dtoJson);
        return meetingService.create(req, files, extractEmail(authHeader));
    }

    @GetMapping
    public List<MeetingResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerId,
            @RequestHeader("Authorization") String authHeader) {

        return meetingService.getAll(search, status, ownerId, extractEmail(authHeader));
    }

    @GetMapping("/{id}")
    public MeetingResponse getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return meetingService.getById(id, extractEmail(authHeader));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeetingResponse update(
            @PathVariable Long id,
            @RequestPart("dto") String dtoJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader) throws Exception {

        MeetingRequest req = parseDto(dtoJson);
        return meetingService.update(id, req, files, extractEmail(authHeader));
    }

    @PatchMapping("/{id}/start")
    public MeetingResponse startMeeting(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return meetingService.startMeeting(id, extractEmail(authHeader));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        meetingService.delete(id, extractEmail(authHeader));
        return Map.of("message", "Meeting deleted successfully");
    }

    private MeetingRequest parseDto(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(json, MeetingRequest.class);
    }
}