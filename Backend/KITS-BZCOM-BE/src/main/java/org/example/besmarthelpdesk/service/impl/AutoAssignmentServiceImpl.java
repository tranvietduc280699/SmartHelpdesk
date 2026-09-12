package org.example.besmarthelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.exception.BadRequestException;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.repository.RequestRepository;
import org.example.besmarthelpdesk.service.AutoAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoAssignmentServiceImpl implements AutoAssignmentService {

    private final MemberRepository memberRepository;
    private final RequestRepository requestRepository;

    private static final List<RequestStatus> ACTIVE_TASK_STATUSES = List.of(RequestStatus.PENDING, RequestStatus.IN_PROGRESS);

    @Override
    @Transactional(readOnly = true)
    public Member selectBestDeveloper() {
        log.info("(selectBestDeveloper) selecting best developer for auto-assignment");

        List<Member> developers = memberRepository.findByRoleAndIsDeletedFalse(Role.DEVELOPER)
                .stream()
                .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        if (developers.isEmpty()) {
            log.warn("(selectBestDeveloper) no eligible active developer found");
            throw new BadRequestException(MessageConstants.NO_DEVELOPER_AVAILABLE);
        }

        // Calculate current active task count for each developer
        Map<UUID, Long> taskCountMap = new HashMap<>();
        for (Member dev : developers) {
            long count = requestRepository.countByAssignedDeveloperIdAndStatusIn(dev.getId(), ACTIVE_TASK_STATUSES);
            taskCountMap.put(dev.getId(), count);
            log.info("(selectBestDeveloper) developer: {} ({}), activeTaskCount: {}", dev.getName(), dev.getId(), count);
        }

        // Rule 1: Find the lowest task count
        long minTaskCount = taskCountMap.values().stream().min(Long::compareTo).orElse(0L);
        List<Member> candidates = developers.stream()
                .filter(dev -> taskCountMap.get(dev.getId()) == minTaskCount)
                .collect(Collectors.toList());

        log.info("(selectBestDeveloper) minTaskCount: {}, candidate count: {}", minTaskCount, candidates.size());

        if (candidates.size() == 1) {
            Member selected = candidates.get(0);
            log.info("(selectBestDeveloper) uniquely selected developer: {} ({})", selected.getName(), selected.getId());
            return selected;
        }

        // Rule 2: If tied, choose the developer who completed a request most recently
        Member bestCandidate = null;
        Instant latestCompletion = null;

        for (Member candidate : candidates) {
            Optional<Instant> completedAtOpt = requestRepository.findLatestCompletedAtByDeveloperId(candidate.getId());
            Instant completedAt = completedAtOpt.orElse(Instant.MIN);
            log.info("(selectBestDeveloper) candidate: {} ({}), latestCompletedAt: {}", candidate.getName(), candidate.getId(), completedAtOpt.orElse(null));

            if (bestCandidate == null) {
                bestCandidate = candidate;
                latestCompletion = completedAt;
            } else {
                if (completedAt.isAfter(latestCompletion)) {
                    bestCandidate = candidate;
                    latestCompletion = completedAt;
                } else if (completedAt.equals(latestCompletion)) {
                    // Deterministic tie-breaker by ID string
                    if (candidate.getId().toString().compareTo(bestCandidate.getId().toString()) < 0) {
                        bestCandidate = candidate;
                    }
                }
            }
        }

        log.info("(selectBestDeveloper) tie-breaker selected developer: {} ({}) with latestCompletedAt: {}",
                bestCandidate.getName(), bestCandidate.getId(), latestCompletion);
        return bestCandidate;
    }
}
