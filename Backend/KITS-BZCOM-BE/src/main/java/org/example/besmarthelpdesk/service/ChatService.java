package org.example.besmarthelpdesk.service;

import java.util.List;
import java.util.UUID;
import org.example.besmarthelpdesk.dto.request.SendChatMessageRequest;
import org.example.besmarthelpdesk.dto.response.ChatMessageResponse;
import org.example.besmarthelpdesk.security.UserPrincipal;

public interface ChatService {
    ChatMessageResponse sendMessage(UUID requestId, SendChatMessageRequest request, UserPrincipal currentUser);

    List<ChatMessageResponse> getMessages(UUID requestId, UserPrincipal currentUser);

    List<ChatMessageResponse> getInbox(UserPrincipal currentUser);

    ChatMessageResponse markRead(UUID messageId, UserPrincipal currentUser);
}
