package org.example.besmarthelpdesk.repository;

import org.example.besmarthelpdesk.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByTargetMemberIdOrderByCreatedAtDesc(UUID memberId);
    List<Alert> findByTargetMemberIdAndIsReadFalseOrderByCreatedAtDesc(UUID memberId);
}
