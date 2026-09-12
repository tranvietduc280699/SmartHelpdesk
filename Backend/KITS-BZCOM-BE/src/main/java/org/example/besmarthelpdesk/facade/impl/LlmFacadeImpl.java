package org.example.besmarthelpdesk.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.adapter.LlmAdapter;
import org.example.besmarthelpdesk.dto.request.ClassifyRequestDto;
import org.example.besmarthelpdesk.dto.request.SuggestPriorityDto;
import org.example.besmarthelpdesk.dto.response.ClassifyResponseDto;
import org.example.besmarthelpdesk.dto.response.RequestSummaryResponseDto;
import org.example.besmarthelpdesk.dto.response.SuggestPriorityResponseDto;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.facade.LlmFacade;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.RequestService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmFacadeImpl implements LlmFacade {

    private final LlmAdapter llmAdapter;
    private final RequestService requestService;

    @Override
    public ClassifyResponseDto classify(ClassifyRequestDto dto) {
        log.info("(classify) request description length: {}", dto.getDescription().length());
        return llmAdapter.classify(dto.getDescription());
    }

    @Override
    public SuggestPriorityResponseDto suggestPriority(SuggestPriorityDto dto) {
        log.info("(suggestPriority) request description length: {}", dto.getDescription().length());
        return llmAdapter.suggestPriority(dto.getDescription());
    }

    @Override
    public RequestSummaryResponseDto summarize(UUID requestId, UserPrincipal currentUser) {
        log.info("(summarize) requestId: {}, user: {}", requestId, currentUser.getUsername());
        Request request = requestService.getRequestById(requestId);
        String summary = llmAdapter.summarize(request);
        return RequestSummaryResponseDto.builder()
                .requestId(requestId)
                .summary(summary)
                .build();
    }
}
