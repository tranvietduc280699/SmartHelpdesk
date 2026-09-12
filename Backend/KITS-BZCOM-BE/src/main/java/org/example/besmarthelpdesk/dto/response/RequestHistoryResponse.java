package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.enums.HistoryAction;
import org.example.besmarthelpdesk.enums.RequestStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestHistoryResponse {
    private UUID id;
    private UUID requestId;
    private UUID changedBy;
    private String changedByName;
    private HistoryAction action;
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private String memo;
    private Instant changedAt;
}
