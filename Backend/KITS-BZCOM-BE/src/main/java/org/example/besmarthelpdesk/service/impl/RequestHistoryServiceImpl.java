package org.example.besmarthelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.dto.response.RequestHistoryResponse;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.RequestHistory;
import org.example.besmarthelpdesk.enums.HistoryAction;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.repository.RequestHistoryRepository;
import org.example.besmarthelpdesk.service.RequestHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestHistoryServiceImpl implements RequestHistoryService {

    private final RequestHistoryRepository requestHistoryRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public RequestHistoryResponse recordHistory(UUID requestId, UUID changedBy, HistoryAction action,
                                                 RequestStatus fromStatus, RequestStatus toStatus, String memo) {
        log.info("(recordHistory) requestId: {}, changedBy: {}, action: {}, from: {}, to: {}",
                requestId, changedBy, action, fromStatus, toStatus);

        RequestHistory history = RequestHistory.builder()
                .requestId(requestId)
                .changedBy(changedBy)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .memo(memo)
                .changedAt(Instant.now())
                .build();

        RequestHistory saved = requestHistoryRepository.save(history);
        log.info("(recordHistory) recorded history ID: {}", saved.getId());

        String changedByName = memberRepository.findById(changedBy)
                .map(Member::getName)
                .orElse(null);

        return mapToResponse(saved, changedByName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestHistoryResponse> getHistoryByRequestId(UUID requestId) {
        log.info("(getHistoryByRequestId) requestId: {}", requestId);

        List<RequestHistory> histories = requestHistoryRepository.findByRequestIdOrderByChangedAtAsc(requestId);
        if (histories.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> memberIds = histories.stream()
                .map(RequestHistory::getChangedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> memberNameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));

        return histories.stream()
                .map(h -> mapToResponse(h, memberNameMap.get(h.getChangedBy())))
                .collect(Collectors.toList());
    }

    private RequestHistoryResponse mapToResponse(RequestHistory history, String changedByName) {
        return RequestHistoryResponse.builder()
                .id(history.getId())
                .requestId(history.getRequestId())
                .changedBy(history.getChangedBy())
                .changedByName(changedByName)
                .action(history.getAction())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .memo(history.getMemo())
                .changedAt(history.getChangedAt())
                .build();
    }
}
