package com.example.nexus.service;

import com.example.nexus.dto.PriorityRankingDtos.RankingEntry;
import com.example.nexus.entity.User;
import com.example.nexus.repository.TaskRepository;
import com.example.nexus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PriorityRankingService {

    // Fewer than this many active tasks → "Available", otherwise "Busy".
    // Adjust here if the business rule changes.
    private static final long BUSY_THRESHOLD = 3;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // ── Resolve which "team" the requester belongs to (same pattern as Project/Task) ──
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

    public List<RankingEntry> getRanking(String requesterEmail, String search, String workloadStatusFilter) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);

        // Team members = the admin themself + everyone they invited
        List<User> team = new ArrayList<>();
        userRepository.findById(teamAdminId).ifPresent(team::add);
        team.addAll(userRepository.findByCreatedByAdminId(teamAdminId));

        // Active task counts, defaulting to 0 for members with none
        Map<Long, Long> countsByUserId = new HashMap<>();
        for (Object[] row : taskRepository.countActiveTasksByTeam(teamAdminId)) {
            Long userId = (Long) row[0];
            Long count  = (Long) row[1];
            countsByUserId.put(userId, count);
        }

        List<RankingEntry> entries = new ArrayList<>();
        for (User u : team) {
            long activeCount = countsByUserId.getOrDefault(u.getId(), 0L);
            String name = (u.getFirstName() + " " + u.getLastName()).trim();
            String status = activeCount < BUSY_THRESHOLD ? "Available" : "Busy";
            entries.add(new RankingEntry(u.getId(), name, activeCount, 0, status));
        }

        // Search filter (case-insensitive, on name)
        if (search != null && !search.isBlank()) {
            String needle = search.trim().toLowerCase();
            entries.removeIf(e -> !e.getAssignedUserName().toLowerCase().contains(needle));
        }

        // Rank: fewer active tasks → better (lower) rank number, tie-break by name
        entries.sort(Comparator
                .comparingLong(RankingEntry::getCurrentActiveTasks)
                .thenComparing(RankingEntry::getAssignedUserName));

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setPriorityRank(i + 1);
        }

        // Workload status filter, applied after ranking so rank numbers stay meaningful
        if (workloadStatusFilter != null && !workloadStatusFilter.isBlank()) {
            String wanted = workloadStatusFilter.trim();
            entries.removeIf(e -> !e.getWorkloadStatus().equalsIgnoreCase(wanted));
        }

        return entries;
    }
}