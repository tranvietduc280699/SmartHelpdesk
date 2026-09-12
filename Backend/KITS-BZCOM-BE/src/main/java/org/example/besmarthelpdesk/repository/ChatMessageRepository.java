package org.example.besmarthelpdesk.repository;

import org.example.besmarthelpdesk.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByRequestIdOrderByCreatedAtAsc(UUID requestId);

    List<ChatMessage> findByRequestIdInOrderByCreatedAtDesc(List<UUID> requestIds);
}
