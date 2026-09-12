package org.example.besmarthelpdesk.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import org.example.besmarthelpdesk.enums.Role;

@Value
@Builder
public class ChatMessageResponse {
    UUID id;
    UUID requestId;
    String requestTitle;
    UUID senderId;
    String senderName;
    Role senderRole;
    String message;
    String messageType;
    Boolean isRead;
    Instant createdAt;
    Boolean persisted;
}
