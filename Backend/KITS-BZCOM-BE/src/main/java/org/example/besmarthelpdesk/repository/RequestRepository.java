package org.example.besmarthelpdesk.repository;

import jakarta.persistence.LockModeType;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequestRepository extends JpaRepository<Request, UUID>, JpaSpecificationExecutor<Request> {

    List<Request> findByClientId(UUID clientId);

    List<Request> findByAssignedDeveloperId(UUID developerId);

    List<Request> findByCompanyId(String companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Request r WHERE r.id = :id")
    Optional<Request> findByIdForUpdate(@Param("id") UUID id);

    long countByAssignedDeveloperIdAndStatusIn(UUID developerId, Collection<RequestStatus> statuses);

    @Query("SELECT MAX(r.updatedAt) FROM Request r WHERE r.assignedDeveloperId = :developerId AND r.status = 'DONE'")
    Optional<Instant> findLatestCompletedAtByDeveloperId(@Param("developerId") UUID developerId);

    long countByStatus(RequestStatus status);

    long countByCategory(RequestCategory category);

    long countByAssignedDeveloperIdAndStatus(UUID developerId, RequestStatus status);

    long countByAssignedDeveloperId(UUID developerId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Request r SET r.updatedAt = :updatedAt WHERE r.id = :id")
    void updateUpdatedAt(@Param("id") UUID id, @Param("updatedAt") Instant updatedAt);
}
