package com.example.nexus.dto;

public class PriorityRankingDtos {

    private PriorityRankingDtos() {
    }

    public static class RankingEntry {
        private Long   assignedUserId;
        private String assignedUserName;
        private long   currentActiveTasks;
        private int    priorityRank;
        private String workloadStatus; // "Available" | "Busy"

        public RankingEntry(Long assignedUserId, String assignedUserName,
                             long currentActiveTasks, int priorityRank, String workloadStatus) {
            this.assignedUserId     = assignedUserId;
            this.assignedUserName   = assignedUserName;
            this.currentActiveTasks = currentActiveTasks;
            this.priorityRank       = priorityRank;
            this.workloadStatus     = workloadStatus;
        }

        public Long getAssignedUserId() { return assignedUserId; }
        public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }

        public String getAssignedUserName() { return assignedUserName; }
        public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }

        public long getCurrentActiveTasks() { return currentActiveTasks; }
        public void setCurrentActiveTasks(long currentActiveTasks) { this.currentActiveTasks = currentActiveTasks; }

        public int getPriorityRank() { return priorityRank; }
        public void setPriorityRank(int priorityRank) { this.priorityRank = priorityRank; }

        public String getWorkloadStatus() { return workloadStatus; }
        public void setWorkloadStatus(String workloadStatus) { this.workloadStatus = workloadStatus; }
    }
}