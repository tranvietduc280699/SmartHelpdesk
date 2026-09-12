package org.example.besmarthelpdesk.facade;

import org.example.besmarthelpdesk.dto.request.AssignDeveloperDto;
import org.example.besmarthelpdesk.dto.request.CreateRequestDto;
import org.example.besmarthelpdesk.dto.request.UpdateRequestStatusDto;
import org.example.besmarthelpdesk.dto.response.RequestDetailResponse;
import org.example.besmarthelpdesk.dto.response.RequestHistoryResponse;
import org.example.besmarthelpdesk.dto.response.RequestResponse;
import org.example.besmarthelpdesk.dto.response.RequestStatsResponse;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RequestFacade {
    RequestResponse createRequest(CreateRequestDto dto, UserPrincipal currentUser);
    Page<RequestResponse> getRequests(UserPrincipal currentUser, RequestCategory category,
                                      RequestPriority priority, RequestStatus status,
                                      String search, Pageable pageable);
    RequestDetailResponse getRequestDetail(UUID id, UserPrincipal currentUser);
    RequestResponse assignDeveloper(UUID requestId, AssignDeveloperDto dto, UserPrincipal currentUser);
    RequestResponse updateStatus(UUID requestId, UpdateRequestStatusDto dto, UserPrincipal currentUser);
    List<RequestHistoryResponse> getRequestHistory(UUID requestId, UserPrincipal currentUser);
    RequestStatsResponse getStats(UserPrincipal currentUser);
}
