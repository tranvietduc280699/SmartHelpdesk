package org.example.besmarthelpdesk.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.example.besmarthelpdesk.enums.HistoryAction;
import org.example.besmarthelpdesk.enums.RequestStatus;

import java.time.Instant;

import java.util.UUID;

@Entity
@Table(name = "request_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RequestHistory extends BaseEntity {

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private RequestStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private Instant changedAt = Instant.now();
}
