package org.example.besmarthelpdesk.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.request.AssignDeveloperDto;
import org.example.besmarthelpdesk.dto.request.CreateRequestDto;
import org.example.besmarthelpdesk.dto.request.UpdateRequestStatusDto;
import org.example.besmarthelpdesk.dto.response.*;
import org.example.besmarthelpdesk.entity.Company;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.*;
import org.example.besmarthelpdesk.exception.BadRequestException;
import org.example.besmarthelpdesk.exception.ResourceNotFoundException;
import org.example.besmarthelpdesk.facade.RequestFacade;
import org.example.besmarthelpdesk.repository.CompanyRepository;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.AlertService;
import org.example.besmarthelpdesk.service.AutoAssignmentService;
import org.example.besmarthelpdesk.service.RequestHistoryService;
import org.example.besmarthelpdesk.service.RequestService;
import org.example.besmarthelpdesk.validator.RequestStatusValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestFacadeImpl implements RequestFacade {

    private final RequestService requestService;
    private final RequestHistoryService requestHistoryService;
    private final AlertService alertService;
    private final AutoAssignmentService autoAssignmentService;
    private final RequestStatusValidator requestStatusValidator;
    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public RequestResponse createRequest(CreateRequestDto dto, UserPrincipal currentUser) {
        log.info("(createRequest) user: {}, title: {}, priority: {}", currentUser.getUsername(), dto.getTitle(), dto.getPriority());

        // Validate that current user is a client and has a company
        Member client = memberRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.MEMBER_NOT_FOUND + currentUser.getId()));

        if (client.getRole() != Role.CLIENT) {
            log.warn("(createRequest) non-client member {} attempted to create request", currentUser.getId());
            throw new BadRequestException(MessageConstants.ONLY_CLIENT_CAN_CREATE);
        }

        if (client.getCompanyId() == null || client.getCompanyId().isBlank()) {
            log.warn("(createRequest) client {} has no company", currentUser.getId());
            throw new BadRequestException(MessageConstants.CLIENT_HAS_NO_COMPANY);
        }

        // 1. Create request
        Request request = requestService.createRequest(dto, client.getId(), client.getCompanyId());

        // 2. Automatically record history
        requestHistoryService.recordHistory(
                request.getId(),
                client.getId(),
                HistoryAction.CREATE,
                null,
                RequestStatus.PENDING,
                "Request created by client"
        );

        // 3. Notify all admins for every new request, regardless of priority.
        boolean highPriority = request.getPriority() == RequestPriority.HIGH;
        alertService.notifyAdmins(
                request.getId(),
                highPriority ? AlertType.HIGH_PRIORITY_REGISTERED : AlertType.REQUEST_REGISTERED,
                highPriority ? "High priority request registered: " + request.getTitle()
                        : "New request registered (" + request.getPriority() + "): " + request.getTitle()
        );

        return mapToResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestResponse> getRequests(UserPrincipal currentUser, RequestCategory category,
                                             RequestPriority priority, RequestStatus status,
                                             String search, Pageable pageable) {
        log.info("(getRequests) user: {}, page: {}, size: {}", currentUser.getUsername(), pageable.getPageNumber(), pageable.getPageSize());

        Page<Request> requestPage = requestService.getRequests(currentUser, category, priority, status, search, pageable);
        List<Request> requests = requestPage.getContent();

        if (requests.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, requestPage.getTotalElements());
        }

        // Batch lookups to avoid N+1 queries
        Set<String> companyIds = requests.stream()
                .map(Request::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> memberIds = new HashSet<>();
        requests.forEach(r -> {
            if (r.getClientId() != null) memberIds.add(r.getClientId());
            if (r.getAssignedDeveloperId() != null) memberIds.add(r.getAssignedDeveloperId());
        });

        Map<String, String> companyMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getCompanyName));

        Map<UUID, String> memberNameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));

        List<RequestResponse> responses = requests.stream()
                .map(r -> mapToResponse(r, companyMap.get(r.getCompanyId()),
                        memberNameMap.get(r.getClientId()), memberNameMap.get(r.getAssignedDeveloperId())))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, requestPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDetailResponse getRequestDetail(UUID id, UserPrincipal currentUser) {
        log.info("(getRequestDetail) id: {}, user: {}", id, currentUser.getUsername());

        Request request = requestService.getRequestById(id);
        validateAccess(request, currentUser);

        // Fetch company info
        Company company = request.getCompanyId() != null
                ? companyRepository.findById(request.getCompanyId()).orElse(null)
                : null;

        // Fetch client info
        Member client = memberRepository.findById(request.getClientId()).orElse(null);

        // Fetch developer info
        Member dev = request.getAssignedDeveloperId() != null
                ? memberRepository.findById(request.getAssignedDeveloperId()).orElse(null)
                : null;

        // Fetch history
        List<RequestHistoryResponse> history = requestHistoryService.getHistoryByRequestId(id);

        return RequestDetailResponse.builder()
                .id(request.getId())
                .companyId(request.getCompanyId())
                .companyName(company != null ? company.getCompanyName() : null)
                .companyAddress(company != null ? company.getAddress() : null)
                .companyPhone(company != null ? company.getPhone() : null)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(request.getStatus())
                .clientId(request.getClientId())
                .clientName(client != null ? client.getName() : null)
                .clientEmail(client != null ? client.getEmail() : null)
                .assignedDeveloperId(request.getAssignedDeveloperId())
                .assignedDeveloperName(dev != null ? dev.getName() : null)
                .assignedDeveloperEmail(dev != null ? dev.getEmail() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .history(history)
                .build();
    }

    @Override
    @Transactional
    public RequestResponse assignDeveloper(UUID requestId, AssignDeveloperDto dto, UserPrincipal currentUser) {
        log.info("(assignDeveloper) requestId: {}, user: {}, developerId: {}",
                requestId, currentUser.getUsername(), dto != null ? dto.getDeveloperId() : "auto");

        // Pessimistic write lock on the request to serialize concurrent assignments
        Request request = requestService.getRequestByIdForUpdate(requestId);

        Member developer;
        if (dto != null && dto.getDeveloperId() != null) {
            developer = memberRepository.findById(dto.getDeveloperId())
                    .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.MEMBER_NOT_FOUND + dto.getDeveloperId()));
            if (developer.getRole() != Role.DEVELOPER) {
                log.warn("(assignDeveloper) selected member {} is not a developer", developer.getId());
                throw new BadRequestException("Assigned member must have DEVELOPER role");
            }
        } else {
            // Auto-assignment algorithm
            log.info("(assignDeveloper) invoking auto-assignment algorithm");
            developer = autoAssignmentService.selectBestDeveloper();
        }

        UUID oldDevId = request.getAssignedDeveloperId();
        request.setAssignedDeveloperId(developer.getId());
        Request saved = requestService.save(request);

        // Record history
        String memo = String.format("Assigned to developer %s (%s)", developer.getName(), developer.getId());
        requestHistoryService.recordHistory(
                request.getId(),
                currentUser.getId(),
                HistoryAction.ASSIGN,
                request.getStatus(),
                request.getStatus(),
                memo
        );

        // Notify assigned developer
        alertService.createAlert(
                request.getId(),
                developer.getId(),
                AlertType.ASSIGNED,
                "You have been assigned to request: " + request.getTitle()
        );

        log.info("(assignDeveloper) request {} successfully assigned to developer {}", requestId, developer.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RequestResponse updateStatus(UUID requestId, UpdateRequestStatusDto dto, UserPrincipal currentUser) {
        log.info("(updateStatus) requestId: {}, user: {}, newStatus: {}", requestId, currentUser.getUsername(), dto.getStatus());

        // Pessimistic write lock to serialize concurrent status updates
        Request request = requestService.getRequestByIdForUpdate(requestId);

        // Authorization check: only ADMIN or the assigned DEVELOPER can update status
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        boolean isAssignedDev = request.getAssignedDeveloperId() != null
                && request.getAssignedDeveloperId().equals(currentUser.getId());

        if (!isAdmin && !isAssignedDev) {
            log.warn("(updateStatus) user {} is neither admin nor assigned developer for request {}", currentUser.getId(), requestId);
            throw new BadRequestException(MessageConstants.ACCESS_DENIED);
        }

        // Validate state transition rules
        RequestStatus oldStatus = request.getStatus();
        requestStatusValidator.validateTransition(oldStatus, dto.getStatus());

        request.setStatus(dto.getStatus());
        Request saved = requestService.save(request);

        // Record history
        HistoryAction action = (dto.getStatus() == RequestStatus.DONE) ? HistoryAction.CLOSE : HistoryAction.UPDATE;
        String memo = (dto.getMemo() != null && !dto.getMemo().isBlank()) ? dto.getMemo() : "Status changed to " + dto.getStatus();

        requestHistoryService.recordHistory(
                request.getId(),
                currentUser.getId(),
                action,
                oldStatus,
                dto.getStatus(),
                memo
        );

        // Notify the client who created this request
        alertService.createAlert(
                request.getId(),
                request.getClientId(),
                AlertType.STATUS_CHANGED,
                String.format("Request status changed from %s to %s for: %s", oldStatus, dto.getStatus(), request.getTitle())
        );

        log.info("(updateStatus) request {} status updated from {} to {}", requestId, oldStatus, dto.getStatus());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestHistoryResponse> getRequestHistory(UUID requestId, UserPrincipal currentUser) {
        log.info("(getRequestHistory) requestId: {}, user: {}", requestId, currentUser.getUsername());

        Request request = requestService.getRequestById(requestId);
        validateAccess(request, currentUser);

        return requestHistoryService.getHistoryByRequestId(requestId);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestStatsResponse getStats(UserPrincipal currentUser) {
        log.info("(getStats) user: {}", currentUser.getUsername());
        return requestService.getStats();
    }

    private void validateAccess(Request request, UserPrincipal currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));

        if (isAdmin) {
            return;
        }

        boolean isOwnerClient = request.getClientId().equals(currentUser.getId());
        boolean isAssignedDev = request.getAssignedDeveloperId() != null
                && request.getAssignedDeveloperId().equals(currentUser.getId());

        if (!isOwnerClient && !isAssignedDev) {
            log.warn("(validateAccess) user {} does not have permission to access request {}", currentUser.getId(), request.getId());
            throw new BadRequestException(MessageConstants.ACCESS_DENIED);
        }
    }

    private RequestResponse mapToResponse(Request request) {
        String companyName = request.getCompanyId() != null
                ? companyRepository.findById(request.getCompanyId()).map(Company::getCompanyName).orElse(null)
                : null;
        String clientName = memberRepository.findById(request.getClientId()).map(Member::getName).orElse(null);
        String devName = request.getAssignedDeveloperId() != null
                ? memberRepository.findById(request.getAssignedDeveloperId()).map(Member::getName).orElse(null)
                : null;

        return mapToResponse(request, companyName, clientName, devName);
    }

    private RequestResponse mapToResponse(Request request, String companyName, String clientName, String devName) {
        return RequestResponse.builder()
                .id(request.getId())
                .companyId(request.getCompanyId())
                .companyName(companyName)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(request.getStatus())
                .clientId(request.getClientId())
                .clientName(clientName)
                .assignedDeveloperId(request.getAssignedDeveloperId())
                .assignedDeveloperName(devName)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
