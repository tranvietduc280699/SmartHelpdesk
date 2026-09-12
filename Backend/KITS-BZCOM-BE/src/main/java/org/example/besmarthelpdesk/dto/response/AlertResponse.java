package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.enums.AlertType;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    private UUID id;
    private UUID requestId;
    private UUID targetMemberId;
    private AlertType alertType;
    private String message;
    private Boolean isRead;
    private Instant createdAt;
}
