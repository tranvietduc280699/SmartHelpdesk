package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.enums.RequestPriority;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestPriorityResponseDto {
    private RequestPriority priority;
    private double confidence;
    private String reason;
}
