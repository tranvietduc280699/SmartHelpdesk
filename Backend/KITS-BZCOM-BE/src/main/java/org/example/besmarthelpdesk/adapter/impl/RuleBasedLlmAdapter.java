package org.example.besmarthelpdesk.adapter.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.adapter.LlmAdapter;
import org.example.besmarthelpdesk.dto.response.ClassifyResponseDto;
import org.example.besmarthelpdesk.dto.response.SuggestPriorityResponseDto;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Slf4j
public class RuleBasedLlmAdapter implements LlmAdapter {

    private static final String[] BUG_KEYWORDS = {
            "bug", "lỗi", "error", "fail", "crash", "hỏng", "exception", "defect",
            "không hoạt động", "chết", "treo", "500", "404", "không đăng nhập được"
    };

    private static final String[] FEATURE_KEYWORDS = {
            "feature", "tính năng", "chức năng", "thêm", "nâng cấp", "phát triển",
            "bổ sung", "cải tiến", "mở rộng", "add", "new feature", "enhancement"
    };

    private static final String[] HIGH_PRIORITY_KEYWORDS = {
            "urgent", "khẩn cấp", "critical", "sập", "blocker", "ngay lập tức",
            "chết hệ thống", "mất dữ liệu", "an toàn", "bảo mật", "security", "high", "production"
    };

    private static final String[] LOW_PRIORITY_KEYWORDS = {
            "low", "thấp", "minor", "khi nào rảnh", "không vội", "tùy chọn",
            "góp ý", "tham khảo", "optional", "trivial"
    };

    @Override
    public ClassifyResponseDto classify(String description) {
        log.info("(classify) analyzing description: {}", description);

        if (description == null || description.isBlank()) {
            return ClassifyResponseDto.builder()
                    .category(RequestCategory.INQUIRY)
                    .confidence(0.5)
                    .reason("Empty description, default to INQUIRY")
                    .build();
        }

        String lower = description.toLowerCase(Locale.ROOT);

        for (String kw : BUG_KEYWORDS) {
            if (lower.contains(kw)) {
                log.info("(classify) matched BUG keyword: {}", kw);
                return ClassifyResponseDto.builder()
                        .category(RequestCategory.BUG)
                        .confidence(0.92)
                        .reason("Matched defect/error keyword: '" + kw + "'")
                        .build();
            }
        }

        for (String kw : FEATURE_KEYWORDS) {
            if (lower.contains(kw)) {
                log.info("(classify) matched FEATURE keyword: {}", kw);
                return ClassifyResponseDto.builder()
                        .category(RequestCategory.FEATURE)
                        .confidence(0.88)
                        .reason("Matched new requirement/feature keyword: '" + kw + "'")
                        .build();
            }
        }

        log.info("(classify) no specific bug/feature keyword found, classified as INQUIRY");
        return ClassifyResponseDto.builder()
                .category(RequestCategory.INQUIRY)
                .confidence(0.80)
                .reason("General question or inquiry pattern detected")
                .build();
    }

    @Override
    public SuggestPriorityResponseDto suggestPriority(String description) {
        log.info("(suggestPriority) analyzing description: {}", description);

        if (description == null || description.isBlank()) {
            return SuggestPriorityResponseDto.builder()
                    .priority(RequestPriority.MEDIUM)
                    .confidence(0.5)
                    .reason("Empty description, default to MEDIUM")
                    .build();
        }

        String lower = description.toLowerCase(Locale.ROOT);

        for (String kw : HIGH_PRIORITY_KEYWORDS) {
            if (lower.contains(kw)) {
                log.info("(suggestPriority) matched HIGH priority keyword: {}", kw);
                return SuggestPriorityResponseDto.builder()
                        .priority(RequestPriority.HIGH)
                        .confidence(0.95)
                        .reason("Matched critical/urgency keyword: '" + kw + "'")
                        .build();
            }
        }

        for (String kw : LOW_PRIORITY_KEYWORDS) {
            if (lower.contains(kw)) {
                log.info("(suggestPriority) matched LOW priority keyword: {}", kw);
                return SuggestPriorityResponseDto.builder()
                        .priority(RequestPriority.LOW)
                        .confidence(0.85)
                        .reason("Matched non-urgent/minor keyword: '" + kw + "'")
                        .build();
            }
        }

        log.info("(suggestPriority) standard issue, suggested MEDIUM");
        return SuggestPriorityResponseDto.builder()
                .priority(RequestPriority.MEDIUM)
                .confidence(0.80)
                .reason("Standard request urgency without critical markers")
                .build();
    }

    @Override
    public String summarize(Request request) {
        log.info("(summarize) summarizing request ID: {}", request.getId());
        return String.format("[%s] %s - Request status is %s with %s priority.",
                request.getCategory(), request.getTitle(), request.getStatus(), request.getPriority());
    }
}
