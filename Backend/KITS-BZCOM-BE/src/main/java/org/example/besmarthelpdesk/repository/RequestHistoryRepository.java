package org.example.besmarthelpdesk.repository;

import org.example.besmarthelpdesk.entity.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistory, UUID> {
    List<RequestHistory> findByRequestIdOrderByChangedAtAsc(UUID requestId);
}
