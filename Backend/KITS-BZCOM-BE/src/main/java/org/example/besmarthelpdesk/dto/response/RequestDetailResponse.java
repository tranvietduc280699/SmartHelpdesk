package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDetailResponse {
    private UUID id;
    private String companyId;
    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String title;
    private String description;
    private RequestCategory category;
    private RequestPriority priority;
    private RequestStatus status;
    private UUID clientId;
    private String clientName;
    private String clientEmail;
    private UUID assignedDeveloperId;
    private String assignedDeveloperName;
    private String assignedDeveloperEmail;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RequestHistoryResponse> history;
}
