package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.enums.RequestCategory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestStatsResponse {
    private long totalRequests;
    private long completedRequests;
    private double completionRate; // percentage 0.0 - 100.0
    private Map<RequestCategory, Long> requestsByCategory;
    private List<DeveloperTaskStats> requestsByDeveloper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeveloperTaskStats {
        private UUID developerId;
        private String developerName;
        private long completedCount;
        private long inProgressCount;
        private long pendingCount;
        private long totalAssigned;
    }
}
