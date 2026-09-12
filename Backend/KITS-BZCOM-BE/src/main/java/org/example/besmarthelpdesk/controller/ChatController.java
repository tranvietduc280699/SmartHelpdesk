package org.example.besmarthelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.dto.request.SendChatMessageRequest;
import org.example.besmarthelpdesk.dto.response.ChatMessageResponse;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat between ADMIN, DEVELOPER, and CLIENT for a request")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/inbox")
    @Operation(summary = "Get chat inbox", description = "Returns chat messages visible to the current user")
    public ResponseEntity<ResponseGeneral<List<ChatMessageResponse>>> getInbox(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ResponseGeneral.success(chatService.getInbox(currentUser)));
    }

    @PatchMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark chat message as read")
    public ResponseEntity<ResponseGeneral<ChatMessageResponse>> markRead(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ResponseGeneral.success(chatService.markRead(messageId, currentUser)));
    }

    @PostMapping("/requests/{requestId}/messages")
    @Operation(summary = "Send chat message", description = "Message is buffered and persisted after 10 minutes of inactivity")
    public ResponseEntity<ResponseGeneral<ChatMessageResponse>> sendMessage(
            @PathVariable UUID requestId,
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ResponseGeneral.success(
                chatService.sendMessage(requestId, request, currentUser)));
    }

    @GetMapping("/requests/{requestId}/messages")
    @Operation(summary = "Get chat history", description = "Returns persisted messages and messages waiting for the inactivity flush")
    public ResponseEntity<ResponseGeneral<List<ChatMessageResponse>>> getMessages(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ResponseGeneral.success(
                chatService.getMessages(requestId, currentUser)));
    }
}
