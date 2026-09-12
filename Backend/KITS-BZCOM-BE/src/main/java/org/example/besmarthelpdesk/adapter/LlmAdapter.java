package org.example.besmarthelpdesk.adapter;

import org.example.besmarthelpdesk.dto.response.ClassifyResponseDto;
import org.example.besmarthelpdesk.dto.response.SuggestPriorityResponseDto;
import org.example.besmarthelpdesk.entity.Request;

public interface LlmAdapter {
    ClassifyResponseDto classify(String description);
    SuggestPriorityResponseDto suggestPriority(String description);
    String summarize(Request request);
}
