package org.example.besmarthelpdesk.service.impl;

import com.fasterxml.uuid.Generators;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.dto.request.SendChatMessageRequest;
import org.example.besmarthelpdesk.dto.response.ChatMessageResponse;
import org.example.besmarthelpdesk.entity.ChatMessage;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.exception.BadRequestException;
import org.example.besmarthelpdesk.exception.ResourceNotFoundException;
import org.example.besmarthelpdesk.repository.ChatMessageRepository;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.repository.RequestRepository;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.ChatService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final Duration INACTIVITY_LIMIT = Duration.ofMinutes(10);

    private final ChatMessageRepository chatMessageRepository;
    private final RequestRepository requestRepository;
    private final MemberRepository memberRepository;
    private final Map<UUID, PendingChat> pendingChats = new ConcurrentHashMap<>();

    @Override
    public ChatMessageResponse sendMessage(UUID requestId, SendChatMessageRequest request, UserPrincipal currentUser) {
        Request targetRequest = getRequest(requestId);
        validateAccess(targetRequest, currentUser);

        Instant now = Instant.now();
        ChatMessage message = ChatMessage.builder()
                .id(Generators.timeBasedEpochGenerator().generate())
                .requestId(requestId)
                .senderId(currentUser.getId())
                .message(request.getMessage().trim())
                .messageType(request.getMessageType() == null || request.getMessageType().isBlank()
                        ? "TEXT" : request.getMessageType().trim().toUpperCase())
                .isRead(false)
                .createdAt(now)
                .build();

        PendingChat pendingChat = pendingChats.computeIfAbsent(requestId, ignored -> new PendingChat());
        synchronized (pendingChat) {
            pendingChat.messages.add(message);
            pendingChat.lastActivity = now;
        }
        return toResponse(message, false, targetRequest.getTitle());
    }

    @Override
    public List<ChatMessageResponse> getMessages(UUID requestId, UserPrincipal currentUser) {
        Request targetRequest = getRequest(requestId);
        validateAccess(targetRequest, currentUser);

        List<ChatMessageResponse> messages = new ArrayList<>(chatMessageRepository.findByRequestIdOrderByCreatedAtAsc(requestId)
                .stream().map(message -> toResponse(message, true, targetRequest.getTitle())).toList());
        PendingChat pendingChat = pendingChats.get(requestId);
        if (pendingChat != null) {
            synchronized (pendingChat) {
                messages.addAll(pendingChat.messages.stream().map(message -> toResponse(message, false, targetRequest.getTitle())).toList());
            }
        }
        messages.sort(Comparator.comparing(ChatMessageResponse::getCreatedAt));
        return messages;
    }

        @Override
        public List<ChatMessageResponse> getInbox(UserPrincipal currentUser) {
        List<Request> visibleRequests = requestRepository.findAll().stream()
            .filter(request -> canAccess(request, currentUser))
            .toList();
        Map<UUID, String> titles = visibleRequests.stream()
            .collect(java.util.stream.Collectors.toMap(Request::getId, Request::getTitle));
        if (titles.isEmpty()) return List.of();
        List<ChatMessageResponse> inbox = new ArrayList<>(
            chatMessageRepository.findByRequestIdInOrderByCreatedAtDesc(new ArrayList<>(titles.keySet()))
                .stream()
                .map(message -> toResponse(message, true, titles.get(message.getRequestId())))
                .toList());
        pendingChats.forEach((requestId, pendingChat) -> {
            String title = titles.get(requestId);
            if (title == null) return;
            synchronized (pendingChat) {
            inbox.addAll(pendingChat.messages.stream()
                .map(message -> toResponse(message, false, title))
                .toList());
            }
        });
        inbox.sort(Comparator.comparing(ChatMessageResponse::getCreatedAt).reversed());
        return inbox;
        }

        @Override
        @Transactional
        public ChatMessageResponse markRead(UUID messageId, UserPrincipal currentUser) {
        ChatMessage message = chatMessageRepository.findById(messageId).orElseGet(() -> findPendingMessage(messageId));
        Request targetRequest = getRequest(message.getRequestId());
        validateAccess(targetRequest, currentUser);
        message.setIsRead(true);
        if (message.getIsRead()) return toResponse(message, !isPending(message), targetRequest.getTitle());
        message.setIsRead(true);
        return toResponse(isPending(message) ? message : chatMessageRepository.save(message), !isPending(message), targetRequest.getTitle());
        }

    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void flushInactiveChats() {
        Instant cutoff = Instant.now().minus(INACTIVITY_LIMIT);
        pendingChats.forEach((requestId, pendingChat) -> {
            synchronized (pendingChat) {
                if (pendingChat.messages.isEmpty() || pendingChat.lastActivity == null
                        || pendingChat.lastActivity.isAfter(cutoff)) {
                    return;
                }
                chatMessageRepository.saveAll(new ArrayList<>(pendingChat.messages));
                pendingChats.remove(requestId, pendingChat);
                log.info("Persisted {} chat messages for request {} after inactivity", pendingChat.messages.size(), requestId);
            }
        });
    }

    private Request getRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + requestId));
    }

    private void validateAccess(Request request, UserPrincipal currentUser) {
        boolean admin = hasRole(currentUser, Role.ADMIN);
        boolean client = request.getClientId().equals(currentUser.getId());
        boolean developer = request.getAssignedDeveloperId() != null
                && request.getAssignedDeveloperId().equals(currentUser.getId());
        if (!admin && !client && !developer) {
            throw new BadRequestException("You do not have permission to access this chat");
        }
    }

    private boolean hasRole(UserPrincipal user, Role role) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private ChatMessageResponse toResponse(ChatMessage message, boolean persisted, String requestTitle) {
        Member sender = memberRepository.findById(message.getSenderId()).orElse(null);
        return ChatMessageResponse.builder()
                .id(message.getId())
                .requestId(message.getRequestId())
                .requestTitle(requestTitle)
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getName() : null)
                .senderRole(sender != null ? sender.getRole() : null)
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .persisted(persisted)
                .build();
    }

    private boolean canAccess(Request request, UserPrincipal currentUser) {
        return hasRole(currentUser, Role.ADMIN)
                || request.getClientId().equals(currentUser.getId())
                || (request.getAssignedDeveloperId() != null
                && request.getAssignedDeveloperId().equals(currentUser.getId()));
    }

    private ChatMessage findPendingMessage(UUID messageId) {
        for (PendingChat pendingChat : pendingChats.values()) {
            synchronized (pendingChat) {
                for (ChatMessage message : pendingChat.messages) {
                    if (message.getId().equals(messageId)) return message;
                }
            }
        }
        throw new ResourceNotFoundException("Chat message not found with ID: " + messageId);
    }

    private boolean isPending(ChatMessage message) {
        PendingChat pendingChat = pendingChats.get(message.getRequestId());
        return pendingChat != null && pendingChat.messages.contains(message);
    }

    private static class PendingChat {
        private final List<ChatMessage> messages = new ArrayList<>();
        private Instant lastActivity;
    }
}
