package org.example.besmarthelpdesk.service;

import org.example.besmarthelpdesk.dto.response.RequestHistoryResponse;
import org.example.besmarthelpdesk.enums.HistoryAction;
import org.example.besmarthelpdesk.enums.RequestStatus;

import java.util.List;
import java.util.UUID;

public interface RequestHistoryService {
    RequestHistoryResponse recordHistory(UUID requestId, UUID changedBy, HistoryAction action,
                                         RequestStatus fromStatus, RequestStatus toStatus, String memo);
    List<RequestHistoryResponse> getHistoryByRequestId(UUID requestId);
}
