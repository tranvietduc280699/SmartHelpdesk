package org.example.besmarthelpdesk.service;

import org.example.besmarthelpdesk.dto.request.CreateRequestDto;
import org.example.besmarthelpdesk.dto.response.RequestStatsResponse;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RequestService {
    Request createRequest(CreateRequestDto dto, UUID clientId, String companyId);
    Page<Request> getRequests(UserPrincipal currentUser, RequestCategory category, RequestPriority priority,
                              RequestStatus status, String search, Pageable pageable);
    Request getRequestById(UUID id);
    Request getRequestByIdForUpdate(UUID id);
    Request save(Request request);
    RequestStatsResponse getStats();
}
