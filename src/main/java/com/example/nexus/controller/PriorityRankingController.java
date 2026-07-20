package com.example.nexus.controller;

import com.example.nexus.dto.PriorityRankingDtos.RankingEntry;
import com.example.nexus.service.PriorityRankingService;
import com.example.nexus.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/priority-ranking")
@CrossOrigin("*")
public class PriorityRankingController {

    @Autowired
    private PriorityRankingService priorityRankingService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractEmail(String authHeader) {
        return jwtUtil.extractEmail(authHeader.replace("Bearer ", ""));
    }

    // GET /api/priority-ranking?search=John&workloadStatus=Available
    @GetMapping
    public List<RankingEntry> getRanking(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String workloadStatus,
            @RequestHeader("Authorization") String authHeader) {

        return priorityRankingService.getRanking(extractEmail(authHeader), search, workloadStatus);
    }
}