package org.example.besmarthelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.request.CreateRequestDto;
import org.example.besmarthelpdesk.dto.response.RequestStatsResponse;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.exception.ResourceNotFoundException;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.repository.RequestRepository;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.RequestService;
import org.example.besmarthelpdesk.specification.RequestSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Request createRequest(CreateRequestDto dto, UUID clientId, String companyId) {
        log.info("(createRequest) title: {}, category: {}, priority: {}, clientId: {}, companyId: {}",
                dto.getTitle(), dto.getCategory(), dto.getPriority(), clientId, companyId);

        RequestPriority priority = dto.getPriority() != null ? dto.getPriority() : RequestPriority.MEDIUM;

        Request request = Request.builder()
                .title(dto.getTitle().trim())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .priority(priority)
                .status(RequestStatus.PENDING)
                .clientId(clientId)
                .companyId(companyId)
                .build();

        Request saved = requestRepository.save(request);
        log.info("(createRequest) created request ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Request> getRequests(UserPrincipal currentUser, RequestCategory category,
                                     RequestPriority priority, RequestStatus status,
                                     String search, Pageable pageable) {
        log.info("(getRequests) currentUser: {}, category: {}, priority: {}, status: {}, search: {}",
                currentUser != null ? currentUser.getUsername() : "anonymous", category, priority, status, search);

        Specification<Request> spec = RequestSpecification.build(currentUser, category, priority, status, search);
        return requestRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Request getRequestById(UUID id) {
        log.info("(getRequestById) id: {}", id);
        return requestRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("(getRequestById) request with ID {} not found", id);
                    return new ResourceNotFoundException(MessageConstants.REQUEST_NOT_FOUND + id);
                });
    }

    @Override
    @Transactional
    public Request getRequestByIdForUpdate(UUID id) {
        log.info("(getRequestByIdForUpdate) locking request ID: {}", id);
        return requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> {
                    log.warn("(getRequestByIdForUpdate) request with ID {} not found", id);
                    return new ResourceNotFoundException(MessageConstants.REQUEST_NOT_FOUND + id);
                });
    }

    @Override
    @Transactional
    public Request save(Request request) {
        log.info("(save) saving request ID: {}", request.getId());
        return requestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestStatsResponse getStats() {
        log.info("(getStats) calculating request statistics");

        long total = requestRepository.count();
        long completed = requestRepository.countByStatus(RequestStatus.DONE);
        double completionRate = total == 0 ? 0.0 : Math.round((double) completed * 10000.0 / total) / 100.0;

        Map<RequestCategory, Long> categoryMap = new EnumMap<>(RequestCategory.class);
        for (RequestCategory cat : RequestCategory.values()) {
            categoryMap.put(cat, requestRepository.countByCategory(cat));
        }

        List<Member> developers = memberRepository.findByRoleAndIsDeletedFalse(Role.DEVELOPER);
        List<RequestStatsResponse.DeveloperTaskStats> devStatsList = new ArrayList<>();

        for (Member dev : developers) {
            long devCompleted = requestRepository.countByAssignedDeveloperIdAndStatus(dev.getId(), RequestStatus.DONE);
            long devInProgress = requestRepository.countByAssignedDeveloperIdAndStatus(dev.getId(), RequestStatus.IN_PROGRESS);
            long devPending = requestRepository.countByAssignedDeveloperIdAndStatus(dev.getId(), RequestStatus.PENDING);
            long totalAssigned = requestRepository.countByAssignedDeveloperId(dev.getId());

            devStatsList.add(RequestStatsResponse.DeveloperTaskStats.builder()
                    .developerId(dev.getId())
                    .developerName(dev.getName())
                    .completedCount(devCompleted)
                    .inProgressCount(devInProgress)
                    .pendingCount(devPending)
                    .totalAssigned(totalAssigned)
                    .build());
        }

        return RequestStatsResponse.builder()
                .totalRequests(total)
                .completedRequests(completed)
                .completionRate(completionRate)
                .requestsByCategory(categoryMap)
                .requestsByDeveloper(devStatsList)
                .build();
    }
}
