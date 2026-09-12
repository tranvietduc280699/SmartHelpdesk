package org.example.besmarthelpdesk.facade;

import org.example.besmarthelpdesk.dto.request.ClassifyRequestDto;
import org.example.besmarthelpdesk.dto.request.SuggestPriorityDto;
import org.example.besmarthelpdesk.dto.response.ClassifyResponseDto;
import org.example.besmarthelpdesk.dto.response.RequestSummaryResponseDto;
import org.example.besmarthelpdesk.dto.response.SuggestPriorityResponseDto;
import org.example.besmarthelpdesk.security.UserPrincipal;

import java.util.UUID;

public interface LlmFacade {
    ClassifyResponseDto classify(ClassifyRequestDto dto);
    SuggestPriorityResponseDto suggestPriority(SuggestPriorityDto dto);
    RequestSummaryResponseDto summarize(UUID requestId, UserPrincipal currentUser);
}
